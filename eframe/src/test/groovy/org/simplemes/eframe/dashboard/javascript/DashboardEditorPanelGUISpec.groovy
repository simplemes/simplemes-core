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


  /*
   menu Remove, Details
   context menu Remove, Details
   double-click

   */


}
