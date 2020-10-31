/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.javascript

import org.openqa.selenium.Keys
import org.simplemes.eframe.dashboard.domain.DashboardButton
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.test.BaseDashboardSpecification
import spock.lang.IgnoreIf

/**
 * Tests of the dashboard_editor.js methods - button scenarios.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings('GroovyAssignabilityCheck')
class DashboardEditorButtonGUISpec extends BaseDashboardSpecification {


  def "verify that button details dialog works - basic field change - triggered by menu"() {
    given: 'a dashboard with a button'
    def buttons = [['PASS']]
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'], buttons: buttons)

    and: 'an initial button value'
    DashboardButton.withTransaction {
      dashboard.buttons[0][fieldName] = expectedValue
      dashboard.buttons[0].save()
    }

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the buttons details dialog is displayed'
    editorButton('B0').click()
    clickMenu('buttons', 'buttonDetails')
    waitFor { dialog1.exists }

    then: 'the dialog labels are correct'
    getFieldLabel('buttonID') == lookupRequired('buttonID.label')
    getFieldLabel('label') == lookupRequired('label.label')
    getFieldLabel('title') == lookup('title.label')
    getFieldLabel('css') == lookup('css.label')
    getFieldLabel('size') == lookup('size.label')
    dialog1.okButton.text() == lookup('ok.label')
    dialog1.cancelButton.text() == lookup('cancel.label')
    dialog1.title == lookup('dashboard.editor.buttonDetailsDialog.title')

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
    dashboard2.buttons[0][fieldName] == newValue

    where:
    fieldName  | expectedValue | newValue
    'buttonID' | 'B0'          | 'PASS_ABC'
    'label'    | 'B0'          | 'LABEL_ABC'
    'title'    | 'orig-title'  | 'title_PASS'
    'css'      | 'orig-css'    | 'new_CSS'
    'size'     | 2             | 237
  }

  def "verify that cancel leaves dashboard unchanged"() {
    given: 'a dashboard with a button'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'], buttons: ['/aUrl'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the buttons details dialog is displayed'
    editorButton('B0').click()
    clickMenu('buttons', 'buttonDetails')
    waitFor { dialog1.exists }

    and: 'the field value is changed'
    setFieldValue('title', '', 'changed value')

    and: 'the dialog is closed'
    dialog1.cancelButton.click()
    waitFor { !dialog1.exists }

    then: 'the title indicates the dashboard has not been changed'
    !dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the record in the DB is not changed'
    waitForRecordChange(dashboard)
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    !dashboard2.buttons[0].title
  }

  def "verify that button details dialog works - triggered by double-click"() {
    given: 'a dashboard with a button'
    def buttons = [['PASS']]
    buildDashboard(defaults: ['Content A', 'Content C'], buttons: buttons)

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the button is double-clicked'
    interact {
      doubleClick editorButton('B0')
    }
    waitFor { dialog1.exists }

    then: 'the dialog is displayed'
    dialog1.title == lookup('dashboard.editor.buttonDetailsDialog.title')
  }

  def "verify that button details dialog works - triggered by context menu"() {
    given: 'a dashboard with a button'
    def buttons = [['PASS']]
    buildDashboard(defaults: ['Content A', 'Content C'], buttons: buttons)

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the right-click details option is triggered'
    interact {
      contextClick editorButton('B0')
    }
    waitFor { menu("buttonDetailsContext").displayed }
    menu("buttonDetailsContext").click()

    waitFor { dialog1.exists }

    then: 'the dialog is displayed'
    dialog1.title == lookup('dashboard.editor.buttonDetailsDialog.title')
  }

  def "verify that a button can be removed and saved - context menu and toolbar menu"() {
    given: 'a dashboard with buttons'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'], buttons: initialButtons)

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the button(s) are removed'
    for (button in buttonsToRemove) {
      if (mechanism == 'toolbar') {
        editorButton(button).click()
        clickMenu('buttons', 'removeButton')
      } else {
        interact {
          contextClick editorButton(button)
        }
        waitFor { menu("removeButtonContext").displayed }
        menu("removeButtonContext").click()
      }
    }

    then: 'the title indicates the dashboard has been changed'
    waitFor { dialog0.title.startsWith('*') }
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the record in the DB is correct'
    waitForRecordChange(dashboard)
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.buttons.size() == initialButtons.size() - buttonsToRemove.size()

    and: 'the removed buttons are not saved in the DB'
    for (button in buttonsToRemove) {
      assert !dashboard2.buttons.find { it.buttonID == button }
    }

    where:
    initialButtons     | buttonsToRemove    | mechanism
    ['A0', 'A1', 'A2'] | ['B1']             | 'context'
    ['A0', 'A1', 'A2'] | ['B0']             | 'toolbar'
    ['A0', 'A1', 'A2'] | ['B2']             | 'context'
    ['A0', 'A1', 'A2'] | ['B0', 'B1', 'B2'] | 'toolbar'
  }

  def "verify that a button can be added before or after other button - context menu and toolbar menu"() {
    given: 'a dashboard with buttons'
    def dashboard = buildDashboard(defaults: ['Content A', 'Content C'], buttons: initialButtons)

    and: 'the sequences are incremented by 10'
    DashboardButton.withTransaction {
      def sequence = 10
      for (button in dashboard.buttons) {
        button.sequence = sequence
        button.save()
        sequence += 10
      }
    }

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the button is added as desired'
    def menuText = before ? 'Before' : 'After'
    if (mechanism == 'toolbar') {
      if (buttonToClick) {
        editorButton(buttonToClick).click()
      }
      clickMenu('buttons', "addButton$menuText")
    } else {
      interact {
        contextClick editorButton(buttonToClick)
      }
      waitFor { menu("addButton${menuText}Context").displayed }
      menu("addButton${menuText}Context").click()
    }

    then: 'the title indicates the dashboard has been changed'
    waitFor { dialog0.title.startsWith('*') }
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the record in the DB is correct'
    waitForRecordChange(dashboard)
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.buttons.size() == initialButtons.size() + 1

    and: 'the new button is saved in the DB'
    def button = dashboard2.buttons.find { it.sequence == expectedSequence }
    button.label == "label$expectedSequence"
    button.buttonID == "button$expectedSequence"
    button.panel == "A"
    button.url == "change"

    where:
    initialButtons     | buttonToClick | mechanism | before | expectedSequence
    ['A0', 'A1', 'A2'] | 'B1'          | 'context' | true   | 19
    ['A0', 'A1', 'A2'] | 'B0'          | 'toolbar' | true   | 9
    ['A0', 'A1', 'A2'] | 'B2'          | 'context' | false  | 31
    ['A0', 'A1', 'A2'] | 'B0'          | 'toolbar' | false  | 11
    ['A0', 'A1', 'A2'] | null          | 'toolbar' | true   | 9
    ['A0', 'A1', 'A2'] | null          | 'toolbar' | false  | 31
  }

  def "verify that editor detects no button selected for menu actions"() {
    given: 'a dashboard with buttons'
    buildDashboard(defaults: ['Content A', 'Content C'], buttons: ['B0'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the menu is triggered'
    clickMenu('buttons', menu)
    waitFor { dialog0.messages.text() }

    then: 'the message is correct'
    dialog0.messages.text().contains(lookup('error.118.message'))

    where:
    menu            | _
    'removeButton'  | _
    'buttonDetails' | _
  }

  def "verify that the activity list can be changed - edit and add row"() {
    given: 'a dashboard with a button'
    def dashboard = buildDashboard(defaults: ['Content A'], buttons: ['/aUrl'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the buttons details dialog is displayed'
    editorButton('B0').click()
    clickMenu('buttons', 'buttonDetails')
    waitFor { dialog1.exists }

    then: 'the activity grid is correct'
    buttonActivities.cell(0, 0).text() == '10'
    buttonActivities.cell(0, 1).text() == 'A'
    buttonActivities.cell(0, 2).text() == '/aUrl'

    when: 'the activity is changed'
    buttonActivities.cell(0, 2).click()
    sendKey('NEW1')
    sendKey(Keys.TAB)

    and: 'an activity is added'
    buttonActivities.addRowButton.click()
    sendKey(Keys.TAB)  // Use default sequence

    sendKey('A')  // Panel
    sendKey(Keys.TAB)

    sendKey('/newURL2')  // Url
    sendKey(Keys.TAB)

    and: 'the dialog is closed'
    dialog1.okButton.click()
    waitFor { !dialog1.exists }

    then: 'the title indicates the dashboard has changed'
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the record in the DB is correct'
    waitForRecordChange(dashboard)
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.buttons.size() == 2

    dashboard2.buttons[0].sequence == 10
    dashboard2.buttons[0].panel == 'A'
    dashboard2.buttons[0].url == 'NEW1'

    and: 'the new activity has the calculated sequence'
    dashboard2.buttons[1].sequence == 20
    dashboard2.buttons[1].panel == 'A'
    dashboard2.buttons[1].url == '/newURL2'

    and: 'the two activities are saved as separate button records with the same label/ID'
    dashboard2.buttons[0].title == dashboard2.buttons[1].title
    dashboard2.buttons[0].buttonID == dashboard2.buttons[1].buttonID
  }

  def "verify that the activity list can be changed - remove row and rows are sorted"() {
    given: 'a dashboard with a button'
    def dashboard = buildDashboard(defaults: ['Content A'], buttons: [['/aUrl', '/bUrl', '/cUrl']])

    and: 'the sequences are changed in the DB to move the second activity to the end'
    DashboardButton.withTransaction {
      dashboard.buttons[1].sequence = 40
      dashboard.buttons[1].save()
    }
    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the buttons details dialog is displayed'
    editorButton('B0').click()
    clickMenu('buttons', 'buttonDetails')
    waitFor { dialog1.exists }

    then: 'the activity grid is correct and sorted on sequence'
    buttonActivities.cell(0, 0).text() == '1'
    buttonActivities.cell(0, 2).text() == '/aUrl'
    buttonActivities.cell(1, 0).text() == '3'
    buttonActivities.cell(1, 2).text() == '/cUrl'
    buttonActivities.cell(2, 0).text() == '40'
    buttonActivities.cell(2, 2).text() == '/bUrl'

    when: 'the activity for sequence 30 is deleted'
    buttonActivities.cell(1, 0).click()
    buttonActivities.removeRowButton.click()

    and: 'the dialog is closed'
    dialog1.okButton.click()
    waitFor { !dialog1.exists }

    then: 'the title indicates the dashboard has changed'
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the record in the DB is correct'
    waitForRecordChange(dashboard)
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.buttons.size() == 2

    dashboard2.buttons[0].sequence == 1
    dashboard2.buttons[0].panel == 'A'
    dashboard2.buttons[0].url == '/aUrl'

    and: 'the new activity has the calculated sequence'
    dashboard2.buttons[1].sequence == 40
    dashboard2.buttons[1].panel == 'A'
    dashboard2.buttons[1].url == '/bUrl'

    and: 'the two activities are saved as separate button records with the same label/ID'
    dashboard2.buttons[0].title == dashboard2.buttons[1].title
    dashboard2.buttons[0].buttonID == dashboard2.buttons[1].buttonID
  }

  def "verify that the buttons can be renumbered"() {
    given: 'a dashboard with a button'
    def dashboard = buildDashboard(defaults: ['Content A'], buttons: ['/aUrl', '/bUrl', '/cUrl'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the buttons details dialog is displayed'
    editorButton('B0').click()
    clickMenu('buttons', 'renumberButtons')

    then: 'the title indicates the dashboard has changed'
    dialog0.title.startsWith('*')

    when: 'the changes are saved'
    saveEditorChanges(dashboard)

    then: 'the record in the DB is correct'
    waitForRecordChange(dashboard)
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.buttons[0].sequence == 10
    dashboard2.buttons[0].url == '/aUrl'
    dashboard2.buttons[1].sequence == 20
    dashboard2.buttons[1].url == '/bUrl'
    dashboard2.buttons[2].sequence == 30
    dashboard2.buttons[2].url == '/cUrl'
  }

  def "verify that button validation triggers save failure with message"() {
    given: 'a dashboard with a button'
    def dashboard = buildDashboard(defaults: ['Content A'], buttons: ['/aUrl'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the editor is opened'
    openEditor()

    and: 'the buttons details dialog is displayed'
    editorButton('B0').click()
    clickMenu('buttons', 'buttonDetails')
    waitFor { dialog1.exists }

    and: 'a bad panel is entered'
    buttonActivities.cell(0, 1).click()
    sendKey('BAD')
    sendKey(Keys.TAB)

    and: 'the dialog is closed'
    dialog1.okButton.click()
    waitFor { !dialog1.exists }

    and: 'the save is attempted'
    editorSaveButton.click()
    waitFor() { dialog0.messages.text() }

    then: 'an error is displayed in the dialog'
    //error.204.message=The button {1} references an invalid panel {2}
    dialog0.messages.text().contains(lookup('error.204.message', null, 'buttons', dashboard.buttons[0], 'BAD'))

    and: 'the record in the DB is unchanged'
    def dashboard2 = DashboardConfig.findByUuid(dashboard.uuid)
    dashboard2.version == dashboard.version
    dashboard2.buttons[0].panel == 'A'
  }

}
