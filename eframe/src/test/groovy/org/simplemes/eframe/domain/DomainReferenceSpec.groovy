/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/**
 * Tests.
 */
class DomainReferenceSpec extends BaseSpecification {

  static specNeeds = [SERVER]

  def "test buildDomainReference with no sub-object nesting"() {
    given: 'a domain class and object'
    def parent = new SampleParent(name: 'ABC', allFieldsDomain: new AllFieldsDomain(name: 'AFD'))

    when: 'the property is resolved to a domain reference'
    def ref = DomainReference.buildDomainReference('name', parent)

    then: 'the right domain reference is built'
    ref.model.is(parent)
    ref.fieldName == 'name'
    ref.value == 'ABC'
  }

  @Rollback
  def "test buildDomainReference with sub-object nesting"() {
    given: 'a domain class and real foreign reference'
    def afd = new AllFieldsDomain(name: 'AFD').save()
    def parent = new SampleParent(name: 'ABC', allFieldsDomain: afd)

    when: 'the property is resolved to a domain reference'
    def ref = DomainReference.buildDomainReference('allFieldsDomain.name', parent)

    then: 'the right domain reference is built'
    ref.model instanceof AllFieldsDomain
    ref.fieldName == 'name'
    ref.value == 'AFD'
    ref.clazz == String
  }

  def "test buildDomainReference with sub-object nesting and a null sub-object"() {
    given: 'a domain class and object'
    def parent = new SampleParent(name: 'ABC')

    when: 'the property is resolved to a domain reference'
    def ref = DomainReference.buildDomainReference('allFieldsDomain.name', parent)

    then: 'the right domain reference is built'
    ref.model == null
    ref.fieldName == 'name'
    ref.value == null
    ref.clazz == String
  }

  @Rollback
  def "test buildDomainReference with sub-object nesting and a null value"() {
    given: 'a domain class and real foreign reference'
    def afd = new AllFieldsDomain(name: 'AFD').save()
    def parent = new SampleParent(name: 'ABC', allFieldsDomain: afd)

    when: 'the property is resolved to a domain reference'
    def ref = DomainReference.buildDomainReference('allFieldsDomain.title', parent)

    then: 'the right domain reference is built'
    ref.model instanceof AllFieldsDomain
    ref.fieldName == 'title'
    ref.value == null
    ref.clazz == String
  }

  def "test buildDomainReference fails gracefully with nesting two levels deep"() {
    given: 'a domain class and object'
    def parent = new SampleParent(name: 'ABC', allFieldsDomain: new AllFieldsDomain(title: 'AFD'))

    when: 'the property is resolved to a domain reference'
    DomainReference.buildDomainReference('allFieldsDomain.name.dummy', parent)

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['allFieldsDomain.name.dummy'])
  }

  def "test buildDomainReference fails gracefully  with invalid sub object name"() {
    given: 'a domain class and object'
    def parent = new SampleParent(name: 'ABC', allFieldsDomain: new AllFieldsDomain(title: 'AFD'))

    when: 'the property is resolved to a domain reference'
    DomainReference.buildDomainReference('gibberish.name', parent)

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['property', 'gibberish', 'SampleParent', 'name'])
  }

  @Rollback
  def "test buildDomainReference and getValue fails gracefully with invalid sub element name"() {
    given: 'a domain class and real foreign reference'
    def afd = new AllFieldsDomain(name: 'AFD').save()
    def parent = new SampleParent(name: 'ABC', allFieldsDomain: afd)

    when: 'the property is resolved to a domain reference'
    def ref = DomainReference.buildDomainReference('allFieldsDomain.gibberish', parent)
    ref.getValue()

    then: 'the right exception is thrown'
    def ex = thrown(Throwable)
    UnitTestUtils.assertExceptionIsValid(ex, ['property', 'gibberish', 'AllFieldsDomain', 'allFieldsDomain.gibberish'])
  }

  def "test buildDomainReference works with domain object property reference"() {
    when: 'the property is resolved to a domain reference'
    def ref = DomainReference.buildDomainReference('AllFieldsDomain.title')

    then: 'the reference is correct'
    ref.domainClass == AllFieldsDomain
    ref.fieldName == 'title'
    ref.fieldDefinition.type == String
  }

  def "test buildDomainReference gracefully handles when the domain is not found"() {
    when: 'the property is resolved to a domain reference'
    DomainReference.buildDomainReference('String.title')

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['String', 'title'])
  }

  def "test buildDomainReference gracefully handles when the property is not in the domain"() {
    when: 'the property is resolved to a domain reference'
    DomainReference.buildDomainReference('AllFieldsDomain.gibberish')

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['AllFieldsDomain', 'gibberish'])
  }

}
