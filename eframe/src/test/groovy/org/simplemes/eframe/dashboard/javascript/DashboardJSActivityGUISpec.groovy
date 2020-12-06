/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.javascript

import groovy.json.JsonSlurper
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
    clickDashboardButton(0)
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
    clickDashboardButton(0)
    waitForCompletion()
    def originalText = messages.text()

    and: 'the non-gui activity is clicked again'
    clickDashboardButton(0)
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
    clickDashboardButton(0)
    waitForCompletion()
    def originalText = messages.text()

    and: 'the non-gui activity is clicked again'
    clickDashboardButton(0)
    waitForCompletion()

    then: 'the response is displayed'
    messages.text() != originalText
  }

  def "verify that non-gui activity keeps all GUI activities active in the dashboard"() {
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
    clickDashboardButton(0)
    waitForCompletion()

    then: 'the GUI activity was active and received the event'
    messages.text().contains('ABC')
    messages.text().contains('XYZZY')
  }

  def "verify that non-gui activity can get parameters from displaced activities"() {
    given: 'a dashboard with a simple non-gui activity'
    def guiActivity = '''
    <script>
      <@efForm id="logFailure" dashboard="buttonHolder">  
        <@efField field="serial" value="RMA1001" width=20/>  
      </@efForm>
      ${params._variable}.provideParameters = function() {
        return { workCenter: 'XYZZY-123' };
      }
    </script>
    '''
    def nonGUIActivity = '''<script>
      ${params._variable}.execute =  function() {
        var params = dashboard._getExtraParamsFromActivities();
        ef.displayMessage(JSON.stringify(params));
      }
    </script>  
    '''
    buildDashboard(defaults: [guiActivity], buttons: [nonGUIActivity])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the non-gui activity is clicked'
    clickDashboardButton(0)
    waitForCompletion()

    then: 'the GUI activity was active and received the event'
    messages.text().contains('XYZZY-123')
  }

  def "verify that non-gui activity with caching and changing parameters is still cached"() {
    given: 'a dashboard with a non-gui activity and a gui activity that will change on call'
    def guiActivity = '''
    <script>
      <@efForm id="logFailure" dashboard="buttonHolder">  
        <@efField field="serial" value="RMA1001" width=20/>  
      </@efForm>
      window['_test_counter'] = 0;
      ${params._variable}.provideParameters = function() {
        window['_test_counter'] = window['_test_counter'] +1;
        return { workCenter: 'XYZZY-'+window['_test_counter'] };
      }
    </script>
    '''
    def nonGUIActivity = '''<script>
      ${params._variable}.execute =  function() {
        var p = dashboard.getCurrentProvidedParameters();
        p[p.length] = {timeStamp: "${timeStamp}"}; 
        ef.displayMessage(JSON.stringify(p));
      }
      ${params._variable}.cache = true;
      ${params._variable}.timeStamp = "${timeStamp}";
    </script>  
    '''
    buildDashboard(defaults: [guiActivity], buttons: [nonGUIActivity])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the non-gui activity is clicked'
    clickDashboardButton(0)
    waitForCompletion()
    def originalText = messages.text()
    def originalJson = new JsonSlurper().parseText(originalText)

    and: 'the non-gui activity is clicked again'
    clickDashboardButton(0)
    waitForCompletion()

    then: 'the new value for the parameter is displayed'
    def json = new JsonSlurper().parseText(messages.text())
    messages.text() != originalText

    and: 'no new request was made to the server - the timestamp is the same'
    //noinspection GroovyAssignabilityCheck
    originalJson[1].timeStamp == json[1].timeStamp
  }

  def "verify that activity state store - restore works"() {
    given: 'a dashboard with a simple gui activity that stores and restores state'
    def guiActivity1 = '''
    <script>
      <@efForm id="logFailure" dashboard="buttonHolder">  
        <@efField field="serial" value="RMA1001" width=20/>  
      </@efForm>
      ${params._variable}.getState = function() {
        return {serial: $$('serial').getValue()};
      }
      ${params._variable}.restoreState = function(state) {
        if (state && state.serial) {
          $$('serial').setValue(state.serial);
        }
      }
    </script>
    '''
    def guiActivity2 = '''
    <script>
      <@efForm id="logFailure" dashboard="buttonHolder">  
        <@efButtonGroup>
          <@efButton id='doneButton' label="Done" click="dashboard.finished('${params._panel}')"/>  
        </@efButtonGroup>  
      </@efForm>
    </script>
    '''
    buildDashboard(defaults: [guiActivity1], buttons: [guiActivity2])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'a user input is made'
    $('#serial').value('XYZZY')

    and: 'the second activity is displayed'
    clickDashboardButton(0)
    waitForCompletion()

    and: 'the second activity is dismissed'
    clickButton('doneButton')
    waitForCompletion()

    //sleep(20000)
    then: 'a user input is restored'
    $('#serial').value() == 'XYZZY'
  }

}
