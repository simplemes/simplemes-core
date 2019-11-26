package org.simplemes.eframe.custom.gui

import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.page.TextFieldModule
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent
import sample.page.AllFieldsDomainCreatePage
import sample.page.SampleParentCreatePage
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of the eframe_definition.js definition editor functions.  Tests customization of definition
 * GUIs.
 */
@IgnoreIf({ !sys['geb.env'] })
class DefinitionEditorGUISpec extends BaseDefinitionEditorSpecification {


  @SuppressWarnings("unused")
  static dirtyDomains = [FieldGUIExtension, FieldExtension]

  /**
   * The view ID of the available list.
   */
  public static final String AVAILABLE_LIST = 'available'

  /**
   * The view ID of the configured list.
   */
  public static final String CONFIGURED_LIST = 'configured'

  // *********************************************************************************************
  //  Some support methods for testing the definition editor.
  // *********************************************************************************************


  def "verify that editor initial display works - core fields only"() {
    when: 'the definition page is shown'
    openEditor(AllFieldsDomainCreatePage)

    then: 'the title is correct'
    dialog0.title == lookup('definitionEditor.title')

    and: 'the available list has the expected field'
    def anotherFieldItem = findListElement('anotherField')
    anotherFieldItem.text() == lookup('anotherField.label')
    findParentListForElement('anotherField').@view_id == AVAILABLE_LIST

    and: 'the configured list has the expected field - title'
    def titleItem = findListElement('title')
    titleItem.text() == lookup('title.label')
    findParentListForElement('title').@view_id == CONFIGURED_LIST
    titleItem.siblings('img').@src.contains('textField.png')

    and: 'the configured list has the expected field - dueDate'
    def dueDateItem = findListElement('dueDate')
    dueDateItem.text() == lookup('dueDate.label')
    findParentListForElement('dueDate').@view_id == CONFIGURED_LIST
    dueDateItem.siblings('img').@src.contains('dateField.png')

    and: 'the configured list has the expected field - enabled'
    def enabledItem = findListElement('enabled')
    enabledItem.text() == lookup('enabled.label')
    findParentListForElement('enabled').@view_id == CONFIGURED_LIST
    enabledItem.siblings('img').@src.contains('checkBox.png')

    and: 'the configured list has the expected field - panel'
    def detailsPanelItem = findListElement('group-details')
    detailsPanelItem.text() == lookup('details.panel.label')
    findParentListForElement('group-details').@view_id == CONFIGURED_LIST
    detailsPanelItem.siblings('img').@src.contains('tabbedPanels.png')

    and: 'the buttons are localized'
    button('save').text() == lookup('save.label')
    buttonTooltip('save') == lookup('save.tooltip')
    button('cancel').text() == lookup('cancel.label')
    buttonTooltip('cancel') == lookup('cancel.tooltip')
  }

  def "verify that editor drag and drop can add a core field to the fieldOrder"() {
    when: 'the definition page is shown'
    openEditor(AllFieldsDomainCreatePage)

    and: 'the field is dragged to the configured list'
    dragListElement('anotherField', 'title')

    then: 'the field is now in the right place'
    findParentListForElement('anotherField').@view_id == CONFIGURED_LIST

    when: 'the record is saved'
    button('save').click()
    waitForNonZeroRecordCount(FieldGUIExtension)

    then: 'the field order record is correct'
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(AllFieldsDomain)
    fieldOrder.indexOf('anotherField') < fieldOrder.indexOf('title')

    and: 'the saved message is displayed'
    messages.text().contains(lookup('definitionEditor.saved.message'))
  }

  def "verify that editor drag and drop can move a core field to the fieldOrder"() {
    when: 'the definition page is shown'
    openEditor(AllFieldsDomainCreatePage)

    and: 'the field is dragged to the configured list'
    dragListElement('title', 'count')

    then: 'the field is now in the right place'
    findListElement('title').y > findListElement('qty').y

    when: 'the record is saved'
    button('save').click()
    waitForNonZeroRecordCount(FieldGUIExtension)

    then: 'the field order record is correct'
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(AllFieldsDomain)
    fieldOrder.indexOf('qty') < fieldOrder.indexOf('title')
  }

  def "verify that editor drag and drop can move a custom field in the fieldOrder"() {
    given: 'a custom field'
    buildCustomField(fieldName: 'custom1', domainClass: AllFieldsDomain)
    def fieldGUIExtension = null
    FieldGUIExtension.withTransaction {
      fieldGUIExtension = FieldGUIExtension.list()[0]
    }

    when: 'the definition page is shown'
    openEditor(AllFieldsDomainCreatePage)

    and: 'the field is dragged to the configured list'
    dragListElement('custom1', 'count')

    then: 'the field is now in the right place'
    findListElement('custom1').y > findListElement('qty').y

    when: 'the record is saved'
    button('save').click()
    waitForRecordChange(fieldGUIExtension)

    then: 'the field order record is correct'
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(AllFieldsDomain)
    fieldOrder.indexOf('qty') < fieldOrder.indexOf('custom1')
  }

  def "verify that editor drag and drop can remove a core field from the fieldOrder"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the field is dragged to the available list'
    dragListElement('title', 'notDisplayed')

    then: 'the field is now in the right place'
    findParentListForElement('notDisplayed').@view_id == AVAILABLE_LIST

    when: 'the configuration is saved'
    button('save').click()
    waitForNonZeroRecordCount(FieldGUIExtension)

    then: 'the field order record is correct'
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)
    !fieldOrder.contains('title')

    and: 'the create page is displayed again'
    waitFor {
      messages.text() == ''
    }

    and: 'the title field is not displayed'
    !$('div.webix_el_label', view_id: "TitleLabel").displayed
  }

  def "verify that a custom tabbed panel can be added to the field order"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the add panel dialog is opened'
    clickMenu('panels', 'addPanel')
    waitFor { dialog1.exists }

    then: 'the panel field is visible'
    def panelField = $("body").module(new TextFieldModule(field: 'panel'))
    panelField.input.value() == 'custom'

    and: 'marked as required'
    //panelField.input.attr('aria-required') == 'true'
    panelField.required

    and: 'the title is correct'
    dialog1.title == lookup('definitionEditor.addPanel.title')

    when: 'the panel is added'
    button('dialog1-ok').click()
    waitFor { !dialog1.exists }

    then: 'the new panel is in the available list'
    findParentListForElement('group-custom').@view_id == AVAILABLE_LIST

    when: 'the panel is dragged to the configured columns'
    dragListElement('group-custom', 'notes')

    then: 'the panel is is the configured list'
    findParentListForElement('group-custom').@view_id == CONFIGURED_LIST

    when: 'the configuration is saved'
    button('save').click()
    waitForNonZeroRecordCount(FieldGUIExtension)

    then: 'the field order record is correct'
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)
    fieldOrder.contains('group:custom')

    and: 'the create page is displayed again'
    waitFor {
      messages.text() == ''
    }

    and: 'the custom panel is used'
    // Bug: no main panel displayed.  Check tests.
    mainPanel.displayed
    $('div.webix_item_tab', button_id: "customBody").displayed
  }

  def "verify that the addPanel dialog gracefully detects and enforces a missing panel name"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the add panel dialog is opened'
    clickMenu('panels', 'addPanel')
    waitFor { dialog1.exists }

    then: 'the field has focus'
    def panelField = $("body").module(new TextFieldModule(field: 'panel'))
    waitFor { panelField.input.focused }

    and: 'the field can be blanked and Ok is clicked'
    sendKey(' ')
    button('dialog1-ok').click()

    then: 'the error message is displayed in the dialog without closing the dialog'
    dialog1.exists
    dialog1Messages.text().contains(lookup('error.1.message', lookup('panel.label')))
  }

  def "verify that the addPanel dialog gracefully detects and enforces a duplicate panel name"() {
    given: 'a custom panel configured for the domain'
    buildCustomPanel(domainClass: SampleParent, panel: 'group:custom1', afterFieldName: 'title')

    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the add panel dialog is opened'
    clickMenu('panels', 'addPanel')
    waitFor { dialog1.exists }

    then: 'the existing panel is entered into the panel field'
    sendKey('custom1')
    button('dialog1-ok').click()

    then: 'the error message is displayed in the dialog without closing the dialog'
    dialog1.exists
    dialog1Messages.text().contains(lookup('error.133.message', 'custom1'))
  }

  def "verify that the editPanel dialog can change an existing custom panel - menu"() {
    given: 'a custom panel configured for the domain'
    def fieldGUIExtension = buildCustomPanel(domainClass: SampleParent, panel: 'group:custom1', afterFieldName: 'title')

    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the custom panel is selected'
    selectListElement('group-custom1')

    and: 'the edit panel dialog is opened'
    clickMenu('panels', 'editPanel')
    waitFor { dialog1.exists }

    then: 'the title is correct'
    dialog1.title == lookup('definitionEditor.editPanel.title')

    when: 'a change panel name is entered and Ok is pressed'
    sendKey('custom2')
    button('dialog1-ok').click()

    then: 'the new panel is in the available list'
    findParentListForElement('group-custom2').@view_id == CONFIGURED_LIST

    when: 'the configuration is saved'
    button('save').click()
    waitForRecordChange(fieldGUIExtension)

    then: 'the field order record is correct'
    def fieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)
    fieldOrder.contains('group:custom2')

    and: 'the create page is displayed again'
    waitFor {
      messages.text() == ''
    }

    and: 'the custom panel is used'
    // Bug: no main panel displayed.  Check tests.
    mainPanel.displayed
    $('div.webix_item_tab', button_id: "custom2Body").displayed
  }

  def "verify that editPanel dialog can be opened with double-clicking in configured list"() {
    given: 'a custom panel configured for the domain'
    buildCustomPanel(domainClass: SampleParent, panel: 'group:custom1', afterFieldName: 'title')

    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the custom panel is double-clicked'
    interact { doubleClick(findListElement('group-custom1')) }
    waitFor { dialog1.exists }

    then: 'the correct panel is being edited'
    def panelField = $("body").module(new TextFieldModule(field: 'panel'))
    panelField.input.value() == 'custom1'
  }

  def "verify that editPanel dialog can be opened with double-click on custom panel in the available list"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the add panel dialog is opened'
    clickMenu('panels', 'addPanel')
    waitFor { dialog1.exists }

    and: 'the panel is changed'
    def panelField = $("body").module(new TextFieldModule(field: 'panel'))
    panelField.input.value('custom1')

    and: 'the panel change is done'
    button('dialog1-ok').click()
    waitFor { !dialog1.exists }

    then: 'the new panel is in the available list'
    findParentListForElement('group-custom1').@view_id == AVAILABLE_LIST

    when: 'the custom panel is double-clicked'
    interact { doubleClick(findListElement('group-custom1')) }
    waitFor { dialog1.exists }

    then: 'the correct panel is being edited'
    def panelField2 = $("body").module(new TextFieldModule(field: 'panel'))
    panelField2.input.value() == 'custom1'
  }

  def "verify that editPanel menu item fails when nothing is selected"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the edit panel dialog is opened'
    clickMenu('panels', 'editPanel')

    then: 'the error message is displayed in the main editor dialog'
    //error.132.message=Please select a custom panel to edit.
    !dialog1.exists
    dialog0Messages.text().contains(lookup('error.132.message'))
  }

  def "verify that editPanel menu item fails when wrong type of field is selected"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'a normal field is selected'
    selectListElement('title')

    and: 'the edit panel dialog is opened'
    clickMenu('panels', 'editPanel')

    then: 'the error message is displayed in the main editor dialog'
    //error.132.message=Please select a custom panel to edit.
    !dialog1.exists
    dialog0Messages.text().contains(lookup('error.132.message'))
  }

  def "verify that editPanel menu item fails when core panel is selected"() {
    when: 'the definition page is shown'
    openEditor(AllFieldsDomainCreatePage)

    and: 'a core panel is selected'
    selectListElement('group-details')

    and: 'the edit panel dialog is opened'
    clickMenu('panels', 'editPanel')

    then: 'the error message is displayed in the main editor dialog'
    //error.132.message=Please select a custom panel to edit.
    !dialog1.exists
    dialog0Messages.text().contains(lookup('error.132.message'))
  }

  def "verify that editPanel ok validation fails when renaming to a pre-existing panel"() {
    given: 'two custom panels configured for the domain'
    buildCustomPanel(domainClass: AllFieldsDomain,
                     list: [[panel: 'group:custom1', afterFieldName: 'title'],
                            [panel: 'group:custom2', afterFieldName: 'count']])

    when: 'the definition page is shown'
    openEditor(AllFieldsDomainCreatePage)

    and: 'the second custom panel is selected'
    selectListElement('group-custom2')

    and: 'the edit panel dialog is opened'
    clickMenu('panels', 'editPanel')

    and: 'the panel name is changed to the other existing panel'
    def panelField = $("body").module(new TextFieldModule(field: 'panel'))
    panelField.input.value('custom1')

    and: 'the Ok is clicked to change the panel'
    button('dialog1-ok').click()

    then: 'the error message is displayed in the main editor dialog'
    //error.133.message=Panel {0} already exists in the configured list.
    dialog1.exists
    dialog1Messages.text().contains(lookup('error.133.message', 'custom1'))
  }

  def "verify that addField dialog works"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the add field panel dialog is opened'
    clickMenu('fields', 'addField')
    waitFor { dialog1.exists }

    then: 'the add field dialog is displayed'
    fieldName.label == lookupRequired('fieldName.label')

    when: 'the values are filled in and the focus is in the fieldName field'
    sendKey('custom1')
    //fieldName.input.value('custom1')
    fieldLabel.input.value('Custom1')
    setCombobox((Object) fieldFormat, IntegerFieldFormat.instance.id)
    maxLength.input.value('237')
    valueClassName.input.value(SampleParent.name)

    and: 'the field is saved'
    saveFieldButton.click()
    waitFor { !dialog1.exists }
    waitForNonZeroRecordCount(FieldExtension)

    then: 'the field is in the available list'
    findParentListForElement('custom1').@view_id == AVAILABLE_LIST

    and: 'the custom field record is saved to the DB'
    FieldExtension.withTransaction {
      FieldExtension fe = FieldExtension.findByDomainClassNameAndFieldName(SampleParent.name, 'custom1')
      assert fe.fieldLabel == 'Custom1'
      //noinspection GrEqualsBetweenInconvertibleTypes
      assert fe.fieldFormat == IntegerFieldFormat.instance
      assert fe.maxLength == 237
      assert fe.valueClassName == SampleParent.name
      true
    }
  }

  def "verify that addField dialog validation failures are handled gracefully"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the add field panel dialog is opened'
    clickMenu('fields', 'addField')
    waitFor { dialog1.exists }

    then: 'the add field dialog is displayed'
    fieldName.label == lookupRequired('fieldName.label')
    dialog1.title == lookup('definitionEditor.addField.title')

    when: 'no values are filled in and the save is attempted'
    saveFieldButton.click()
    waitFor { dialog1Messages.text() }

    then: 'the validation error is displayed'
    UnitTestUtils.assertContainsAllIgnoreCase(dialog1Messages.text(), ['missing', 'fieldName'])

    and: 'no custom field record is saved to the DB'
    FieldExtension.withTransaction {
      assert FieldExtension.list().size() == 0
      true
    }
  }

  def "verify that editField dialog can be opened with the menu"() {
    given: 'a custom field is defined'
    def fieldExtension = buildCustomField(fieldName: 'custom1', domainClass: SampleParent,
                                          fieldLabel: 'a custom label', maxLength: 237,
                                          fieldFormat: IntegerFieldFormat.instance, valueClassName: AllFieldsDomain.name)

    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the custom field is selected'
    selectListElement('custom1')

    and: 'the dialog is opened'
    clickMenu('fields', 'editField')
    waitFor { dialog1.exists }

    then: 'the edit field dialog is displayed'
    dialog1.title == lookup('definitionEditor.editField.title')
    fieldName.label == lookupRequired('fieldName.label')

    when: 'the values are filled in'
    fieldName.input.value('custom2')
    fieldLabel.input.value('Custom2')
    setCombobox((Object) fieldFormat, DateFieldFormat.instance.id)
    maxLength.input.value('437')
    valueClassName.input.value('sample.test.TestClass')

    and: 'the field is saved'
    saveFieldButton.click()
    waitFor { !dialog1.exists }
    waitForRecordChange(fieldExtension)

    then: 'the field is in the configured list'
    findParentListForElement('custom2').@view_id == CONFIGURED_LIST

    def customItem = findListElement('custom2')
    customItem.text() == 'Custom2'
    customItem.siblings('img').@src.contains('dateField.png')

    and: 'the custom field record is saved to the DB'
    FieldExtension.withTransaction {
      FieldExtension fe = FieldExtension.findByDomainClassNameAndFieldName(SampleParent.name, 'custom2')
      assert fe.fieldLabel == 'Custom2'
      //noinspection GrEqualsBetweenInconvertibleTypes
      assert fe.fieldFormat == DateFieldFormat.instance
      assert fe.maxLength == 437
      assert fe.valueClassName == 'sample.test.TestClass'
      true
    }
  }

  def "verify that editField dialog can be opened with double-click"() {
    given: 'a custom field is defined'
    buildCustomField(fieldName: 'custom1', domainClass: SampleParent,
                     fieldLabel: 'a custom label', maxLength: 237,
                     fieldFormat: IntegerFieldFormat.instance, valueClassName: AllFieldsDomain.name)

    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the custom field is double-clicked'
    interact { doubleClick(findListElement('custom1')) }
    waitFor { dialog1.exists }

    then: 'the edit field dialog is displayed'
    dialog1.title == lookup('definitionEditor.editField.title')
  }

  def "verify that editField menu item fails when nothing is selected"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the edit field dialog is opened'
    clickMenu('fields', 'editField')

    then: 'the error message is displayed in the main editor dialog'
    //error.135.message=Please select a custom field to edit.
    !dialog1.exists
    dialog0Messages.text().contains(lookup('error.135.message'))
  }

  def "verify that editField menu item fails when wrong type of field is selected"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'a normal field is selected'
    selectListElement('title')

    and: 'the edit field dialog is opened'
    clickMenu('fields', 'editField')

    then: 'the error message is displayed in the main editor dialog'
    //error.135.message=Please select a custom field to edit.
    !dialog1.exists
    dialog0Messages.text().contains(lookup('error.135.message'))
  }

  def "verify that editField menu item fails when core panel is selected"() {
    when: 'the definition page is shown'
    openEditor(AllFieldsDomainCreatePage)

    and: 'a core field is selected'
    selectListElement('title')

    and: 'the edit field dialog is opened'
    clickMenu('fields', 'editField')

    then: 'the error message is displayed in the main editor dialog'
    //error.135.message=Please select a custom field to edit.
    !dialog1.exists
    dialog0Messages.text().contains(lookup('error.135.message'))
  }

  def "verify that deleteField works with a custom field"() {
    given: 'a custom field is defined'
    buildCustomField(fieldName: 'custom1', domainClass: SampleParent,
                     fieldLabel: 'a custom label', maxLength: 237,
                     fieldFormat: IntegerFieldFormat.instance, valueClassName: AllFieldsDomain.name)

    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the custom field is selected'
    selectListElement('custom1')

    and: 'the delete menu item is selected'
    clickMenu('fields', 'deleteField')
    waitFor { dialog1.exists }

    and: 'the user presses Ok'
    dialog1.okButton.click()
    waitFor { !dialog1.exists }

    then: 'the field is not in either list'
    findListElement('custom1').isEmpty()

    and: 'the record is removed from the DB'
    FieldExtension.withTransaction {
      assert FieldExtension.findByDomainClassNameAndFieldName(AllFieldsDomain.name, 'custom1') == null
      true
    }
  }

  def "verify that deleteField detects when nothing is selected"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the delete menu item is selected'
    clickMenu('fields', 'deleteField')
    waitFor { dialog0Messages.text() != '' }

    then: 'the error message is displayed in the main editor dialog'
    //error.135.message=Please select a custom field.
    !dialog1.exists
    dialog0Messages.text().contains(lookup('error.135.message'))
  }

  def "verify that deleteField detects when a core field is selected"() {
    when: 'the definition page is shown'
    openEditor(SampleParentCreatePage)

    and: 'the custom field is selected'
    selectListElement('title')

    and: 'the delete menu item is selected'
    clickMenu('fields', 'deleteField')
    waitFor { dialog0Messages.text() != '' }

    then: 'the error message is displayed in the main editor dialog'
    //error.135.message=Please select a custom field.
    !dialog1.exists
    dialog0Messages.text().contains(lookup('error.135.message'))
  }


  // TODO: Add remember dialog position logic?

  /*

  Legacy tests:
  testAdditionField

   */
}
