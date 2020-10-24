/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.javascript

import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.test.BaseDashboardSpecification
import spock.lang.IgnoreIf

/**
 * Tests of the dashboard_editor.js methods - main flow scenarios.
 */
@IgnoreIf({ !sys['geb.env'] })
class DashboardEditorGUISpec extends BaseDashboardSpecification {

  /**
   * Utility method to resize a panel.
   * @param resizerID The Resizer,
   * @param deltaX The X adjustments.
   * @param deltaY The Y adjustments.
   */
  void resizePanel(String resizerID, int deltaX, int deltaY) {
    interact {
      moveToElement(editorResizer(resizerID))
      clickAndHold()
      moveByOffset(deltaX, deltaY)
      release()
    }

  }

  def "verify that editor can save changes"() {
    given: 'a dashboard with a single splitter'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the resizer is moved for the main splitter panel'
    resizePanel('0', 0, -50)

    then: 'the title indicates the dashboard has been changed'
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

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
    saveEditorChanges(dashboard)

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
    saveEditorChanges(dashboard)

    then: 'the record in the DB is correct'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboard == dashboard.dashboard
  }

  def "verify that the unsaved changes dialog works - cancel button scenario"() {
    given: 'a dashboard with a single splitter'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the resizer is moved for the main splitter panel'
    resizePanel('0', 0, -50)

    and: 'the dialog is closed'
    editorCancelButton.click()
    waitFor() { dialog1.exists }

    then: 'the dialog content is valid'
    dialog1.title == lookup('unsavedChanges.title')
    dialog1.templateContent.text() == lookup('unsavedChanges.message', dashboard.dashboard)
    editorCloseSaveConfirmButton.button.text() == lookup('save.label')
    editorCloseNoSaveButton.button.text() == lookup('noSave.label')
    editorCloseCancelButton.button.text() == lookup('cancel.label')

    when: 'the cancel button is triggered'
    editorCloseCancelButton.click()
    waitFor() { !dialog1.exists }

    then: 'the dialog is closed'
    !dialog1.exists

    when: 'the dialog is closed again'
    editorCancelButton.click()
    waitFor() { dialog1.exists }

    and: 'the do not save button is clicked'
    editorCloseNoSaveButton.click()
    waitFor() { !dialog0.exists }

    then: 'no save message on original dashboard'
    messages.text() == ''

    when: 'the editor is opened again'
    openEditor()

    and: 'the resizer is moved for the main splitter panel'
    resizePanel('0', 0, -50)

    and: 'the dialog is closed'
    editorCancelButton.click()
    waitFor() { dialog1.exists }

    and: 'the save button is triggered'
    editorCloseSaveConfirmButton.click()
    waitFor() { !dialog0.exists }

    then: 'the response is displayed'
    def msg = lookup('default.updated.message', lookup('dashboard.label'), dashboard.dashboard)
    messages.text().contains(msg)
  }

  def "verify that the unsaved changes dialog works - window close scenario"() {
    given: 'a dashboard with a single splitter'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the resizer is moved for the main splitter panel'
    resizePanel('0', 0, -50)

    and: 'the dialog is closed'
    dialog0.closeButton.click()
    waitFor() { dialog1.exists }

    then: 'the dialog is the correct dialog'
    dialog1.title == lookup('unsavedChanges.title')
    dialog1.templateContent.text() == lookup('unsavedChanges.message', dashboard.dashboard)
  }

  // TODO: DashboardJS GUI Specs (X 6 Dashboard, 4 editor)
  /*
   EditorButton - click, context menu, double-click.
   EditorUnsaved - unsaved/save, unsaved/dontSave, unsaved/Cancel
   Editor Fails Save - Displays message in dialog
   Editor.duplicate - leaves panels/splitters/buttons/config records unchanged.
   */


}
