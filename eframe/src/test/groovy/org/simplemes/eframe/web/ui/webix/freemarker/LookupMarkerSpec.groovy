/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.controller.SampleParentController

/**
 * Tests.
 */
class LookupMarkerSpec extends BaseMarkerSpecification {

  def "verify that marker works for the simple case"() {
    expect: 'the lookup works'
    execute(source: '<@efLookup key="addLogger.title"/>', controllerClass: SampleParentController) == lookup('addLogger.title')
  }

  def "verify that marker works with arguments"() {
    //searchResultSummary.label={0} results ({1}ms)
    when: 'the message is looked up'
    def s = execute(source: '<@efLookup key="searchResultSummary.label" arg1="xyz" arg2="abc"/>',
                    controllerClass: SampleParentController)

    then: 'the value is correct'
    s == lookup('searchResultSummary.label', null, 'xyz', 'abc')
  }

  def "verify that marker detects missing key"() {
    when: 'the lookup works'
    execute(source: '<@efLookup/>', controllerClass: SampleParentController)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efLookup', 'key'])
  }

  def "verify that marker detects bad argument index"() {
    when: 'the lookup works'
    execute(source: """<@efLookup key="1" $arg="XYZ"/>""", controllerClass: SampleParentController)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efLookup', 'arg', error])

    where:
    arg    | error
    'argX' | "'X'"
    'arg'  | "''"
  }
}
