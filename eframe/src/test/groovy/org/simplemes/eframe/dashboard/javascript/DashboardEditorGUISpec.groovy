/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.javascript

import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.page.ButtonModule
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

  def "verify that editor can save changes - dialog button"() {
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

  def "verify that editor can save changes - toolbar save button"() {
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
    editorSaveToolbarButton.click()
    waitFor() { dialog0.messages.text() }

    then: 'the response is displayed in the dialog'
    def msg = lookup('default.updated.message', lookup('dashboard.label'), dashboard.dashboard)
    dialog0.messages.text().contains(msg)

    and: 'the record in the DB is correct'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboardPanels[0].defaultSize != null
  }

  def "verify that editor menus are correct"() {
    given: 'a dashboard'
    buildDashboard(defaults: ['Content A'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    then: 'the top-level menus on the toolbar are correct'
    menu('details').text() == lookup('details.label')
    menu('save').text() == lookup('save.label')

    and: 'the splitters menu is displayed'
    checkMenuLabels('splitters', 'dashboardEditorMenu.splitter.label',
                    [['addHorizontalSplitter', 'addHorizontalSplitter.label'],
                     ['addVerticalSplitter', 'addVerticalSplitter.label']])
    checkMenuLabels('panels', 'dashboardEditorMenu.panel.label',
                    [['removePanel', 'removePanel.label'],
                     ['panelDetails', 'details.label']])
    checkMenuLabels('buttons', 'dashboardEditorMenu.button.label',
                    [['addButtonBefore', 'addButtonBefore.label'],
                     ['addButtonAfter', 'addButtonAfter.label'],
                     ['renumberButtons', 'renumberButtons.label'],
                     ['removeButton', 'removeButton.label'],
                     ['buttonDetails', 'details.label'],])
    checkMenuLabels('more.menu', 'more.menu.label',
                    [['create.menu', 'create.menu.label'],
                     ['duplicate.menu', 'duplicate.menu.label'],
                     ['delete.menu', 'delete.menu.label']])

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

    then: 'the dialog labels are correct'
    getFieldLabel('dashboard') == lookupRequired('dashboard.label')
    getFieldLabel('category') == lookup('category.label')
    getFieldLabel('title') == lookup('title.label')
    dialog1.okButton.text() == lookup('ok.label')
    dialog1.cancelButton.text() == lookup('cancel.label')
    dialog1.title == lookup('dashboard.editor.detailsDialog.title')

    when: 'the field value is changed'
    setFieldValue(fieldName, expectedValue, newValue)

    and: 'the dialog is closed'
    dialog1.okButton.click()
    waitFor { !dialog1.exists }

    then: 'the title indicates the dashboard has been changed'
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the record in the DB is correct'
    waitForRecordChange(dashboard)
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

  def "verify that save failures are detected and displayed correctly"() {
    given: 'a dashboard'
    def dashboard = buildDashboard(defaults: ['Content A'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the details dialog is displayed'
    clickMenu('details')
    waitFor { dialog1.exists }

    and: 'the panel is cleared'
    setFieldValue('dashboard', dashboard.dashboard, '')

    and: 'the dialog is closed'
    dialog1.okButton.click()
    waitFor { !dialog1.exists }

    and: 'the save is attempted'
    editorSaveButton.click()
    waitFor() { dialog0.messages.text() }

    then: 'an error is displayed in the dialog'
    dialog0.messages.text() != ''

    and: 'the record in the DB is unchanged'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.version == dashboard.version
  }

  def "verify that delete works"() {
    given: 'a dashboard with a single splitter'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the details dialog is displayed'
    clickMenu('more.menu', 'delete_menu')

    then: 'the dialog labels are correct'
    dialog1.title == lookup('delete.confirm.title')
    dialog1.templateContent.text() == lookup('delete.confirm.message', lookup('dashboard.label'), dashboard.dashboard)
    editorCloseCancelButton.button.text() == lookup('cancel.label')
    editorDeleteConfirmButton.button.text() == lookup('delete.label')

    when: 'the delete is cancelled'
    editorDeleteCancelButton.button.click()
    waitFor { !dialog1.exists }

    then: 'the editor dialog is still displayed'
    dialog0.exists

    when: 'the delete is confirmed'
    and: 'the details dialog is displayed'
    clickMenu('more.menu', 'delete_menu')
    editorDeleteConfirmButton.button.click()
    waitFor { !dialog0.exists }

    then: 'the page is refreshed with a message'
    waitFor() { messages.text() }

    and: 'the message is correct'
    def msg = lookup('default.deleted.message', lookup('dashboard.label'), dashboard.dashboard)
    messages.text().contains(msg)

    and: 'the record has been deleted'
    !DashboardConfig.findByUuid(dashboard.uuid)
  }

  def "verify that create works"() {
    given: 'a dashboard with a single splitter'
    def createdDashboardName = 'NEW'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'], buttons: ['PASS', 'FAIL'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the actions is triggered'
    clickMenu('more.menu', 'create_menu')

    then: 'the dialog title has the correct title'
    dialog0.title.contains(createdDashboardName)

    when: 'the new record is saved'
    editorSaveButton.click()
    waitFor { !dialog0.exists }

    then: 'the page is refreshed with a message'
    waitFor() { messages.text() }

    and: 'the message is correct'
    def msg = lookup('default.created.message', lookup('dashboard.label'), createdDashboardName)
    messages.text().contains(msg)

    and: 'the original record has not been affected'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboardPanels.size() == 2
    dashboard2.splitterPanels.size() == 1
    dashboard2.buttons.size() == 2

    and: 'the new record has been created'
    def dashboardNew = DashboardConfig.findByDashboard(createdDashboardName)
    dashboardNew.dashboardPanels.size() == 1
    dashboardNew.splitterPanels.size() == 0
    dashboardNew.buttons.size() == 0
  }

  def "verify that duplicate works"() {
    given: 'a dashboard with a single splitter'
    def createdDashboardName = 'COPY _TEST'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'], buttons: ['PASS', 'FAIL'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the actions is triggered'
    clickMenu('more.menu', 'duplicate_menu')

    then: 'the dialog title has the correct title'
    dialog0.title.contains(createdDashboardName)

    when: 'the new record is saved'
    editorSaveButton.click()
    waitFor { !dialog0.exists }

    then: 'the page is refreshed with a message'
    waitFor() { messages.text() }

    and: 'the message is correct'
    def msg = lookup('default.created.message', lookup('dashboard.label'), createdDashboardName)
    messages.text().contains(msg)

    and: 'the original record has not been affected'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboardPanels.size() == 2
    dashboard2.splitterPanels.size() == 1
    dashboard2.buttons.size() == 2

    and: 'the new record has been created'
    def dashboardNew = DashboardConfig.findByDashboard(createdDashboardName)
    dashboardNew.dashboardPanels.size() == 2
    dashboardNew.splitterPanels.size() == 1
    dashboardNew.buttons.size() == 2

    and: 'the panel was copied correctly'
    dashboardNew.dashboardPanels[1].panel == dashboard.dashboardPanels[1].panel
    dashboardNew.dashboardPanels[1].defaultURL == dashboardNew.dashboardPanels[1].defaultURL

    and: 'the button was copied correctly'
    dashboardNew.buttons[1].buttonID == dashboardNew.buttons[1].buttonID
    dashboardNew.buttons[1].title == dashboardNew.buttons[1].title
    dashboardNew.buttons[1].label == dashboardNew.buttons[1].label
    dashboardNew.buttons[1].url == dashboardNew.buttons[1].url
    dashboardNew.buttons[1].panel == dashboardNew.buttons[1].panel
    dashboardNew.buttons[1].size == dashboardNew.buttons[1].size
    dashboardNew.buttons[1].css == dashboardNew.buttons[1].css

  }

  def "verify that editor displays buttons in sorted order"() {
    given: 'a dashboard with buttons saved in non-sorted order'
    def buttons = [[label: 'pass.label', url: 'PASS Content', panel: 'A', title: 'pass.title', buttonID: 'PASS'],
                   [label: 'fail.label', url: 'FAIL Content', panel: 'A', buttonID: 'FAIL']]
    def dashboard = buildDashboard(defaults: [BUTTON_PANEL], buttons: buttons)
    DashboardConfig.withTransaction {
      dashboard.buttons[0].sequence = 100
      dashboard.buttons[0].save()
    }

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    then: 'the buttons are in the correct order - FAIL first due to sequence change above'
    $("body").module(new ButtonModule(id: 'FAILEditor')).button.x < $("body").module(new ButtonModule(id: 'PASSEditor')).button.x
  }


}
