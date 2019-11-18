package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.MockControllerUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.UnitTestUtils
import sample.controller.AllFieldsDomainController
import sample.controller.SampleParentController
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class BaseWidgetSpec extends BaseWidgetSpecification {

  def "verify that escape works on supported cases"() {
    expect: 'the HTML value to be escaped for safe display'
    new BaseWidget(new WidgetContext()).escape(input) == res

    where:
    input                  | res
    "<script>bad</script>" | "&lt;script&gt;bad&lt;/script&gt;"
    "<"                    | "&lt;"
    ">"                    | "&gt;"
    "\""                   | "&quot;"
    "&"                    | "&amp;"
  }

  def "verify that the base apply method fails"() {
    when: 'apply is called'
    new BaseWidget(new WidgetContext()).build()

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['BaseWidget', 'build()'])
  }

  def "verify that a baseWidget constructor makes the widgetContext and it properties available"() {
    when: 'the widget is built using a widget context'
    def baseWidget = new BaseWidget(new WidgetContext(parameters: [id: 'theID']))

    then: 'the base widget has access to the options and value'
    baseWidget.widgetContext.parameters == [id: 'theID']
  }

  def "verify that lookup works with locale"() {
    when: ''
    def s = new BaseWidget(new WidgetContext()).lookup('home.label')

    then: ''
    s == lookup('home.label')
  }

  def "verify that the default HTML ID uses the controller prefix, if provided"() {
    when: 'the widget is created'
    def baseWidget = new BaseWidget(new WidgetContext(parameters: [controller: 'AllFieldsDomain']))

    then: 'the ID created is correct'
    baseWidget.id == 'allFieldsDomainBase'
  }

  def "verify that the default HTML ID uses the class name for the ID"() {
    when: 'the widget is created'
    def baseWidget = new BaseWidget(new WidgetContext())

    then: 'the default ID is correct'
    baseWidget.id == 'base'
  }

  def "verify that getDomainClass works"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the widget is created'
    def baseWidget = new BaseWidget(new WidgetContext(controllerClass: AllFieldsDomainController))

    then: 'the domain class is correct'
    baseWidget.domainClass == AllFieldsDomain
  }

  def "verify that getEffectiveDomainClass works with no controller override parameter"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the widget is created'
    def baseWidget = new BaseWidget(new WidgetContext(controllerClass: AllFieldsDomainController))

    and: 'the option to allow override is set'
    baseWidget.controllerOverrideAllowed = true

    then: 'the domain class is correct'
    baseWidget.domainClass == AllFieldsDomain
  }

  def "verify that getEffectiveDomainClass works with controller override parameter"() {
    given: 'mocked domain and controller utils'
    new MockDomainUtils(this, [AllFieldsDomain, SampleParent]).install()
    new MockControllerUtils(this, [AllFieldsDomainController, SampleParentController]).install()

    when: 'the widget is created by one controller'
    def widgetContext = new WidgetContext(parameters: [controller: 'SampleParentController'], controllerClass: AllFieldsDomainController)
    def baseWidget = new BaseWidget(widgetContext)

    and: 'the option to allow override is set'
    baseWidget.controllerOverrideAllowed = true

    then: 'the domain class reflects the controller parameter value'
    baseWidget.domainClass == SampleParent
  }

  def "verify that getEffectiveControllerClass works with no controller override parameter"() {
    when: 'the widget is created with the specific controller'
    def baseWidget = new BaseWidget(new WidgetContext(controllerClass: AllFieldsDomainController))

    and: 'the option to allow override is set'
    baseWidget.controllerOverrideAllowed = true

    then: 'the domain class is correct'
    baseWidget.controllerClass == AllFieldsDomainController
  }

  def "verify that getEffectiveControllerClass works with controller override parameter"() {
    given: 'mocked domain and controller utils'
    new MockDomainUtils(this, [AllFieldsDomain, SampleParent]).install()
    new MockControllerUtils(this, [AllFieldsDomainController, SampleParentController]).install()

    when: 'the widget is created by one controller'
    def widgetContext = new WidgetContext(parameters: [controller: 'SampleParentController'], controllerClass: AllFieldsDomainController)
    def baseWidget = new BaseWidget(widgetContext)

    and: 'the option to allow override is set'
    baseWidget.controllerOverrideAllowed = true

    then: 'the controller class reflects the controller parameter value'
    baseWidget.controllerClass == SampleParentController
  }

}
