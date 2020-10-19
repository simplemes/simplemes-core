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

  /**
   * Opens the editor dialog.
   */
  void openEditor() {
    configButton.click()
    waitFor { dialog0.exists }
  }

  /**
   * Opens the editor dialog and waits for the dialog to close and the record to be updated in the DB.
   *
   * @param dashboard The dashboard record to wait for the record update to be committed.
   */
  void saveChanges(DashboardConfig dashboard) {
    editorSaveButton.click()
    waitFor { !dialog0.exists }
    waitForRecordChange(dashboard)
  }

  def "verify that editor can save changes"() {
    given: 'a dashboard with a single splitter'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the resizer is moved for the main splitter panel'
    interact {
      moveToElement(editorResizer('0'))
      clickAndHold()
      moveByOffset(0, -50)
      release()
    }

    then: 'the title indicates the dashboard has been changed'
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveChanges(dashboard)

    then: 'the response is displayed'
    def msg = lookup('default.updated.message', lookup('dashboard.label'), dashboard.dashboard)
    messages.text().contains(msg)

    and: 'the record in the DB is correct'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboardPanels[0].defaultSize != null
  }

  def "verify that details dialog works - basic field changes"() {
    given: 'a dashboard with a single splitter'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the details dialog is displayed'
    clickMenu('details')
    waitFor { dialog1.exists }

    and: 'the field value is changed'
    setFieldValue(fieldName, expectedValue, newValue)

    and: 'the dialog is closed'
    dialog1.okButton.click()
    waitFor { !dialog1.exists }

    then: 'the title indicates the dashboard has been changed'
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveChanges(dashboard)

    then: 'the record in the DB is correct'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2[fieldName] == newValue

    where:
    fieldName       | expectedValue                    | newValue
    'dashboard'     | '_TEST'                          | 'TEST_ABC'
    'category'      | DashboardConfig.DEFAULT_CATEGORY | 'CAT_ABC'
    'title'         | 'title-_TEST'                    | 'title_ABC'
    'defaultConfig' | true                             | false
  }

  def "verify that details dialog works - cancel discards the changes"() {
    given: 'a dashboard with a single splitter'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the details dialog is displayed'
    clickMenu('details')
    waitFor { dialog1.exists }

    and: 'the field value is changed'
    setFieldValue('dashboard', '_TEST', 'TEST_ABC')

    and: 'the dialog is closed'
    dialog1.cancelButton.click()
    waitFor { !dialog1.exists }

    then: 'the title indicates the dashboard has been changed'
    !dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveChanges(dashboard)

    then: 'the record in the DB is correct'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboard == dashboard.dashboard
  }


  // TODO: DashboardJS GUI Specs (X 6 Dashboard, 4 editor)
  /*
  // single panel
  // nested panels
   EditorButton
   Editor
   EditorPanel
   EditorUnsaved
   Editor Fails Save - Displays message in dialog
   */


}
