package org.simplemes.eframe.dashboard.javascript

import org.simplemes.eframe.test.BaseDashboardSpecification
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of the dashboard.js methods related to events.
 */
@IgnoreIf({ !sys['geb.env'] })
class DashboardJSEventGUISpec extends BaseDashboardSpecification {

  def "verify that activity handleEvent methods can receive events"() {
    given: 'a dashboard with an event handler activity and an event sender button'
    def handlerActivity = '''
    <script>
      <#assign panel = "${params._panel}"/>
      <#assign variable = "${params._variable}"/>
    
      ${variable}.display = {
        view: 'form', type: 'clean', margin: 0,
        rows: [
          {view: "template", template: "Top"},
          {
            view: "form", id: "ButtonsA", type: "clean", borderless: true, elements: [
              {view: "template", id: "ButtonsContentA", template: "-"}
            ]
          }
        ]
      };
      ${params._variable}.handleEvent = function(event) {
        ef.displayMessage({info: JSON.stringify(event)});
      }
    </script>
    '''
    def sendEventActivity = '''
      <script>
        dashboard.sendEvent({type: 'ABC',otherField: 'XYZZY'});
        dashboard.finished("${params._panel}");
      </script>
    '''
    buildDashboard(defaults: [handlerActivity, 'No Content'], buttons: [sendEventActivity])

    when: 'the dashboard is displayed the event is sent'
    displayDashboard()
    clickButton(0)
    waitForCompletion()

    then: 'the handler displayed the value as a message'
    messages.text().contains('XYZZY')
  }

  def "verify that activity provideParameters methods can provide parameters for activities"() {
    given: 'a dashboard with a provider and display activity'
    def providerActivity = '''
    <script>
      <#assign panel = "${params._panel}"/>
      <#assign variable = "${params._variable}"/>
    
      ${variable}.display = {
        view: 'form', type: 'clean', margin: 0,
        rows: [
          {view: "template", template: "Top"},
          {
            view: "form", id: "ButtonsA", type: "clean", borderless: true, elements: [
              {view: "template", id: "ButtonsContentA", template: "-"}
            ]
          }
        ]
      };
      ${params._variable}.provideParameters = function() {  
        return {
          workCenter: 'XYZZY',  
        }
      }
      </script>
    '''
    def activity = '''
      <script>
        ef.displayMessage({info: 'Work Center: ${params.workCenter}'});
        dashboard.finished("${params._panel}");
      </script>
    '''
    buildDashboard(defaults: [providerActivity, 'No Content'], buttons: [activity])

    when: 'the dashboard is displayed the event is sent'
    displayDashboard()
    clickButton(0)
    waitForCompletion()

    then: 'the provided parameter was used'
    messages.text().contains('Work Center: XYZZY')
  }

  def "verify that activity provideParameters methods provide parameters from multiple activities"() {
    given: 'a dashboard with two provider activities'
    def providerActivity1 = '''
    <script>
      <#assign panel = "${params._panel}"/>
      <#assign variable = "${params._variable}"/>
    
      ${variable}.display = {
        view: 'form', type: 'clean', margin: 0,
        rows: [
          {view: "template", template: "Top"},
          {
            view: "form", id: "ButtonsA", type: "clean", borderless: true, elements: [
              {view: "template", id: "ButtonsContentA", template: "-"}
            ]
          }
        ]
      };
      ${params._variable}.provideParameters = function() {  
        return {
          workCenter: 'XYZZY',  
        }
      }
      </script>
    '''
    def providerActivity2 = '''
    <script>
      <#assign panel = "${params._panel}"/>
      <#assign variable = "${params._variable}"/>
    
      ${variable}.display = {
        view: 'form', type: 'clean', margin: 0,
        rows: [
          {view: "template", template: "Top"},
          {
            view: "form", id: "ButtonsA", type: "clean", borderless: true, elements: [
              {view: "template", id: "ButtonsContentA", template: "-"}
            ]
          }
        ]
      };
      ${params._variable}.provideParameters = function() {  
        return {
          order: 'ABC_XYZ',  
        }
      }
      </script>
    '''
    def buttonActivity = '''
      <script>
        ef.displayMessage({info: 'Work Center: ${params.workCenter}, Order: ${params.order}'});
        dashboard.finished("${params._panel}");
      </script>
    '''
    buildDashboard(defaults: [providerActivity1, providerActivity2], buttons: [buttonActivity])

    when: 'the dashboard is displayed the event is sent'
    displayDashboard()
    clickButton(0)
    waitForCompletion()

    then: 'the provided parameters were used'
    messages.text().contains('Work Center: XYZZY')
    messages.text().contains('Order: ABC_XYZ')
  }
}
