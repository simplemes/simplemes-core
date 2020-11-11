/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.UnitTestUtils

/**
 * Tests.
 */
class ButtonMarkerSpec extends BaseMarkerSpecification {

  def "verify that the marker generates the button - basic scenario"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efButton label="list.menu.label" click="ABC();"/>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    //{view: "button", value: "Log Failure", type: "form", click: "${variable}.log();", width: tk.pw("11em")}
    and: 'the field type is valid'
    def buttonText = TextUtils.findLine(page, 'view: "button"')
    JavascriptTestUtils.extractProperty(buttonText, 'view') == 'button'

    and: 'label is used'
    def label = lookup('list.menu.label')
    JavascriptTestUtils.extractProperty(buttonText, 'label') == label

    and: 'the onClick handler is used'
    JavascriptTestUtils.extractProperty(buttonText, 'click') == 'ABC();'

    and: 'the correct type is used'
    JavascriptTestUtils.extractProperty(buttonText, 'type') == 'form'

    and: 'the width is based on the label length'
    JavascriptTestUtils.extractProperty(buttonText, 'width') == """tk.pw("${label.size()}em")"""

    and: 'the tooltip is used'
    JavascriptTestUtils.extractProperty(buttonText, 'tooltip') == lookup('list.menu.tooltip')
  }

  def "verify that the marker works in a button group with fields and multiple buttons"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efField field="query" maxLength=255 width="40em" label="searchQuery.label"/>
        <@efField field="query2" maxLength=255 width="40em" label="searchQuery.label"/>
        <@efButtonGroup>
            <@efButton id='searchButton' label="searchButton.label" click="submitSearch()" />
            <@efButton id='searchButton2' label="searchButton.label" click="submitSearch2()" />
        </@efButtonGroup>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)
  }

  def "verify that the marker works in a button group with fields and multiple buttons - dashboard case"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="filter" dashboard=true>
        <@efField field="query" maxLength=255 width="40em" label="searchQuery.label"/>
        <@efField field="query2" maxLength=255 width="40em" label="searchQuery.label"/>
        <@efButtonGroup>
            <@efButton id='searchButton' label="searchButton.label" click="submitSearch()" />
            <@efButton id='searchButton2' label="searchButton.label" click="submitSearch2()" />
        </@efButtonGroup>
      </@efForm>
    """

    def page = execute(source: src, dataModel: [params: [_variable: 'A']])

    then: 'the javascript is legal'
    checkPage(page)
  }

  def "verify that the marker supports the size attribute"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efButton label="list.menu.label" click="ABC();" size='1.356'/>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the size is used'
    JavascriptTestUtils.extractProperty(page, 'height') == """tk.ph("1.356em")"""
    JavascriptTestUtils.extractProperty(page, 'inputHeight') == """tk.ph("1.356em")"""
  }

  def "verify that the marker supports the css attribute"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efButton label="list.menu.label" click="ABC();" css='style-abc style-xyz'/>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the css is used'
    JavascriptTestUtils.extractProperty(page, 'css').contains("style-abc style-xyz")
  }

  def "verify that the marker handles double-quotes in click script"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efButton label="list.menu.label" click='ABC("XYZ");'/>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the script quote is escaped'
    page.contains(JavascriptUtils.escapeForJavascript('ABC("XYZ");'))
  }

  def "verify that the marker can be used inside of a button group marker"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efButtonGroup>
          <@efButton label="list.menu.label" click='ABC();'/>
        </@efButtonGroup>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the button group is created with the button'
    def columnsBlock = JavascriptTestUtils.extractBlock(page, 'cols: [')
    def buttonBlock = JavascriptTestUtils.extractBlock(columnsBlock, '{view:')
    JavascriptTestUtils.extractProperty(buttonBlock, 'label') == lookup('list.menu.label')
  }

  def "verify that the marker supports the url attribute"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efButton label="list.menu.label" link="/ABC"/>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'the css is used'
    JavascriptTestUtils.extractProperty(page, 'click') == """window.location='/ABC'"""
  }

  def "verify that the marker detects missing url and onClick"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efButton label="abc"/>
      </@efForm>
    """
    execute(source: src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efButton', 'link', 'click'])
  }

  def "verify that the marker detects when not used inside of an efForm"() {
    when: 'the marker is built'
    def src = """
      <@efButton label="abc"/>
    """
    execute(source: src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efButton', 'efForm'])
  }

  def "verify that the marker supports the type option - undo"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="logFailure" dashboard=true>
        <@efField field="order" label="Order/LSN" value="M1008" width=20 labelWidth='35%'>
          <@efButton type='undo' click='ABC();' />
        </@efField>
      </@efForm>
    """

    def page = execute(source: src, dataModel: [params: [_variable: 'A']])

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the right HTML element is generated'
    def viewText = JavascriptTestUtils.extractBlock(page, '{view: "template"')
    JavascriptTestUtils.extractProperty(viewText, 'view') == 'template'
    page.contains('<button')

    and: 'the right size is used'
    JavascriptTestUtils.extractProperty(viewText, 'width') == """tk.pw("1.5em")"""
    JavascriptTestUtils.extractProperty(viewText, 'height') == """tk.ph("1.5em")"""

    and: 'the right HTML element is used'
    def htmlText = JavascriptTestUtils.extractProperty(viewText, 'template')
    htmlText.contains('<button')
    htmlText.contains('type="button"')
    htmlText.contains('id="undoButton"')
    htmlText.contains('class="undo-button-disabled"')

    and: 'the click option is used'
    htmlText.contains('onclick="ABC();"')
  }

  def "verify that the marker supports undo button with options - spacer tooltip id css label"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="logFailure" dashboard=true>
        <@efField field="order" label="Order/LSN" value="M1008" width=20 labelWidth='35%'>
          <@efButton type='undo' click='ABC();' spacer='after' tooltip='abc_tip' id='uniqueID' css='the-css-class' label='XYZ'/>
        </@efField>
      </@efForm>
    """

    def page = execute(source: src, dataModel: [params: [_variable: 'A']])

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the spacer is used after the button'
    def viewLine = TextUtils.findLine(page, '{view: "template"')
    viewLine.contains('},{}')

    and: 'the tooltip is used'
    def viewText = JavascriptTestUtils.extractBlock(page, '{view: "template"')
    def htmlText = JavascriptTestUtils.extractProperty(viewText, 'template')
    htmlText.contains('title="abc_tip"')

    and: 'the id is used'
    htmlText.contains('id="uniqueID"')

    and: 'the css class is used'
    htmlText.contains('class="the-css-class"')

    and: 'the label is used'
    htmlText.contains('>XYZ<')
  }

  def "verify that the marker supports spacer before option - undo case"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="logFailure" dashboard=true>
        <@efField field="order" label="Order/LSN" value="M1008" width=20 labelWidth='35%'>
          <@efButton type='undo' click='ABC();' spacer='before'/>
        </@efField>
      </@efForm>
    """

    def page = execute(source: src, dataModel: [params: [_variable: 'A']])

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptsOnPage(page)

    and: 'the spacer is used after the button'
    def viewLine = TextUtils.findLine(page, '{view: "template"')
    viewLine.contains('{}, {view')
  }

  def "verify that the marker generates standard button - with spacer options"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="edit">
        <@efButton spacer="$spacer" click="ABC();"/>
      </@efForm>
    """

    def page = execute(source: src)

    then: 'the javascript is legal'
    checkPage(page)

    and: 'spacer is correct'
    def viewLine = TextUtils.findLine(page, '{view: "button"')
    viewLine.contains(contains)

    where:
    spacer         | contains
    'before'       | '{},{view'
    'after'        | '},{}'
    'before after' | '},{}'
    'after before' | '{},{view'
  }

  def "verify that the marker detects incorrect type value"() {
    when: 'the marker is built'
    def src = """
      <@efForm id="logFailure" dashboard=true>
        <@efField field="order" label="Order/LSN"/>
        <@efButton type='gibberish' click='ABC();'/>
      </@efForm>
    """
    execute(source: src, dataModel: [params: [_variable: 'A']])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efButton', 'type', 'gibberish'])
  }

}
