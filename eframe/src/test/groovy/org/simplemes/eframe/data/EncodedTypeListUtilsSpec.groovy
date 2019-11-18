package org.simplemes.eframe.data

import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.system.EnabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class EncodedTypeListUtilsSpec extends BaseSpecification {

  def "verify that getAllValues works for a core base class"() {
    when: 'the list is returned'
    def list = EncodedTypeListUtils.instance.getAllValues(BasicStatus)

    then: 'the list of choices is returned'
    list == [EnabledStatus.instance, DisabledStatus.instance]
  }

/*
  def "verify that getAllValues works for values in an addition"() {
    given: 'a simple mocked addition'
    def src = """
    package sample
    
    import org.simplemes.eframe.system.BasicStatus
    
    class TestAddition {
      static addedOptions = [(BasicStatus):[SampleBasicStatus]]
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockAdditionHelper(this, [clazz]).install()

    when: 'the list is returned'
    def list = EncodedTypeListUtils.instance.getAllValues(BasicStatus)

    then: 'the list of choices is returned'
    list == [EnabledStatus.instance, DisabledStatus.instance, SampleBasicStatus.instance]
  }
*/


  def "verify that getAllValues gracefully fails with wrong base class type in addition"() {
    when: 'the list is returned'
    EncodedTypeListUtils.instance.getAllValues(String)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['String', 'ChoiceListInterface'])
  }

}
