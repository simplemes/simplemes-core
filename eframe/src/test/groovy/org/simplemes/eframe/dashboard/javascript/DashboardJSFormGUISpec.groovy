/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.javascript


import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.UnitTestUtils
import spock.lang.IgnoreIf

/**
 * Tests of the dashboard.js methods related to form handling (e.g. submitting from activity).
 */
@IgnoreIf({ !sys['geb.env'] })
class DashboardJSFormGUISpec extends BaseDashboardSpecification {

  //static dirtyDomains = [DashboardConfig]

  def "verify that basic postActivity works - single panel scenario"() {
    // Also tests some of BaseDashboardSpecification and DashboardPage elements.
    given: 'a dashboard with a simple submit page'
    def activity = """
      <@efForm id="logFailure" dashboard=true>
        <@efField field="rma" value="RMA1001" width=20/>
        <@efButtonGroup>
          <@efButton id="FAIL" label="Log Failure" click="dashboard.postActivity('logFailure','/test/dashboard/echo','A');"/>
        </@efButtonGroup>
      </@efForm>
    """
    buildDashboard(defaults: [activity])

    when: 'the dashboard is displayed - with a URI argument'
    displayDashboard([workCenter: 'ABC'])

    and: 'the RMA field is filled in - using the AbstractPage element with a dynamic field name'
    textField('rma').input.value('RMA1002')

    and: 'the form is submitted'
    button('FAIL').click()
    waitForCompletion()

    then: 'the response is displayed'
    messages.text().contains('RMA1002')
  }

  def "verify that basic postActivity works - two panel scenario"() {
    given: 'a dashboard with a simple submit page'
    def failActivity = """
      <@efForm id="logFailure" dashboard=true>
        <@efField field="rma" value="RMA1001" width=20/>
        <@efButtonGroup>
          <@efButton id="FAIL" label="Log Failure" click="dashboard.postActivity('logFailure','/test/dashboard/echo','B');"/>
        </@efButtonGroup>
      </@efForm>
    """
    buildDashboard(defaults: [BUTTON_PANEL, "<div id='ABC'>Panel B Content</div>"], buttons: [failActivity])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the default content for the second panel is displayed'
    $('div#ABC').text().contains('Panel B Content')

    and: 'the fail button displays the fail activity in the second panel'
    clickDashboardButton(0)
    waitForCompletion()

    and: 'the form is submitted'
    button('FAIL').click()
    waitForCompletion()

    then: 'the response is displayed'
    messages.text().contains('RMA1001')

    and: 'the default content for the second panel is re-displayed'
    $('div#ABC').text().contains('Panel B Content')
  }

  def "verify that postActivity detects HTTP error"() {
    given: 'a dashboard with a simple submit page'
    def activity = """
      <@efForm id="logFailure" dashboard=true>
        <@efField field="rma" value="RMA1001" width=20/>
        <@efButtonGroup>
          <@efButton id="FAIL" label="Log Failure" click="dashboard.postActivity('logFailure','/sample/gibberish/log','A');"/>
        </@efButtonGroup>
      </@efForm>
    """
    buildDashboard(defaults: [activity])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the form is submitted'
    button('FAIL').click()
    waitForCompletion()

    then: 'the response is displayed'
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['fail', 'gibberish'])
  }

  // TODO: test MPE  - See Legacy DashboardSetupAjaxFormGUISpec.groovy
}
