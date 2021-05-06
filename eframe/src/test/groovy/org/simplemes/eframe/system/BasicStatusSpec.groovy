package org.simplemes.eframe.system

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class BasicStatusSpec extends BaseSpecification {

  def "verify that sub-classes have the correct methods - isEnabled"() {
    expect: 'the method finds the correct value'
    clazz.instance.enabled == results

    where:
    clazz          | results
    EnabledStatus  | true
    DisabledStatus | false
  }

  def "verify that valueOf works for core statuses"() {
    expect: 'the method finds the correct value'
    BasicStatus.valueOf(id) == results

    where:
    id         | results
    'ENABLED'  | EnabledStatus.instance
    'DISABLED' | DisabledStatus.instance
    null       | null
    'bad'      | null
  }

  def "verify that getDisplayValue works for core statuses"() {
    expect:
    status.instance.getDisplayValue() == results

    where:
    status         | results
    EnabledStatus  | 'label.enabledStatus'
    DisabledStatus | 'label.disabledStatus'
  }

/*
  def "verify that valueOf works for added elements"() {
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

    expect: 'the method finds the correct value'
    BasicStatus.valueOf(id) == results

    where:
    id                            | results
    'ENABLED'                     | EnabledStatus.instance
    'DISABLED'                    | DisabledStatus.instance
    null                          | null
    'bad'                         | null
    SampleBasicStatus.instance.id | SampleBasicStatus.instance
  }
*/


  // test localized string
}
