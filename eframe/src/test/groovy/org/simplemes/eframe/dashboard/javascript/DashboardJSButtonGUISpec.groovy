/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.javascript

import org.simplemes.eframe.test.BaseDashboardSpecification
import spock.lang.IgnoreIf

/**
 * Tests of the dashboard.js methods related to buttons.
 */
@IgnoreIf({ !sys['geb.env'] })
class DashboardJSButtonGUISpec extends BaseDashboardSpecification {

  def "verifies that basic button configuration and operation works"() {
    given: 'a button with all of the configuration fields'
    def buttons = [[label: 'pass.label', url: 'PASS Content', panel: 'A',
                    title: 'pass.title', css: 'dummy-css', buttonID: 'PASS'],
                   [label: 'fail.label', url: 'FAIL Content', panel: 'A',
                    size : 2.0, buttonID: 'FAIL']]

    and: 'a dashboard with the button'
    buildDashboard(defaults: [BUTTON_PANEL], buttons: buttons)

    when: 'the dashboard is displayed'
    displayDashboard()

    then: 'the buttons are displayed with the right text'
    button('PASS').text() == lookup('pass.label')
    getTooltip('PASS') == 'pass.title'

    and: 'the buttons has the right size and css properties'
    button('PASS').classes().contains('dummy-css')
    button('FAIL').height == button('PASS').height * 2

    when: 'the button is clicked'
    button('PASS').click()
    waitForCompletion()

    then: 'the button executes correctly'
    panel('A').text() == 'PASS Content'
  }

  def "verifies that dashboard with one button loads 2 activities in 2 panels"() {
    given: 'a dashboard with 1 button to open 2 activities in 2 panels'
    def buttons = [[buttonID: 'B10', label: 'B10', url: buildSimplePanel(text: 'Content W', finished: 'FW'), panel: 'B'],
                   [buttonID: 'B10', label: 'B10', url: buildSimplePanel(text: 'Content X', finished: 'FX'), panel: 'C'],
                   [buttonID: 'B10', label: 'B10', url: buildSimplePanel(text: 'Content Y', finished: 'FY'), panel: 'B'],
                   [buttonID: 'B10', label: 'B10', url: buildSimplePanel(text: 'Content Z', finished: 'FZ'), panel: 'C']]
    buildDashboard(defaults: [BUTTON_PANEL, 'Content B', 'Content C'],
                   buttons: buttons)
    when: 'the dashboard is displayed and the button is pressed'
    displayDashboard()
    button('B10').click()
    waitForCompletion()

    then: 'the first pages are displayed in the two panels'
    panel('B').text().contains('Content W')
    panel('C').text().contains('Content X')

    when: 'the first activity in one panel is finished'
    button('FW').click()
    waitForCompletion()

    then: 'the next activity is displayed in one panel and the other panel is left as-is'
    panel('B').text().contains('Content Y')
    panel('C').text().contains('Content X')

    when: 'the first activity in the other panel is finished'
    button('FX').click()
    waitForCompletion()

    then: 'the next activity is displayed in one panel and the other panel is left as-is'
    panel('B').text().contains('Content Y')
    panel('C').text().contains('Content Z')

    when: 'the second activity in the first panel is finished'
    button('FY').click()
    waitForCompletion()

    then: 'the next activity is displayed in one panel and the other panel is left as-is'
    panel('B').text().contains('Content B')
    panel('C').text().contains('Content Z')

    when: 'the second activity in the other panel is finished'
    button('FZ').click()
    waitForCompletion()

    then: 'the defaults are displayed in the panels'
    panel('B').text().contains('Content B')
    panel('C').text().contains('Content C')
  }

  def "verify that one button with two gui activities work in sequence"() {
    given: 'a dashboard with two activities on a button'
    def guiActivity = '''
    <script>
      <@efForm id="logFailure" dashboard="buttonHolder">  
        <@efField field="serial1" value="RMA1001" width=20/>
          <@efButtonGroup>
            <@efButton id="DONE" label="Done" click="dashboard.finished({panel: '${params._panel}',info: 'Finished1'});"/>
          </@efButtonGroup>
      </@efForm>
    </script>
    '''
    def guiActivity2 = '''
    <script>
      <@efForm id="logFailure" dashboard="buttonHolder">  
        <@efField field="serial2" value="RMA1002" width=20/>
          <@efButtonGroup>
            <@efButton id="DONE" label="Done" click="dashboard.finished({panel: '${params._panel}',info: 'Finished1'});"/>
          </@efButtonGroup>
      </@efForm>
    </script>
    '''
    buildDashboard(defaults: [BUTTON_PANEL], buttons: [[guiActivity, guiActivity2]])

    when: 'the dashboard is displayed'
    displayDashboard()
    clickDashboardButton(0)
    waitForCompletion()

    then: 'the GUI activity is displayed in the second panel'
    panel('A').text().contains('serial1')

    when: 'the activity is finished'
    clickButton('DONE')
    waitForPanelsToLoad()

    then: 'the second GUI activity is displayed'
    panel('A').text().contains('serial2')

    when: 'the activity is finished'
    clickButton('DONE')
    waitForPanelsToLoad()

    then: 'the original activity is re-displayed'
    isButtonPanelDisplayed()
  }

  def "verify that one button with non-gui and gui activities work in sequence"() {
    given: 'a dashboard with two activities on a button'
    def guiActivity = '''
    <script>
      <@efForm id="logFailure" dashboard="buttonHolder">  
        <@efField field="serial1" value="RMA1001" width=20/>
          <@efButtonGroup>
            <@efButton id="DONE" label="Done" click="dashboard.finished({panel: '${params._panel}',info: 'Finished1'});"/>
          </@efButtonGroup>
      </@efForm>
    </script>
    '''
    def nonGUIActivity = '''<script>
      ${params._variable}.execute =  function() {
        ef.displayMessage({info: 'Finished Non-GUI'});
        dashboard.finished('${params._panel}');
      }
    </script>  
    '''
    buildDashboard(defaults: [BUTTON_PANEL], buttons: [[nonGUIActivity, guiActivity]])

    when: 'the dashboard is displayed'
    displayDashboard()
    clickDashboardButton(0)
    waitForCompletion()

    then: 'the non-GUI activity is finished'
    messages.text.contains('Finished Non-GUI')

    and: 'the next GUI activity is displayed'
    panel('A').text().contains('serial1')

    when: 'the activity is finished'
    clickButton('DONE')
    waitForPanelsToLoad()

    then: 'the original activity is re-displayed'
    isButtonPanelDisplayed()
  }

  def "verify that one button with non-gui activity works and does not affect the displayed panel"() {
    given: 'a dashboard with 1 non-gui activity'
    def activity = '''
      <script>
        ${params._variable}.execute =  function() {
          ef.displayMessage({info: 'Non-GUI Message'});
          dashboard.finished('${params._panel}');
        }
      </script>
    '''
    buildDashboard(defaults: [BUTTON_PANEL, 'Content B'], buttons: [activity, 'Other GUI Activity'])

    when: 'the dashboard is displayed and other GUI activity is triggered'
    displayDashboard()
    clickDashboardButton(1)
    waitForCompletion()

    then: 'the GUI activity is displayed in the second panel'
    panel('B').text().contains('Other GUI Activity')

    when: 'the non-gui activity is executed'
    clickDashboardButton(0)
    waitForCompletion()

    then: 'the GUI activity in the second panel is unchanged'
    panel('B').text().contains('Other GUI Activity')

    and: 'the non-gui activity executed'
    messages.text().contains('Non-GUI Message')
  }

  def "verify that one button with non-gui activity that triggers a server error response displays the error"() {
    given: 'a dashboard with 1 non-gui activity that throws a server-side exception'
    def activity = '''
      <script>
        dashboard.postActivity({throwException: "An Exception Message"},'/test/dashboard/echo','${params._panel}');
      </script>
    '''
    buildDashboard(defaults: [BUTTON_PANEL, 'Content B'], buttons: [activity])

    and: 'disable server-side stack trace logging to reduce console output'
    disableStackTraceLogging()

    when: 'the dashboard is displayed and the non-GUI activity is triggered'
    displayDashboard()
    clickDashboardButton(0)
    waitForCompletion()

    then: 'the error message is displayed'
    messages.text().contains('An Exception Message')
  }

  def "verify that clickButton works"() {
    given: 'a dashboard with two activities - one to display call clickButton and another to respond to the clickButton'
    def activity1 = '''
      <script>
        ${params._variable}.execute =  function() {
          dashboard.finished("${params._panel}");
          ef.displayMessage({info: 'Finished1'});
          dashboard.clickButton('B1');
          ef.displayMessage({info: 'Click2'});
        }
      </script>
    '''
    def activity2 = '''
      <script>
        ${params._variable}.execute =  function() {
          ef.displayMessage({info: "Non-GUI Message"});
          dashboard.finished("${params._panel}");
        }
      </script>
    '''
    buildDashboard(defaults: [BUTTON_PANEL, 'Content B'], buttons: [activity1, activity2])

    when: 'the dashboard is displayed and other GUI activity is triggered'
    displayDashboard()
    clickDashboardButton(0)
    waitForCompletion()

    then: 'the second activity is executed which displays a message'
    messages.text().contains('Non-GUI Message')
  }

}
