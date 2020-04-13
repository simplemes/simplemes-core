/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.annotation

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockBean
import sample.SampleExtensionInterface
import sample.SampleExtensionPOGOInterface
import sample.SampleMisMatchedMethodExtensionInterface
import sample.SampleNoArgumentExtensionInterface
import sample.pogo.SampleAlternatePOGO
import sample.pogo.SamplePOGO

/**
 * Tests.
 */
class ExtensionPointHelperSpec extends BaseSpecification {

  def "verify that the invokePre method works - with one argument"() {
    given: 'a mocked bean that implements the interface'
    def argument1 = new SamplePOGO(name: 'ABC')
    def mock = Mock(SampleExtensionPOGOInterface)
    new MockBean(this, SampleExtensionPOGOInterface, [mock]).install()

    when: 'the extension is triggered'
    ExtensionPointHelper.instance.invokePre(SampleExtensionPOGOInterface, 'preCoreMethod', argument1)

    then: 'the extension method is called'
    1 * mock.preCoreMethod(argument1)
  }

  def "verify that the invokePre method works - with two arguments"() {
    given: 'a mocked bean that implements the interface'
    def argument1 = 'ABC'
    def argument2 = 237
    def mock = Mock(SampleExtensionInterface)
    new MockBean(this, SampleExtensionInterface, [mock]).install()

    when: 'the extension is triggered'
    ExtensionPointHelper.instance.invokePre(SampleExtensionInterface, 'preCoreMethod', argument1, argument2)

    then: 'the extension method is called'
    1 * mock.preCoreMethod(argument1, argument2)
  }

  def "verify that the invokePre method works - with no arguments"() {
    given: 'a mocked bean that implements the interface'
    def mock = Mock(SampleNoArgumentExtensionInterface)
    new MockBean(this, SampleNoArgumentExtensionInterface, [mock]).install()

    when: 'the extension is triggered'
    ExtensionPointHelper.instance.invokePre(SampleNoArgumentExtensionInterface, 'preCoreMethod')

    then: 'the extension method is called'
    1 * mock.preCoreMethod()
  }

  def "verify that the invokePost method works - with one argument"() {
    given: 'a mocked bean that implements the interface'
    def response = new SampleAlternatePOGO(name: 'XYZ')
    def argument1 = new SamplePOGO(name: 'ABC')
    def mock = Mock(SampleExtensionPOGOInterface)
    new MockBean(this, SampleExtensionPOGOInterface, [mock]).install()

    when: 'the extension is triggered'
    ExtensionPointHelper.instance.invokePost(SampleExtensionPOGOInterface, 'postCoreMethod', response, argument1)

    then: 'the extension method is called'
    1 * mock.postCoreMethod(response, argument1)
  }

  def "verify that the invokePost method works - with two arguments"() {
    given: 'a mocked bean that implements the interface'
    def response = [code: 237]
    def argument1 = 'ABC'
    def argument2 = 237
    def mock = Mock(SampleExtensionInterface)
    new MockBean(this, SampleExtensionInterface, [mock]).install()

    when: 'the extension is triggered'
    ExtensionPointHelper.instance.invokePost(SampleExtensionInterface, 'postCoreMethod', response, argument1, argument2)

    then: 'the extension method is called'
    1 * mock.postCoreMethod(response, argument1, argument2)
  }

  def "verify that the invokePost method works - with no arguments"() {
    given: 'a mocked bean that implements the interface'
    def response = [code: 237]
    def mock = Mock(SampleNoArgumentExtensionInterface)
    new MockBean(this, SampleNoArgumentExtensionInterface, [mock]).install()

    when: 'the extension is triggered'
    ExtensionPointHelper.instance.invokePost(SampleNoArgumentExtensionInterface, 'postCoreMethod', response)

    then: 'the extension method is called'
    1 * mock.postCoreMethod(response)
  }

  def "verify that the invokePost method works - with no arguments and an altered response"() {
    given: 'a mocked bean that implements the interface'
    def response = [code: 237]
    def mock = Mock(SampleNoArgumentExtensionInterface)
    new MockBean(this, SampleNoArgumentExtensionInterface, [mock]).install()

    when: 'the extension is triggered'
    def res = ExtensionPointHelper.instance.invokePost(SampleNoArgumentExtensionInterface, 'postCoreMethod', response)

    then: 'the extension method is called'
    1 * mock.postCoreMethod(response) >> [code: 437]

    and: 'the response is the one from the extension'
    res == [code: 437]
  }

  def "verify that the invokePost method works - with no arguments and un-altered response"() {
    given: 'a mocked bean that implements the interface'
    def response = [code: 237]
    def mock = Mock(SampleNoArgumentExtensionInterface)
    new MockBean(this, SampleNoArgumentExtensionInterface, [mock]).install()

    when: 'the extension is triggered'
    def res = ExtensionPointHelper.instance.invokePost(SampleNoArgumentExtensionInterface, 'postCoreMethod', response)

    then: 'the extension method is called'
    1 * mock.postCoreMethod(response) >> null

    and: 'the response is the original response'
    res == [code: 237]
  }

  def "verify that the invokePost method works - with void response"() {
    given: 'a mocked bean that implements the interface'
    String argument1 = 'ABC'
    def mock = Mock(SampleMisMatchedMethodExtensionInterface)
    new MockBean(this, SampleMisMatchedMethodExtensionInterface, [mock]).install()

    when: 'the extension is triggered'
    def res = ExtensionPointHelper.instance.invokePost(SampleMisMatchedMethodExtensionInterface, 'postCoreMethod2', null, argument1)

    then: 'the extension method is called'
    1 * mock.postCoreMethod2(argument1) >> null

    and: 'the response is the original response'
    res == null
  }

  def "verify that the invokePost method works - multiple extensions and all alter the response"() {
    given: 'a mocked bean that implements the interface'
    def response = [code: 237]
    def response1 = [code: 437]
    def response2 = [code: 537]
    def mock1 = Mock(SampleNoArgumentExtensionInterface)
    def mock2 = Mock(SampleNoArgumentExtensionInterface)
    new MockBean(this, SampleNoArgumentExtensionInterface, [mock1, mock2]).install()

    when: 'the extension is triggered'
    def res = ExtensionPointHelper.instance.invokePost(SampleNoArgumentExtensionInterface, 'postCoreMethod', response)

    then: 'the extension method is called'
    1 * mock1.postCoreMethod(response) >> response1
    1 * mock2.postCoreMethod(response1) >> response2

    and: 'the response is the one from the last extension'
    res == response2
  }

  def "verify that the invokePost method works - multiple extensions and only one alters the response"() {
    given: 'a mocked bean that implements the interface'
    def coreResponse = [code: 237]
    def response2 = [code: 537]
    def mock1 = Mock(SampleNoArgumentExtensionInterface)
    def mock2 = Mock(SampleNoArgumentExtensionInterface)
    def mock3 = Mock(SampleNoArgumentExtensionInterface)
    new MockBean(this, SampleNoArgumentExtensionInterface, [mock1, mock2, mock3]).install()

    when: 'the extension is triggered'
    def res = ExtensionPointHelper.instance.invokePost(SampleNoArgumentExtensionInterface, 'postCoreMethod', coreResponse)

    then: 'the extension method is called'
    1 * mock1.postCoreMethod(coreResponse) >> null
    1 * mock2.postCoreMethod(coreResponse) >> response2
    1 * mock3.postCoreMethod(response2) >> null

    and: 'the response is the one from the last extension'
    res == response2
  }


}
