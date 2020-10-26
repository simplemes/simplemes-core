/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.javascript

import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.test.BaseDashboardSpecification
import spock.lang.IgnoreIf

/**
 * Tests of the dashboard_editor.js editor methods - panel-related
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings('GroovyAssignabilityCheck')
class DashboardEditorPanelGUISpec extends BaseDashboardSpecification {

  def "verify that a splitter can be added to panels"() {
    given: 'a dashboard with the right number of panels'
    def defaults = []
    for (i in (1..nPanels)) {
      defaults << "Content $i"
    }
    def dashboard = buildDashboard(defaults: defaults)

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the top panel is selected'
    editorPanel(targetPanel).click()

    and: 'a horizontal splitter is added'
    def direction = vertical ? 'Vertical' : 'Horizontal'
    clickMenu('splitters', "add${direction}Splitter")
    waitFor() {
      editorPanel(expectedPanel).displayed
    }

    then: 'the title indicates the dashboard has been changed'
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the panel is displayed in the original dashboard page after refresh'
    waitFor() {
      panel(expectedPanel).displayed
    }

    then: 'the record in the DB is correct'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboardPanels.size() == nPanels + 1
    dashboard2.splitterPanels.size() == splitterIndex + 1

    dashboard2.splitterPanels[splitterIndex].vertical == vertical

    where:
    nPanels | vertical | targetPanel | expectedPanel | splitterIndex
    2       | false    | 'A'         | 'C'           | 1
    2       | true     | 'A'         | 'C'           | 1
    1       | true     | 'A'         | 'B'           | 0
  }

  def "verify that a splitter can be added to a panel via the context menu"() {
    given: 'a dashboard with the right number of panels'
    def dashboard = buildDashboard(defaults: ['Content 0'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the context-right-click is triggered on the panel'
    def direction = vertical ? 'Vertical' : 'Horizontal'
    interact {
      contextClick editorPanel('A')
    }
    waitFor { menu("add${direction}Splitter").displayed }
    menu("add${direction}Splitter").click()

    then: 'the title indicates the dashboard has been changed'
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the panel is displayed in the original dashboard page after refresh'
    waitFor() {
      panel('B').displayed
    }

    then: 'the record in the DB is correct'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboardPanels.size() == 2
    dashboard2.splitterPanels.size() == 1

    dashboard2.splitterPanels[0].vertical == vertical

    where:
    vertical | _
    false    | _
    true     | _
  }

  def "verify that a panel can be removed via menu - 1 and 2 splitters"() {
    given: 'a dashboard with the right number of panels'
    def defaults = []
    for (i in (1..nPanels)) {
      defaults << "Content $i"
    }
    def dashboard = buildDashboard(defaults: defaults)
    def originalPanelCount = dashboard.dashboardPanels.size()
    def originalSplitterCount = dashboard.splitterPanels.size()

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the panel is selected'
    editorPanel(targetPanel).click()

    and: 'the panel is removed'
    clickMenu('panels', "removePanel")
    waitFor() {
      dialog0.title.startsWith('*')
    }

    and: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the panel is removed from the original dashboard page after refresh'
    waitFor() {
      messages.text()
    }

    then: 'the record in the DB is correct'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboardPanels.size() == originalPanelCount - 1
    dashboard2.splitterPanels.size() == originalSplitterCount - 1

    dashboard2.dashboardPanels.find { it.panel == 'B' }
    !dashboard2.dashboardPanels.find { it.panel == 'A' }

    where:
    nPanels | targetPanel | remainingPanel
    2       | 'A'         | 'B'
    3       | 'A'         | 'B'
  }

  def "verify that a panel can be removed via context menu - right mouse click"() {
    given: 'a dashboard with the right number of panels'
    def dashboard = buildDashboard(defaults: ['Content 1', 'Content 2'])
    def originalPanelCount = dashboard.dashboardPanels.size()
    def originalSplitterCount = dashboard.splitterPanels.size()

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the panel is selected'
    editorPanel('A').click()

    and: 'the context menu is displayed'
    interact {
      contextClick editorPanel('A')
    }
    waitFor { menu("removePanelContext").displayed }

    then: 'the context menu label is correct'
    menu("removePanelContext").text() == lookup('dashboardEditorMenu.removePanel.label')

    when: 'the panel is removed'
    menu("removePanelContext").click()

    and: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the panel is removed from the original dashboard page after refresh'
    waitFor() {
      messages.text()
    }

    then: 'the record in the DB is correct'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.dashboardPanels.size() == originalPanelCount - 1
    dashboard2.splitterPanels.size() == originalSplitterCount - 1

    dashboard2.dashboardPanels.find { it.panel == 'B' }
    !dashboard2.dashboardPanels.find { it.panel == 'A' }
  }

  def "verify that remove detects no selected panel case"() {
    given: 'a dashboard with the right number of panels'
    buildDashboard(defaults: ['Content 1', 'Content 2'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the menu is triggered'
    clickMenu('panels', "removePanel")
    waitFor { dialog0.messages.text() }

    then: 'the message is correct'
    dialog0.messages.text().contains(lookup('error.114.message'))
  }

  def "verify that context menu remove detects attempt to remove last panel"() {
    given: 'a dashboard with the right number of panels'
    buildDashboard(defaults: ['Content 1'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the context-right-click is triggered on the panel'
    interact {
      contextClick editorPanel('A')
    }
    waitFor { menu("removePanelContext").displayed }
    menu("removePanelContext").click()
    waitFor { dialog0.messages.text() }

    then: 'the message is correct'
    dialog0.messages.text().contains(lookup('error.116.message'))
  }

  def "verify that panel context menu is correct"() {
    given: 'a dashboard with the right number of panels'
    buildDashboard(defaults: ['Content 1'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the context-right-click is triggered on the panel'
    interact {
      contextClick editorPanel('A')
    }
    waitFor { menu("removePanelContext").displayed }

    then: 'the menus are correct'
    menu("addHorizontalSplitter").text() == lookup('dashboardEditorMenu.addHorizontalSplitter.label')
    menu("addVerticalSplitter").text() == lookup('dashboardEditorMenu.addVerticalSplitter.label')
    menu("removePanelContext").text() == lookup('dashboardEditorMenu.removePanel.label')
    menu("panelDetailsContext").text() == lookup('dashboardEditorMenu.details.label')
  }

  static private final String defaultPanelURL = '/test/dashboard/page?view=sample/dashboard/workList'

  def "verify that the details dialog works - triggered by context menu"() {
    given: 'a dashboard with the right number of panels'
    def dashboard = buildDashboard(defaults: [defaultPanelURL, 'Content 2'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the context menu is displayed'
    interact {
      contextClick editorPanel('A')
    }
    waitFor { menu("panelDetailsContext").displayed }

    and: 'the details menu item is clicked'
    menu("panelDetailsContext").click()
    waitFor { dialog1.exists }

    then: 'the dialog labels are correct'
    getFieldLabel('panel') == lookup('panel.label')
    getFieldLabel('defaultURL') == lookup('defaultURL.label')
    dialog1.okButton.text() == lookup('ok.label')
    dialog1.cancelButton.text() == lookup('cancel.label')
    dialog1.title == lookup('dashboard.editor.panelDetailsDialog.title')

    when: 'the field value is changed'
    setFieldValue(fieldName, expectedValue, newValue)

    and: 'the dialog is closed'
    dialog1.okButton.click()
    waitFor { !dialog1.exists }

    then: 'the title indicates the dashboard has been changed'
    dialog0.title.startsWith('*')

    and: 'the contents of the panel in the editor dialog is updated'
    def expectedContents = (fieldName == 'panel') ? lookup('panel.label') + ' D:' : '/test/dashboard/page?view=sample/dashboard/wcSelection'
    def panelName = (fieldName == 'panel') ? 'D' : 'A'
    $('div.webix_view', view_id: "EditorContent$panelName").text().contains(expectedContents)

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the record in the DB is correct'
    waitForRecordChange(dashboard)
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    def panel = dashboard2.dashboardPanels[0]
    panel[fieldName] == newValue

    where:
    fieldName    | expectedValue   | newValue
    'panel'      | 'A'             | 'D'
    'defaultURL' | defaultPanelURL | '/test/dashboard/page?view=sample/dashboard/wcSelection'
  }

  def "verify that the details dialog works - triggered by 2 methods and cancelled"() {
    given: 'a dashboard with the right number of panels'
    buildDashboard(defaults: ['Content 1', 'Content 2'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the dialog is triggered'
    if (mechanism == 'toolbar') {
      editorPanel('A').click()
      clickMenu('panels', 'panelDetails')
    } else if (mechanism == 'double') {
      interact {
        doubleClick editorPanel('A')
      }
    }
    waitFor { dialog1.exists }

    and: 'the field value is changed'
    setFieldValue('panel', 'A', 'D')

    and: 'the dialog is cancelled'
    dialog1.cancelButton.click()
    waitFor { !dialog1.exists }

    then: 'the title indicates the dashboard has not been changed'
    !dialog0.title.startsWith('*')

    where:
    mechanism | _
    'toolbar' | _
    'double'  | _
  }

}
