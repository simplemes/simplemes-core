package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.UnitTestUtils
import sample.controller.SampleParentController

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class HTMLMarkerSpec extends BaseMarkerSpecification {

  def "verify that marker works for the simple case"() {
    when: 'the text is generated'
    def page = execute(source: '<@efForm dashboard=true><@efHTML>ABC</@efHTML></@efForm>',
                       dataModel: [params: [_variable: 'A']],
                       controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the generated text is correct'
    //  ,{margin: 8,view: "label", template: "&nbsp;"}  ,{type: "clean", width: tk.pw('10%'), template: "<b>Work Center</b> <a href='./'>link</a>"}
    JavascriptTestUtils.extractProperty(page, 'type') == 'clean'
    JavascriptTestUtils.extractProperty(page, 'template') == 'ABC'
    JavascriptTestUtils.extractProperty(page, 'width') == 'tk.pw("10%")'
  }


  def "verify that marker works with multiple lines of HTML and quotes"() {
    when: 'the text is generated'
    def src = '''
      <@efForm dashboard=true>
        <@efHTML>
ABC"
XYZ
        </@efHTML>
      </@efForm>
    '''
    def page = execute(source: src,
                       dataModel: [params: [_variable: 'A']],
                       controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the generated text is correct'
    page.contains('"ABC\\""+\n"XYZ"')
  }

  def "verify that marker escapes script tags from content section and variables"() {
    when: 'the text is generated'
    def page = execute(source: '<@efForm dashboard=true><@efHTML><script>ABC${params.X}</@efHTML></@efForm>',
                       dataModel: [params: [_variable: 'A', X: '</script>']],
                       controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the generated text is correct'
    JavascriptTestUtils.extractProperty(page, 'template') == '&lt;script&gt;ABC&lt;/script&gt;'
  }

  def "verify that marker detects when not used in the correct form marker"() {
    when: 'the text is generated'
    execute(source: '<@efForm><@efHTML>ABC</@efHTML></@efForm>',
            dataModel: [params: [_variable: 'A']],
            controllerClass: SampleParentController)


    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efHTML', 'efForm', 'dashboard'])
  }

  def "verify that marker supports the spacer option - true"() {
    when: 'the text is generated'
    def page = execute(source: '<@efForm dashboard=true><@efHTML spacer=true>ABC</@efHTML></@efForm>',
                       dataModel: [params: [_variable: 'A']],
                       controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the spacer is created correctly'
    //    def spacer =  ',{margin: 8,view: "label", template: "&nbsp;"}'
    def spacerText = JavascriptTestUtils.extractBlock(page, '{margin')
    JavascriptTestUtils.extractProperty(spacerText, 'view') == 'label'
    JavascriptTestUtils.extractProperty(spacerText, 'template') == '&nbsp;'

    and: 'it comes before the HTML'
    page.indexOf(spacerText) < page.indexOf('ABC')
  }

  def "verify that marker supports the spacer option - after"() {
    when: 'the text is generated'
    def page = execute(source: '<@efForm dashboard=true><@efHTML spacer="after">ABC</@efHTML></@efForm>',
                       dataModel: [params: [_variable: 'A']],
                       controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'it comes after the HTML'
    def spacerText = JavascriptTestUtils.extractBlock(page, '{margin')
    page.indexOf('ABC') < page.indexOf(spacerText)
  }

  def "verify that marker supports no content gracefully"() {
    when: 'the text is generated'
    def page = execute(source: '<@efForm dashboard=true><@efHTML></@efHTML></@efForm>',
                       dataModel: [params: [_variable: 'A']],
                       controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'and empty string is used for the template value'
    page.contains('template: ""')
  }

  def "verify that marker supports width option"() {
    when: 'the text is generated'
    def page = execute(source: '<@efForm dashboard=true><@efHTML width="5.2%">ABC</@efHTML></@efForm>',
                       dataModel: [params: [_variable: 'A']],
                       controllerClass: SampleParentController)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the width is used'
    JavascriptTestUtils.extractProperty(page, 'width') == 'tk.pw("5.2%")'
  }

}
