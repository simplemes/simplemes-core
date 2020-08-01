/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports


import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.page.ButtonModule
import spock.lang.IgnoreIf

/**
 * Tests for the reportActivity dashboard activity.
 */
@IgnoreIf({ !sys['geb.env'] })
class ReportActivityGUISpec extends BaseDashboardSpecification {

  def "verify that the report activity displays the reports and works"() {
    given: 'a dashboard with a selection panel with input parameter and the report activity on a button'
    def guiActivity = '''
    <script>
      ${params._variable}.provideParameters = function() {
        return {
          className: $$('className').getValue(),
        }
      }
      <@efForm id="logFailure" dashboard="buttonHolder">  
        <@efField id="className" field="className"/>  
      </@efForm>
    </script>
    '''
    def buttons = [[label: 'reports.label', url: '/report/reportActivity', panel: 'A', buttonID: 'B10']]
    buildDashboard(defaults: [guiActivity], buttons: buttons)

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the input field is filled in'
    $('#className').value('M1001-XYZZY')

    and: 'the reports button is clicked'
    clickButton('B10')
    waitFor {
      $("body").module(new ButtonModule(id: 'ArchiveLogButton')).displayed
    }

    and: 'the Order report is displayed'
    $("body").module(new ButtonModule(id: 'ArchiveLogButton')).click()
    waitFor {
      $('div#ReportHeader').displayed
    }

    then: 'the parameter was passed to the report'
    $('div#ReportHeader').text().contains('M1001-XYZZY')
  }


}
