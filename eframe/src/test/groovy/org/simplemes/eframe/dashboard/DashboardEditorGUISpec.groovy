/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard

import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.test.BaseDashboardSpecification
import spock.lang.IgnoreIf

/**
 * Tests of the dashboard_editor.js methods - main flow scenarios.
 */
@IgnoreIf({ !sys['geb.env'] })
class DashboardEditorGUISpec extends BaseDashboardSpecification {

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that non-gui activity works - non-cache case"() {
    given: 'a dashboard with a single splitter'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    configButton.click()
    waitFor { dialog0.exists }

    and: 'the resizer is moved for the main splitter panel'
    interact {
      moveToElement(editorResizer('0'))
      clickAndHold()
      moveByOffset(0, -50)
      release()
    }

    and: 'the save button is clicked'
    //sleep(2000)
    editorSaveButton.click()
    //sleep(9000)
    waitFor { !dialog0.exists }

    then: 'the response is displayed'
    def msg = lookup('default.updated.message', lookup('dashboard.label'), dashboard.dashboard)
    messages.text().contains(msg)

    and: 'the record in the DB is correct'
    waitForRecordChange(dashboard)
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboardPanels[0].defaultSize != null

  }


  // TODO: DashboardJS GUI Specs (X 6 Dashboard, 4 editor)
  /*
   EditorButton
   Editor
   EditorPanel
   EditorUnsaved
   Editor Fails Save - Displays message in dialog
   */


}
