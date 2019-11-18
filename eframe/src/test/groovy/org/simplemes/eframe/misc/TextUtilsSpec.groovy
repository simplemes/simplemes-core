package org.simplemes.eframe.misc

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
class TextUtilsSpec extends BaseSpecification {

  def "test toStringBase with simple replacements"() {
    expect: 'the toStringBase to work'
    def s = 'ABC'
    TextUtils.toStringBase(null) == 'null'
    TextUtils.toStringBase(s) != s
    TextUtils.toStringBase(s).contains(Integer.toHexString(s.hashCode()))
    TextUtils.toStringBase(s).contains(String.name)
  }

  @SuppressWarnings("GStringExpressionWithinString")
  def "test evaluateGString with simple replacements"() {
    expect: 'the replacement to work'
    TextUtils.evaluateGString(gString, map) == results

    where:
    gString                                   | map                              | results
    'A$day B$month'                           | [day: 'Monday', month: 'May']    | 'AMonday BMay'
    'A${day} B${month}'                       | [day: 'Monday', month: 'May']    | 'AMonday BMay'
    'A${day.toString()} B${month.toString()}' | [day: 'Monday', month: 'May']    | 'AMonday BMay'
    'A${day}'                                 | [day: 'Monday']                  | 'AMonday'
    'A${o.toString()}'                        | [o: new StringBuilder('Monday')] | 'AMonday'
    'A${day.toString()} B${month}'            | [day: 'Monday', month: 'May']    | 'AMonday BMay'
    'A$day B$month'                           | [day: '*', month: 'May']         | 'A* BMay'
    '''A$'{'day'}' B$month'''                 | [day: '*', month: 'May']         | 'A* BMay'
    'A${day} B${month}'                       | [day: '$abc', month: 'May']      | 'A$abc BMay'
    'A${day} B${month}'                       | [dayX: '$abc', month: 'May']     | 'Anull BMay'
  }

  def "all works with evaluateGString"() {
    given: 'a map'
    def map = [day: 'Monday', month: 'May']
    def allString = map.toString()

    expect: 'the all parameter works'
    TextUtils.evaluateGString('A$all', map) == "A$allString"
  }

  def "verify evaluateGString using all with a map with an all property doesn't overwrite the map property"() {
    given: 'a map with an all property'
    def map = [all: 'some', other: 'other']

    when: 'the gString is evaluated'
    def s = TextUtils.evaluateGString('A$all', map)

    then: 'the provided property is used'
    s.contains('some')

    and: 'the rest of the map is not used'
    !s.contains('other')
  }

  @SuppressWarnings("GStringExpressionWithinString")
  def "verify evaluateGString using a POGO with a sub-property "() {
    given: 'a POGO for field testing'
    //noinspection GroovyAssignabilityCheck
    def pogo = new DummyPOGO(field: 'Monday')

    expect: 'the POGO field works'
    TextUtils.evaluateGString('A${pogo.field}', [pogo: pogo]) == 'AMonday'

    and: 'the POGO works as part of a map (nested field test)'
    TextUtils.evaluateGString('A$pogo.field', [pogo: [field: 'Monday']]) == 'AMonday'
  }

  def "test basic findLine"() {
    given: 'a string to search'
    String s = """
       before1 line1  some after1 text
       before2 line2  some after2 text
       before3 line3  some after3 text
       before4 line4  some after4 text
    """

    expect: 'find to handle specific cases'
    TextUtils.findLine(s, 'line1').contains('after1')
    !TextUtils.findLine(s, 'line1').contains('after2')
    TextUtils.findLine(s, 'line1').contains('before1')
    !TextUtils.findLine(s, 'line2').contains('before1')
    TextUtils.findLine(s, 'lineX') == null
  }

  def "test findLine with empty input"() {
    expect:
    assert TextUtils.findLine('', 'line1') == null
    assert TextUtils.findLine(null, 'line1') == null
  }

  def "test findLine with no newLines"() {
    given: 'a page with no lines'
    String s = 'before1 line1  some after1 text'

    expect: 'returns entire page'
    TextUtils.findLine(s, 'line1') == s
  }

  def "test findLine with no newLine after the match"() {
    given: 'a page with no new lines after the match text'
    String s = 'some header\nbefore1 line1  some after1 text'

    expect: 'returns entire page after the match'
    TextUtils.findLine(s, 'line1') == 'before1 line1  some after1 text'
  }

  def "verify that parseNameValuePairs works with supported strings"() {
    expect: 'the parse works'
    TextUtils.parseNameValuePairs(src) == results

    where:
    src                       | results
    "required='true'"         | [required: 'true']
    "required='true'ok='ABC'" | [required: 'true', ok: 'ABC']
    "  "                      | [:]
    ""                        | [:]
    null                      | [:]
    "required=''"             | [required: '']
  }

  def "verify that parseNameValuePairs detects badly formed inputs"() {
    when: 'the parse happens'
    TextUtils.parseNameValuePairs(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['badly', 'formed'])
    UnitTestUtils.assertExceptionIsValid(ex, msgs)

    where:
    src          | msgs
    "required='" | ['required', 'quote', 'near(10)']
    "required"   | ['required', 'near(0)', "'=' not found"]
  }

  def "verify that addFieldForDisplay handles supported cases"() {
    expect: 'the method works'
    TextUtils.addFieldForDisplay(new StringBuilder(initialValue), label, value, options).toString() == result

    where:
    initialValue | label    | value   | options            | result
    ''           | "label1" | 'xyzzy' | [highlight: false] | 'label1: xyzzy'
    ''           | "label1" | 'xyzzy' | [highlight: true]  | '<b>label1</b>: xyzzy'
    'a:1'        | "label1" | 'xyzzy' | [newLine: true]    | 'a:1 <br>label1: xyzzy'
    ''           | "label1" | 'xyzzy' | [maxLength: 10]    | 'label1: xyzzy'
    '123456'     | "label1" | 'xyzzy' | [maxLength: 10]    | '123456 ...'
  }


  /**
   * A simple POGO to test passing objects to the NameSequence format() method.
   */
  private class DummyPOGO {
    String field
  }


}
