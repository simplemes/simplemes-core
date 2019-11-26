package org.simplemes.eframe.dashboard.javascript

import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.test.BaseDashboardSpecification
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of the dashboard.js methods related to undo handling.
 * <p>
 * Uses the {@link DashboardTestController#echo(io.micronaut.http.HttpRequest, java.security.Principal)}
 * server action to test the undo actions from the server.
 */
@IgnoreIf({ !sys['geb.env'] })
class DashboardJSUndoGUISpec extends BaseDashboardSpecification {

  def "verify that the dashboard can detect and handle undo action responses from the server"() {
    given: 'a dashboard with 1 non-gui activity that can be undone'
    def startActivity = '''
      <script>
        dashboard.postActivity({order: "XYZZY"},'/test/dashboard/start','${params._panel}', 
        {success: function(res) {ef.displayMessage({info: res})} });
      </script>
    '''
    buildDashboard(defaults: [BUTTON_PANEL, 'Content B'], buttons: [startActivity])

    and: 'the test counters are cleared'
    DashboardTestController.clearStartCounters()

    and: 'disable server-side stack trace logging to reduce console output'
    disableStackTraceLogging()

    when: 'the dashboard is displayed and the non-GUI activity is triggered'
    displayDashboard()

    then: 'the undo button is disabled'
    undoButton.classes().contains('undo-button-disabled')
    !undoButton.classes().contains('undo-button')

    when: 'the increment button is pressed'
    clickButton(0)
    waitForCompletion()

    then: 'the undo button is enabled'
    !undoButton.classes().contains('undo-button-disabled')
    undoButton.classes().contains('undo-button')

    and: 'the counter is correct'
    messages.text().contains('"counter" : 1')

    when: 'the undo is triggered'
    undoButton.click()
    waitForCompletion()

    then: 'the undo is disabled again'
    undoButton.classes().contains('undo-button-disabled')
    !undoButton.classes().contains('undo-button')

    and: 'the counter is correct'
    messages.text().contains('counter=0')
    messages.text().contains('XYZZY')
  }

  def "verify that the dashboard undo stack only holds 8 items"() {
    given: 'a dashboard with 1 non-gui activity that can be undone'
    def startActivity = '''
      <script>
        var order = document.getElementById('order').value; 
        dashboard.postActivity({order: order},'/test/dashboard/start','${params._panel}', 
        {success: function(res) {ef.displayMessage({info: res})} });
      </script>
    '''
    buildDashboard(defaults: [BUTTON_PANEL, 'Content B'], buttons: [startActivity])

    and: 'the test counters are cleared'
    DashboardTestController.clearStartCounters()

    and: 'disable server-side stack trace logging to reduce console output'
    disableStackTraceLogging()

    when: 'the dashboard is displayed and the non-GUI activity is triggered'
    displayDashboard()

    and: '10 start actions are triggered'
    for (count in 1..10) {
      $('#order').value("ABC_$count")
      clickButton(0)
      waitForCompletion()
    }

    then: 'the undo button is enabled'
    !undoButton.classes().contains('undo-button-disabled')
    undoButton.classes().contains('undo-button')

    when: 'the undo is triggered 9 times'
    for (count in 1..9) {
      undoButton.click()
      waitForCompletion()
    }

    then: 'the undo is disabled again'
    undoButton.classes().contains('undo-button-disabled')
    !undoButton.classes().contains('undo-button')

    and: 'the start for orders 1 and 2 are undone'
    !messages.text().contains('ABC_2')
    !messages.text().contains('ABC_!')
  }

  // test clicking when disabled
}
