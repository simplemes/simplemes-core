/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.javascript

import org.simplemes.eframe.test.BaseDashboardSpecification
import spock.lang.IgnoreIf

/**
 * Tests of the dashboard.js methods related to activity execution.
 */
@IgnoreIf({ !sys['geb.env'] })
class DashboardJSActivityGUISpec extends BaseDashboardSpecification {

  def "verify that non-gui activity works - non-cache case"() {
    given: 'a dashboard with a simple non-gui activity'
    def activity = '''<script>
      ${params._variable}.execute =  function() {  
        ef.displayMessage('Started Order');
      }
      ${params._variable}.cache = false;
    </script>  
    '''
    buildDashboard(defaults: [BUTTON_PANEL], buttons: [activity])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the non-gui activity is clicked'
    clickButton(0)
    waitForCompletion()

    then: 'the response is displayed'
    messages.text().contains('Started Order')
  }

  def "verify that non-gui activity works - cache case"() {
    given: 'a dashboard with a simple non-gui activity'
    def activity = '''<script>
      ${params._variable}.execute =  function() {
        ef.displayMessage('Started Order '+${params._variable}.timeStamp);
      }
      ${params._variable}.cache =  true;
      ${params._variable}.timeStamp = "${timeStamp}";
    </script>  
    '''
    buildDashboard(defaults: [BUTTON_PANEL], buttons: [activity])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the non-gui activity is clicked'
    clickButton(0)
    waitForCompletion()
    def originalText = messages.text()

    and: 'the non-gui activity is clicked again'
    clickButton(0)
    waitForCompletion()

    then: 'the response is displayed'
    messages.text() == originalText
  }

  def "verify that non-gui activity works - cache false case"() {
    given: 'a dashboard with a simple non-gui activity'
    def activity = '''<script>
      ${params._variable}.execute =  function() {
        ef.displayMessage('Started Order '+${params._variable}.timeStamp);
      }
      ${params._variable}.cache =  false;
      ${params._variable}.timeStamp = "${timeStamp}";
    </script>  
    '''
    buildDashboard(defaults: [BUTTON_PANEL], buttons: [activity])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the non-gui activity is clicked'
    clickButton(0)
    waitForCompletion()
    def originalText = messages.text()

    and: 'the non-gui activity is clicked again'
    clickButton(0)
    waitForCompletion()

    then: 'the response is displayed'
    messages.text() != originalText
  }

  def "verify that non-gui activity works keeps all GUI activities active in the dashboard"() {
    given: 'a dashboard with a simple non-gui activity'
    def guiActivity = '''
    <script>
      <@efForm id="logFailure" dashboard="buttonHolder">  
        <@efField field="serial" value="RMA1001" width=20/>  
      </@efForm>
      ${params._variable}.handleEvent = function(event) {
        ef.displayMessage({info: JSON.stringify(event)});
      }
    </script>
    '''
    def nonGUIActivity = '''<script>
      ${params._variable}.execute =  function() {
        dashboard.sendEvent({type: 'ABC',otherField: 'XYZZY'});
      }
    </script>  
    '''
    buildDashboard(defaults: [guiActivity], buttons: [nonGUIActivity])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the non-gui activity is clicked'
    clickButton(0)
    waitForCompletion()

    then: 'the GUI activity was active and received the event'
    messages.text().contains('ABC')
    messages.text().contains('XYZZY')
  }

}
