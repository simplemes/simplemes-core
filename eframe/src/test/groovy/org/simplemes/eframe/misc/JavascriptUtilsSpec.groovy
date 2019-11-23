package org.simplemes.eframe.misc

import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.UnitTestUtils
import spock.lang.Shared

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class JavascriptUtilsSpec extends BaseSpecification {
  def "verify that escape works on supported cases - normal mode"() {
    expect: 'the HTML value to be escaped for safe display'
    JavascriptUtils.escapeForJavascript(input, false) == res

    where:
    input                  | res
    "<script>bad</script>" | "<script>bad<\\/script>"
    "<"                    | "<"
    "\""                   | '\\"'
    ""                     | ""
    null                   | ""
  }

  def "verify that escape works on supported cases - label mode"() {
    expect: 'the HTML value to be escaped for safe display'
    JavascriptUtils.escapeForJavascript(input, true) == res

    where:
    input                  | res
    "<script>bad</script>" | "&lt;script>bad&lt;\\/script>"
    "<"                    | "&lt;"
    "\""                   | '\\"'
    ""                     | ""
  }

  @Shared
  def dateOnly = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
  @Shared
  def date = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that formatForObject works on supported cases"() {
    given: 'some values'

    expect: 'the HTML value to be escaped for safe display'
    JavascriptUtils.formatForObject(value, format.instance) == res

    where:
    value    | format                | res
    'ABC'    | StringFieldFormat     | '"ABC"'
    ''       | StringFieldFormat     | '""'
    null     | StringFieldFormat     | null
    237      | IntegerFieldFormat    | '237'
    437L     | LongFieldFormat       | '437'
    23.2     | BigDecimalFieldFormat | '23.2'
    true     | BooleanFieldFormat    | 'true'
    dateOnly | DateOnlyFieldFormat   | "\"${ISODate.format(dateOnly)}\""
    date     | DateFieldFormat       | "\"${ISODate.format(date)}\""
    //new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)    | DateOnlyFieldFormat   | ISODate.format(new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS))
    //new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS) | DateFieldFormat       | ISODate.format(new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS))
  }

  def "verify that buildJavascriptObject supports correct field types"() {
    given: 'a list of maps'
    def l = [[name: 'abc', title: 'Some Quotes"', count: 237],
             [name: 'xyz', qty: 1.2, enabled: false]]

    when: 'the list is converted to JS code'
    def s = JavascriptUtils.buildJavascriptObject(l)

    then: 'the script is legal JS'
    JavascriptTestUtils.checkScriptsOnPage("var x = $s")

    and: 'the right elements are in the string'
    s.contains('[')
    s.contains(']')
    s.contains('"name":"abc"')
    s.contains('"title":"Some Quotes\\\""')
    s.contains('"count":237')
    s.contains('"qty":1.2')
    s.contains('"enabled":false')

  }

  def "verify that formatMultilineHTMLString works on supported cases"() {
    expect: 'the HTML value to be escaped for safe display'
    JavascriptUtils.formatMultilineHTMLString(input) == res

    where:
    input            | res
    'abc'            | '"abc"'
    '"abc'           | '"\\"abc"'
    '''abc\nxyz'''   | '"abc"+\n"xyz"'
    '''abc\n\rxyz''' | '"abc"+\n"xyz"'
    'abc<script>'    | '"abc&lt;script&gt;"'
  }

  def "verify that escapeHTMLForJavascript works on supported cases"() {
    expect: 'the HTML value to be escaped for safe display'
    JavascriptUtils.escapeHTMLForJavascript(input) == res

    where:
    input          | res
    null           | ''
    ''             | ''
    'abc'          | 'abc'
    'abc<script>'  | 'abc&lt;script&gt;'
    'abc<sCrIpT>'  | 'abc&lt;script&gt;'
    'abc</script>' | 'abc&lt;/script&gt;'
  }
}
