package org.simplemes.eframe.dashboard.javascript

import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.UnitTestUtils
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of the dashboard.js methods related to the finished() method.
 */
@IgnoreIf({ !sys['geb.env'] })
class DashboardJSFinishedGUISpec extends BaseDashboardSpecification {

  def "verify that the finished method can display a message"() {
    given: 'a dashboard with 1 button to open 2 activities in sequence'
    def activity1 = '''
      <@efForm id="logFailure" dashboard=true>
        <@efButtonGroup>
          <@efButton id="DONE" label="Done" click="dashboard.finished({panel: '${params._panel}',info: 'Finished1'});"/>
        </@efButtonGroup>
      </@efForm>
    '''
    def activity2 = '''
      <@efForm id="logFailure" dashboard=true>
        <@efButtonGroup>
          <@efButton id="DONE2" label="Done2" click="dashboard.finished({panel: '${params._panel}',info: 'Finished2'});"/>
        </@efButtonGroup>
      </@efForm>
    '''
    def buttons = [[buttonID: 'B10', label: 'B10', url: activity1, panel: 'B'],
                   [buttonID: 'B10', label: 'B10', url: activity2, panel: 'B']]
    buildDashboard(defaults: [BUTTON_PANEL, 'Content B'],
                   buttons: buttons)

    when: 'the dashboard is displayed and the activity sequence is started'
    displayDashboard()
    button('B10').click()
    waitForCompletion()

    then: 'no finished messages are displayed yet'
    messages.text() == ''

    when: 'the first activity is finished'
    button('DONE').click()
    waitForCompletion()

    then: 'only the first finished message is displayed'
    messages.text().contains('Finished1')
    !messages.text().contains('Finished2')

    when: 'the second activity is finished'
    button('DONE2').click()
    waitForCompletion()

    then: 'the first finished message is displayed'
    messages.text().contains('Finished2')

    and: 'the original content is re-displayed'
    panel('B').text().contains('Content B')
  }

  def "verify that the finished method can cancel a sequence of activities on a button"() {
    given: 'a dashboard with 1 button to open 2 activities in sequence'
    def activity1 = '''
      <@efForm id="logFailure" dashboard=true>
        <@efButtonGroup>
          <@efButton id="CANCEL" label="Cancel" click="dashboard.finished({panel: '${params._panel}', cancel: true,error: 'Cancelled1'});"/>
        </@efButtonGroup>
      </@efForm>
    '''
    def activity2 = '''<script>dashboard.finished({panel: '${params._panel}',info: 'Finished2'});</script>'''
    def buttons = [[buttonID: 'B10', label: 'B10', url: activity1, panel: 'B'],
                   [buttonID: 'B10', label: 'B10', url: activity2, panel: 'B']]
    buildDashboard(defaults: [BUTTON_PANEL, 'Content B'],
                   buttons: buttons)

    when: 'the dashboard is displayed and the activity sequence is started'
    displayDashboard()
    button('B10').click()
    waitForCompletion()

    then: 'no finished messages are displayed yet'
    messages.text() == ''

    when: 'the first activity is finished'
    button('CANCEL').click()
    waitForCompletion()

    then: 'only the first finished message is displayed'
    messages.text().contains('Cancelled1')

    and: 'the second activity is not executed'
    !messages.text().contains('Finished2')

    and: 'the original content is re-displayed'
    panel('B').text().contains('Content B')
  }

  def "verify that the finished method gracefully detects a bad panel"() {
    given: 'a dashboard with 1 button to open 2 activities in sequence'
    def activity1 = '''<script> dashboard.finished('GIBBERISH');</script> '''
    def buttons = [[buttonID: 'B10', label: 'B10', url: activity1, panel: 'B']]
    buildDashboard(defaults: [BUTTON_PANEL, 'Content B'],
                   buttons: buttons)

    when: 'the dashboard is displayed and the activity sequence is started'
    displayDashboard()
    button('B10').click()
    waitForCompletion()

    then: 'no finished messages are displayed yet'
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['GIBBERISH', 'finished', 'invalid', 'panel'])
  }

  def "verify that the finished method gracefully detects missing panel"() {
    given: 'a dashboard with 1 button to open 2 activities in sequence'
    def activity1 = '''<script> dashboard.finished({info: 'Finished1'});</script> '''
    def buttons = [[buttonID: 'B10', label: 'B10', url: activity1, panel: 'B']]
    buildDashboard(defaults: [BUTTON_PANEL, 'Content B'],
                   buttons: buttons)

    when: 'the dashboard is displayed and the activity sequence is started'
    displayDashboard()
    button('B10').click()
    waitForCompletion()

    then: 'no finished messages are displayed yet'
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['finished', 'invalid', 'panel'])
  }


}
