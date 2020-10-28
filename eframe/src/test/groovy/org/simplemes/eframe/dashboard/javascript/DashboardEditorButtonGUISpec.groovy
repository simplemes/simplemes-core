/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.javascript

import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.test.BaseDashboardSpecification
import spock.lang.IgnoreIf

/**
 * Tests of the dashboard_editor.js methods - button scenarios.
 */
@IgnoreIf({ !sys['geb.env'] })
class DashboardEditorButtonGUISpec extends BaseDashboardSpecification {


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


  /*
   EditorButton - click, context menu, double-click. See legacy DashboardEditorButtonGUISpec
     remove button - unsaved indicator on each fields changed.  Change to grid.
     add button - before/after selected.  before/after none selected. before/after context menu action
     remove button - remove button. none selected
     renumber
     details dialog - each field changes unsaved changes flag.  Cancel leaves un changed.
     size is NaN


   */


}
