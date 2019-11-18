package org.simplemes.eframe.test

import org.xml.sax.SAXParseException

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class HTMLTestUtilsSpec extends BaseSpecification {

  @SuppressWarnings("GStringExpressionWithinString")
  def "verify basic checkHTML use"() {
    expect: 'good HTML passes'
    HTMLTestUtils.checkHTML("<div>text</div>")
    HTMLTestUtils.checkHTML("")
    HTMLTestUtils.checkHTML(null)
    HTMLTestUtils.checkHTML('<ul class="roles" role="alert"><li><message error="${error}"/></li></ul>')

    and: 'ignored line works'
    def s = '''<div>ok text</div>
    <divBad>ok text</div>
    <div>ok text</div>
    '''
    HTMLTestUtils.checkHTML(s, ['<divBad>'])
  }

  def "verify bad HTML is detected"() {
    when: 'bad HTML is checked'
    HTMLTestUtils.checkHTML("<div>text</divX>")

    then: 'an exception is triggered'
    thrown(SAXParseException)
  }

  def "verify basic extractTag use"() {
    expect: 'the extract is called'
    HTMLTestUtils.extractTag(page, tag, end) == result

    where:
    page                                    | tag                  | end   | result
    "junk<input class='ABC'/>ignored"       | "input"              | null  | "<input class='ABC'/>"
    "junk<input class='ABC'/>ignored"       | "input"              | false | "<input class='ABC'/>"
    "junk<input class='ABC' other/>ignored" | "<input class='ABC'" | false | "<input class='ABC' other/>"
    null                                    | "input"              | false | null
    "junk<input class='ABC'/>ignored"       | "xyz"                | false | null
    "junk<input class='ABC' ignored"        | "input"              | false | null
    "junk<div class='ABC'>ignored</div>"    | "div"                | false | "<div class='ABC'>"
    "junk<a class='ABC'>found</a>ignored"   | "a"                  | true  | "<a class='ABC'>found</a>"
    "junk<a class='XYZ'>found</a>ignored"   | "<a class='XYZ'"     | true  | "<a class='XYZ'>found</a>"
  }

  def "verify basic assertTagHasClass use - pass cases"() {
    expect: 'the assertTagHasClass is called with no errors'
    HTMLTestUtils.assertTagHasClass('<div class="required"', 'required')
    HTMLTestUtils.assertTagHasClass('<div class="error required"', 'required')
    HTMLTestUtils.assertTagHasClass('<div class="other error required"', 'error')
  }

  def "verify basic assertTagHasClass use - fail cases"() {
    when: 'the extract is called'
    HTMLTestUtils.assertTagHasClass(tagText, cssClass)

    then: 'an exception is triggered'
    thrown(AssertionError)

    where:
    tagText                     | cssClass
    '<div '                     | 'required'
    ''                          | 'required'
    '<div class="missing quote' | 'required'
    '<div class="missing"'      | 'required'
  }

  def "verify basic assertOrderInText use - pass cases"() {
    expect: 'the assertTagHasClass is called with no errors'
    HTMLTestUtils.assertOrderInText("A B C D", ['A', 'B', 'C', 'D'])
  }

  def "verify basic assertOrderInText use - fail cases"() {
    when: 'the assertTagHasClass is called with errors'
    HTMLTestUtils.assertOrderInText("A B C D", ['A', 'C', 'B'])

    then: 'an exception is triggered'
    thrown(AssertionError)
  }

}
