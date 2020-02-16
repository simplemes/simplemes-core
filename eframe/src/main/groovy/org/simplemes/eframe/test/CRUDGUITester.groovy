/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import geb.navigator.Navigator
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.openqa.selenium.Keys
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.data.EncodedTypeInterface
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainRefListFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.domain.DomainBinder
import org.simplemes.eframe.domain.DomainReference
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.web.PanelUtils

/**
 * A full-scale GUI tester to test a standardized set of definition pages for a single domain/controller.
 * This tester covers list/show/create/edit pages for a given domain.  It performs these test in the current
 * locale as specified on the command-line (-Dgeb.lang=de-DE).
 * <p>
 * <b>Note:</b> The record created by the <code>recordParams</code> should be sorted by default to appear in the list before
 * <code>minimalParams</code> record.
 * <p>
 * <b>Note:</b> Child records should be created with a list of Maps for the child parameters.  Do not create the the
 *              child records in your code.
 * <p>
 *
 * <h3>-D Options</h3>
 * The -D command-line options for this tester include:
 * <ul>
 *   <li><b>testOnly=phase</b> - -DtestOnly=create for create phase only.  Supports phases: show, create,list, edit.  </li>
 *   <li><b>slowTest</b> - If present, then the test is slowed down after every field that is filled in (create/edit only). </li>
 * </ul>
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs. </li>
 *   <li><b>trace</b> - Logs each field as it is tested and records processed. </li>
 * </ul>
 * */
@Slf4j
@ToString(includeNames = true, includePackage = false)
class CRUDGUITester {
  /**
   * The calling GUI test class.
   */
  BaseGUISpecification _tester

  /**
   * The domain class to test.
   */
  Class _domain

  /**
   * Values to set in the domain object for created and edit.
   * If there are any initial data load records, then this record should appear before the initially loaded records.
   */
  Map<String, Object> _recordParams = [:]

  /**
   * The parameters needed to create a second test record with the minimal required fields specified.
   * If there are any initial data load records, then this minimal record should appear before the initially loaded records.
   * This record should appear alphabetically after the <code>recordParams</code> in the list page with the default sorting.
   * This record should have no child records populated.
   */
  Map<String, Object> _minimalParams = [:]

  /**
   * The base name for the standard elements' HTML IDs.  Defaults to the domain class simple name.
   */
  String _htmlIDBase

  /**
   * The URI to the controller for this domain class.
   */
  String _uri

  /**
   * If true, then enabled the list page tests.  (<b>Default:</b> true).
   */
  boolean _enableListTests = true

  /**
   * A list of columns that are displayed in the list page.  This is parsed from the
   * input option <code>listColumns</code>.  Comma-delimited list.
   */
  String _listColumns

  /**
   * If true, then enabled the show page tests.  (<b>Default:</b> true).
   */
  boolean _enableShowTests = true

  /**
   * A list of fields that are displayed in the show page.  These are the fields that are tested on the show page.
   * This is parsed from the input option <code>createFields</code>.  Comma-delimited list.
   */
  String _showFields

  /**
   * A list of fields that are displayed in the create page.  These are the fields that are tested on the create page.
   * This is parsed from the input option <code>createFields</code>.  Comma-delimited list.
   */
  String _createFields

  /**
   * A list of fields that are displayed in the edit page.  These are the fields that are tested on the edit page.
   * This is parsed from the input option <code>editFields</code>.  Comma-delimited list.
   */
  String _editFields

  /**
   * The -D testOnly option from the command line (or simulated for unit testing).
   */
  String _dashDOption

  /**
   * The -D slowTest option from the command line.  If true, then a small delay will be added after each field.
   */
  boolean slowTest = false

  /**
   * If true, then enabled the create page tests.  (<b>Default:</b> true).
   */
  boolean _enableCreateTests = true

  /**
   * If true, then enabled the edit page tests.  (<b>Default:</b> true).
   */
  boolean _enableEditTests = true

  /**
   * A closure to run when the create page is tested.  Typically used to set values in the GUI before the save.
   */
  Closure _createClosure

  /**
   * A closure to run when the edit page is tested.  Typically used to set values in the GUI before the save.
   */
  Closure _editClosure

  /**
   * A list of fields that do not have a label.
   */
  List<String> _unlabeledFields

  /**
   * A list of fields that are not editable on create/edit pages.
   */
  List<String> _readOnlyFields

  /**
   * Sets the domain to Spock test specification to be used to the tests.
   * @param tester The spock test class this CRUD test will be inside of.
   */
  void tester(final BaseGUISpecification tester) { this._tester = tester }

  /**
   * Sets the domain to test the CRUD GUIs for.
   * @param domain The domain class
   */
  void domain(final Class domain) { this._domain = domain }

  /**
   * Values to set in the domain object for created and edit.
   * If there are any initial data load records, then this record should appear before the initially loaded records.
   * @param recordParams The values.
   */
  void recordParams(final Map<String, Object> recordParams) { this._recordParams = recordParams }

  /**
   * The parameters needed to create a second test record with the minimal required fields specified.
   * If there are any initial data load records, then this minimal record should appear before the initially loaded records.
   * This record should appear alphabetically after the <code>recordParams</code> in the list page with the default sorting.
   * This record should have no child records populated.
   * @param minimalParams The values.
   */
  void minimalParams(final Map<String, Object> minimalParams) { this._minimalParams = minimalParams }

  /**
   * The -D testOnly option from the command line (or simulated for unit testing).
   * @param dashDOption The option string.
   */
  void dashDOption(final String dashDOption) { this._dashDOption = dashDOption }

  /**
   * If true, then enable the list page tests.  (<b>Default:</b> true).
   * @param enableListTests True if enabled.
   */
  void enableListTests(final boolean enableListTests) { this._enableListTests = enableListTests }

  /**
   * A list of columns that are displayed in the list page.  This is parsed from the
   * input option <code>listColumns</code>.  A comma-delimited list.
   */
  void listColumns(final String list) { this._listColumns = list }

  /**
   * If true, then enable the show page tests.  (<b>Default:</b> true).
   * @param enableShowTests True if enabled.
   */
  void enableShowTests(final boolean enableShowTests) { this._enableShowTests = enableShowTests }

  /**
   * A list of fields that are displayed in the show page.  This is parsed from the
   * input option <code>showFields</code>.  A comma-delimited list.
   */
  void showFields(final String list) { this._showFields = list }

  /**
   * If true, then enable the create page tests.  (<b>Default:</b> true).
   * @param enableCreateTests True if enabled.
   */
  void enableCreateTests(final boolean enableCreateTests) { this._enableCreateTests = enableCreateTests }

  /**
   * A list of columns that are displayed in the create page.  This is parsed from the
   * input option <code>createFields</code>.  A comma-delimited list.
   */
  void createFields(final String list) { this._createFields = list }

  /**
   * If true, then enable the edit page tests.  (<b>Default:</b> true).
   * @param enableEditTests True if enabled.
   */
  void enableEditTests(final boolean enableEditTests) { this._enableEditTests = enableEditTests }

  /**
   * A list of columns that are displayed in the edit page.  This is parsed from the
   * input option <code>editFields</code>.  A comma-delimited list.
   */
  void editFields(final String list) { this._editFields = list }

  /**
   * The base name for the standard elements' HTML IDs.  Defaults to the domain class simple name.
   */
  void htmlIDBase(final String id) { this._htmlIDBase = id }

  /**
   * The URI to the controller for this domain class.  Defaults to the base name (e.g. '/user').
   */
  void setUri(final String uri) { this._uri = uri }

  /**
   * Set the closure to run when the create page is tested.  Typically used to set values in the GUI before the save.
   */
  void createClosure(final Closure closure) { this._createClosure = closure }

  /**
   * Set the closure to run when the edit page is tested.  Typically used to set values in the GUI before the save.
   */
  void editClosure(final Closure closure) { this._editClosure = closure }

  /**
   * A list of fields that do not have a label.
   * @param s The list of columns (comma-delimited list of field names).
   */
  void unlabeledFields(final String s) { this._unlabeledFields = s?.tokenize(', ') }

  /**
   * A list of fields that are not editable in the create/edit pages.
   * @param s The list of columns (comma-delimited list of field names).
   */
  void readOnlyFields(final String s) { this._readOnlyFields = s?.tokenize(', ') }


  // **************************
  //   Internal variables.
  // **************************

  /**
   * The locale being tested.
   */
  protected Locale currentLocale = Locale.US

  /**
   * The domain name (with initial lowercase letter).
   */
  protected String domainName


  /**
   * The browser automation object (from GEB).
   */
  protected browser

  /**
   * The effective list of columns that are displayed in the list page.
   */
  protected List<String> effectiveListColumns

  /**
   * The effective list of fields that are displayed in the show page.
   */
  protected List<String> effectiveShowFields

  /**
   * The fields to be displayed in a specific panel.  Stores the list of fields per panel.
   */
  Map<String, List<String>> effectiveShowFieldsByPanel

  /**
   * The effective list of fields that are displayed in the create page.
   */
  protected List<String> effectiveCreateFields

  /**
   * The fields to be displayed in a specific panel on the create page.  Stores the list of fields per panel.
   */
  Map<String, List<String>> effectiveCreateFieldsByPanel

  /**
   * The effective list of fields that are displayed in the edit page.
   */
  protected List<String> effectiveEditFields

  /**
   * The fields to be displayed in a specific panel on the edit page.  Stores the list of fields per panel.
   */
  Map<String, List<String>> effectiveEditFieldsByPanel

  /**
   * The fieldOrder list (flattened) from the domain class.
   */
  protected List<String> fieldOrder

  /**
   * The number of tests run.
   */
  int testCount = 0

  /**
   * The field definitions for the domain class.
   */
  protected FieldDefinitions fieldDefinitions

  /**
   * If true, then the test is working on the create page.  Edit and Create mode share a lot of common logic.
   */
  protected boolean createMode = true

  /**
   * Set to true it appears the page has tabbed panels.
   */
  protected boolean hasPanels = false

  /**
   * Triggers the test itself.
   * @param config The configuration.
   * @return The number of tests executed (1 for list page, 1 for show page, etc).
   */
  static CRUDGUITester test(@DelegatesTo(CRUDGUITester) final Closure config) {
    CRUDGUITester crudTester = new CRUDGUITester()
    crudTester.with config
    log.debug('test(): crudTester {}', crudTester)

    ArgumentUtils.checkMissing(crudTester._domain, 'domain')
    ArgumentUtils.checkMissing(crudTester._tester, 'tester')
    ArgumentUtils.checkMissing(crudTester._recordParams, 'recordParams')
    ArgumentUtils.checkMissing(crudTester._minimalParams, 'minimalParams')

    crudTester.doSetup()
    crudTester.doTest()

    return crudTester
  }

  /**
   * Prepare for the test.
   */
  void doSetup() {
    domainName = NameUtils.toDomainName(_domain)
    browser = _tester.browser
    _htmlIDBase = _htmlIDBase ?: domainName
    _uri = _uri ?: "/${domainName}"
    fieldOrder = DomainUtils.instance.getStaticFieldOrder(_domain)
    fieldDefinitions = DomainUtils.instance.getFieldDefinitions(_domain)
    log.trace("doSetup(): ID = {}, URI = {}, fieldOrder = {}", _htmlIDBase, _uri, fieldOrder)
    // The code value overrides the -D option for testing purposes.
    _dashDOption = _dashDOption ?: System.getProperty('testOnly')
    slowTest = System.getProperty('slowTest') != null

    hasPanels = (fieldOrder.find { PanelUtils.isPanel(it) } != null)

    checkDOptions()
    configureListColumns()
    configureShowFields()
    configureCreateFields()
    configureEditFields()
  }

  /**
   * Configures the list of columns to test on the list page.  Removes any panel and other non-column fields.
   * Also currently removes the sub-object references since they are not tested (yet).
   */
  protected void configureListColumns() {
    def list = _listColumns?.tokenize(', ')
    effectiveListColumns = list ?: fieldOrder.findAll { !PanelUtils.isPanel(it) }

    // drop the sub object references.
    effectiveListColumns.removeAll() { it.contains('.') || PanelUtils.isPanel(it) }
  }

  /**
   * Configures the list of fields to test on the show page.  Removes any panel and other non-column fields.
   * Also organizes by panel if panels are used.
   */
  protected void configureShowFields() {
    def list = _showFields?.tokenize(', ')
    effectiveShowFields = (List<String>) (list ?: fieldOrder.clone())

    // Figure out the panels (if any) needed.
    effectiveShowFieldsByPanel = PanelUtils.organizeFieldsIntoPanels(effectiveShowFields)

    // drop the sub object references and panels.
    effectiveShowFields.removeAll() { it.contains('.') || PanelUtils.isPanel(it) }
  }


  /**
   * Configures the list of fields to test on the create page.  Removes any panel and other non-column fields.
   * Also organizes by panel if panels are used.
   */
  protected void configureCreateFields() {
    def list = _createFields?.tokenize(', ')
    effectiveCreateFields = (List<String>) (list ?: fieldOrder.clone())

    // Figure out the panels (if any) needed.
    effectiveCreateFieldsByPanel = PanelUtils.organizeFieldsIntoPanels(effectiveCreateFields)

    // drop the sub object references and panels.
    effectiveCreateFields.removeAll() { it.contains('.') || PanelUtils.isPanel(it) }
  }

  /**
   * Configures the list of fields to test on the edit page.  Removes any panel and other non-column fields.
   * Also organizes by panel if panels are used.
   */
  protected void configureEditFields() {
    def list = _editFields?.tokenize(', ')
    effectiveEditFields = (List<String>) (list ?: fieldOrder.clone())

    // Figure out the panels (if any) needed.
    effectiveEditFieldsByPanel = PanelUtils.organizeFieldsIntoPanels(effectiveEditFields)

    // drop the sub object references and panels.
    effectiveCreateFields.removeAll() { it.contains('.') || PanelUtils.isPanel(it) }
  }


  /**
   * Tests the domain/controller/pages in all of the desired locale.
   */
  void doTest() {
    def locale = Locale.US
    if (System.getProperty('geb.lang')) {
      locale = Locale.forLanguageTag(System.getProperty('geb.lang'))
      log.debug('Using locale {} from -Dgeb.lang option {} ', locale, System.getProperty('geb.lang'))
    }
    doTest(locale)
  }

  /**
   * Tests the domain/controller/pages in a single locale.
   */
  void doTest(Locale locale) {
    currentLocale = locale
    _tester.login()

    try {
      if (_enableListTests) {
        testListPage()
        testCount++
      }
      if (_enableShowTests) {
        testShowPage()
        testCount++
      }
      if (_enableCreateTests) {
        createMode = true
        testCreatePage()
        testCount++
      }
      if (_enableEditTests) {
        createMode = false
        testEditPage()
        testCount++
      }
/*
      if (enableDeleteTests) {
        testDelete()
      }
*/
    } finally {
      cleanupCreatedRecords()
    }
  }

  /**
   * Perform the tests on the list page.
   */
  void testListPage() {
    def mainRecord = createRecord(_recordParams)
    log.trace("testListPage(): Checking columns {}", effectiveListColumns)
    createRecord(_minimalParams)
    go("")
    waitForCompletion()
    assert browser.title == lookup('list.title', [lookup("${domainName}.label"), Holders.configuration.appName])
    assertLocalized((String) browser.title, 'title', 'list')

    // Now, check the columns
    for (column in effectiveListColumns) {
      def domainReference = DomainReference.buildDomainReference(column, mainRecord)
      checkListColumn(column, "${_htmlIDBase}DefinitionList", domainReference.value, 0)
    }

    def buttonID = NameUtils.lowercaseFirstLetter(domainName)
    checkToolbarButton('', "${buttonID}DefinitionListCreate", '/create')
    // TODO: checkListSearch()

    cleanupCreatedRecords()
  }

  /**
   * Checks a single column in the list.
   * @param columnName The column to check.
   * @param listHTMLID The HTML ID of the list itself.
   * @param value The value that should be displayed in the row.
   * @param row The list row the value is expected in.
   */
  protected checkListColumn(String columnName, String listHTMLID, Object value, int row) {
    //log.trace('Checking list column {} for value {}', columnName, value)
    // Check the header first.
    def columnTitle = lookup("${columnName}.label")
    //noinspection SpellCheckingInspection
    def columnHeaders = _tester.$("div#${listHTMLID}").find('div.webix_hcell')

    def columnIndex = -1
    for (int i = 0; i < columnHeaders.size(); i++) {
      def headerText = columnHeaders[i].text()
      if (columnTitle == headerText) {
        columnIndex = i
        checkLookedUpLabel(headerText)
        break
      }
    }

    assert columnIndex >= 0, "Header '$columnName' not found in list for '$listHTMLID' "

    def cell = _tester.$("div#${listHTMLID}").find('div.webix_column', column: "$columnIndex").find('div.webix_cell', row)
    checkListValue(cell, value, columnName)
  }

  /**
   * Checks the value for a given element's text() against given value.  This is used for lists.
   * @param element The element.
   * @param expectedValue The value.  Supports String or Boolean.
   * @param columnName The column name.
   */
  void checkListValue(Navigator element, Object expectedValue, String columnName) {
    // Handle localization/formatting of values
    //def expectedValueString = formatExpectedValueForShow(expectedValue)
    if (expectedValue instanceof Collection) {
      // A string collection should be displayed
      if (expectedValue) {
        expectedValue = expectedValue.toString()
        expectedValue = expectedValue[1..-2]
      }
    }
    if (expectedValue instanceof Boolean) {
      def checkBox = element.find('input')
      def checked = checkBox.@checked
      log.debug('  Checking list column "{}". Expected = {}, value = {}', columnName, expectedValue, checked)
      assert expectedValue != checked, "Check on boolean $columnName failed (was $checked)"
    } else if (expectedValue instanceof BigDecimal) {
      // Strip trailing zero's to avoid issues with GUI toolkit's fixed number of decimal places issue.
      def s = NumberUtils.trimTrailingZeros(element?.text(), currentLocale)
      def expectedValueString = NumberUtils.trimTrailingZeros((String) formatExpectedValueForShow(expectedValue), currentLocale)
      log.debug('  Checking list column "{}". Expected = {}, value = {}', columnName, expectedValueString, s)
      if (s && expectedValueString) {
        // Compare the BigDecimal form of the values
        def bd = new BigDecimal(s)
        assert bd == expectedValue, "Value check for $columnName failed.  Was $s, expected $expectedValueString"
      } else {
        assert s == expectedValueString, "Value check for $columnName failed.  Was $s, expected $expectedValueString"
      }
    } else {
      // Fallback to string comparison.
      def s = element?.text()
      //println "comparing $columnName expected = $expectedValue, value = ${element?.text()}"
      def expectedValueString = formatExpectedValueForShow(expectedValue)
      log.debug('  Checking list column "{}". Expected = {}, value = {}', columnName, expectedValueString, s)
      def c1 = s?.getClass()?.simpleName
      def c2 = expectedValueString?.getClass()?.simpleName
      def txt = "Value check for $columnName failed.  Was '$s'($c1), expected '$expectedValueString'($c2)"
      assert s == expectedValueString, txt
    }
  }

  /**
   * Perform the tests on the show page.
   */
  void testShowPage() {
    testShowPageFields()
  }

  /**
   * test the displayed fields.
   */
  void testShowPageFields() {
    def mainRecord = createRecord(_recordParams)
    log.trace("testShowPage(): Checking fields {}", effectiveShowFields)
    go("/show/${mainRecord.uuid}")
    waitForCompletion()
    assert browser.title == lookup('show.title', [TypeUtils.toShortString(mainRecord), lookup("${domainName}.label"), Holders.configuration.appName])
    assertLocalized((String) browser.title, 'title', 'show')

    // Make sure the first panel (main) is shown (if any panels)
    if (hasPanels) {
      clickPanel('main')
    }

    if (effectiveShowFieldsByPanel) {
      // Check each panel, one at a time
      for (panel in effectiveShowFieldsByPanel.keySet()) {
        clickPanel(panel)
        for (field in effectiveShowFieldsByPanel[panel]) {
          def domainReference = DomainReference.buildDomainReference(field, mainRecord)
          checkShowField(field, domainReference)
        }
      }
    } else {
      // Now, check the fields
      for (field in effectiveShowFields) {
        def domainReference = DomainReference.buildDomainReference(field, mainRecord)
        checkShowField(field, domainReference)
      }
    }

    checkToolbarButton("/show/${mainRecord.uuid}", "showList", "$_uri")
    checkToolbarButton("/show/${mainRecord.uuid}", "showCreate", "$_uri/create")
    checkToolbarButton("/show/${mainRecord.uuid}", "showEdit", "$_uri/edit/${mainRecord.uuid}")
    testShowPageDelete(mainRecord)
    cleanupCreatedRecords()
  }

  /**
   * Test the delete action on the show page.
   * @param mainRecord The domain record to test.
   */
  void testShowPageDelete(Object mainRecord) {
    def id = mainRecord.uuid

    go("/show/${mainRecord.uuid}")
    waitForCompletion()

    // Open the more actions menu
    def moreMenuButton = _tester.$('a.webix_list_item', webix_l_id: "showMoreMenu")
    moreMenuButton.click()
    _tester.waitFor {
      _tester.$('a.webix_list_item', webix_l_id: "showDelete").displayed
    }

    _tester.$('a.webix_list_item', webix_l_id: "showDelete").click()
    _tester.waitFor {
      _tester.$('div.webix_el_button', view_id: "dialog0-ok").find('button').displayed
    }

    _tester.$('div.webix_el_button', view_id: "dialog0-ok").find('button').click()
    waitForCompletion()

    assert !_tester.currentUrl.endsWith('/show'), "No longer on the show page for ${_domain.simpleName}"

    // make sure the record was deleted.
    _domain.withTransaction {
      assert !_domain.findByUuid(id), "${_domain.simpleName} record (${id}) was deleted"
    }

  }

  /**
   * Perform the tests on the create page.
   */
  @SuppressWarnings("GrReassignedInClosureLocalVar")
  void testCreatePage() {
    // Create a a default record for comparison to the displayed values.
    def mainRecord = _domain.newInstance()
    log.trace("testCreatePage(): Checking fields {}", effectiveCreateFields)
    go("/create")
    waitForCompletion()
    assert browser.title == lookup('create.title', [lookup("${domainName}.label"), Holders.configuration.appName])
    assertLocalized((String) browser.title, 'title', 'create')

    // Make sure the first panel (main) is shown (if any panels)
    if (hasPanels) {
      clickPanel('main')
    }

    if (effectiveCreateFieldsByPanel) {
      // Check each panel, one at a time
      for (panel in effectiveCreateFieldsByPanel.keySet()) {
        log.trace('Checking panel "{}"', panel)
        clickPanel(panel)
        for (field in effectiveCreateFieldsByPanel[panel]) {
          def domainReference = DomainReference.buildDomainReference(field, mainRecord)
          checkAndFillInField(field, domainReference, _recordParams[field])
        }
      }
    } else {
      // Now, check the fields
      for (field in effectiveCreateFields) {
        def domainReference = DomainReference.buildDomainReference(field, mainRecord)
        checkAndFillInField(field, domainReference, _recordParams[field])
      }
    }

    _createClosure?.call()

    clickButton('createSave')

    // Wait for the record to be stored in the DB
    _tester.waitFor {
      findRecord(_recordParams)
    }
    _domain.withTransaction {
      def record = findRecord(_recordParams)
      //println "record = $record, $_recordParams"
      checkRecord(record, _recordParams)
      record.delete()
    }

    checkToolbarButton("/create", "createList", "$_uri")
  }

  /**
   * Perform the tests on the edit page.
   */
  @SuppressWarnings("GrReassignedInClosureLocalVar")
  void testEditPage() {
    // Create a a default record with the minimal values.
    def mainRecord = createRecord(_minimalParams)
    log.trace("testEditPage(): Checking fields {}", effectiveEditFields)
    go("/edit/${mainRecord.uuid}")
    waitForCompletion()
    assert browser.title == lookup('edit.title', [TypeUtils.toShortString(mainRecord), lookup("${domainName}.label"), Holders.configuration.appName])
    assertLocalized((String) browser.title, 'title', 'show')

    // Make sure the first panel (main) is shown (if any panels)
    if (hasPanels) {
      clickPanel('main')
    }

    if (effectiveEditFieldsByPanel) {
      // Check each panel, one at a time
      for (panel in effectiveEditFieldsByPanel.keySet()) {
        log.trace('Checking panel "{}"', panel)
        clickPanel(panel)
        for (field in effectiveEditFieldsByPanel[panel]) {
          def domainReference = DomainReference.buildDomainReference(field, mainRecord)
          checkAndFillInField(field, domainReference, _recordParams[field])
        }
      }
    } else {
      // Now, check the fields
      for (field in effectiveEditFields) {
        def domainReference = DomainReference.buildDomainReference(field, mainRecord)
        checkAndFillInField(field, domainReference, _recordParams[field])
      }
    }

    _editClosure?.call()

    clickButton('editSave')

    // Wait for the record to be stored in the DB
    _tester.waitFor {
      findRecord(_recordParams)
    }
    _domain.withTransaction {
      def record = findRecord(_recordParams)
      //println "record = $record, $_recordParams"
      checkRecord(record, _recordParams)
    }

    checkToolbarButton("/edit/${mainRecord.uuid}", "editList", "$_uri")

    cleanupCreatedRecords()
  }

  /**
   * Checks a single show field label and value.
   * @param fieldName The field to check.
   * @param expectedRef The domain reference for the expected value that should be displayed in the value section.
   */
  protected checkShowField(String fieldName, DomainReference expectedRef) {
    log.trace('  Checking show field "{}" for value "{}"', fieldName, expectedRef?.value)
    def fieldDef = fieldDefinitions?.get(fieldName)
    checkFieldLabel(fieldName)
    checkFieldValue(fieldName, expectedRef, fieldDef, false)
  }


  /**
   * Checks a single edit/create field label and value.  Will fill in the optional value.
   * @param fieldName The field to check.
   * @param expectedRef The domain reference for the expected value that should be displayed in the value section.
   * @param fillInValue The value to fill in the field.  Optional.
   */
  protected checkAndFillInField(String fieldName, DomainReference expectedRef, Object fillInValue = null) {
    log.trace('  Check and Fill field "{}" for value "{}".  Fill In: "{}"', fieldName, expectedRef?.value, fillInValue)
    def fieldDef = fieldDefinitions?.get(fieldName)
    checkFieldLabel(fieldName)
    checkFieldValue(fieldName, expectedRef, fieldDef, true)
    fillInField(fieldName, fillInValue, fieldDef)
    if (slowTest) {
      sleep(500)
    }
  }

  /**
   * Checks the field label.
   * @param fieldName The field name.
   */
  protected void checkFieldLabel(String fieldName) {
    if (_unlabeledFields?.contains(fieldName)) {
      return
    }
    def fieldDefinitions = DomainUtils.instance.getFieldDefinitions(_domain)
    def required = fieldDefinitions[fieldName]?.required
    def labelText = _tester.$('div.webix_el_label', view_id: "${fieldName}Label").text()
    def expectedLabel = required ? lookupRequired("${fieldName}.label") : lookup("${fieldName}.label")
    assert labelText == expectedLabel, "Label for $fieldName is not correct. Found $labelText, expected $expectedLabel"
  }

  /**
   * Checks the field value against the expected value.
   * @param fieldName The field name.
   * @param expectedRef The domain reference for the expected value that should be displayed in the value section.
   * @param fieldDef The field definition.
   * @param editable If true, then the value will come from an input field.
   */
  @SuppressWarnings("EmptyIfStatement")
  protected void checkFieldValue(String fieldName, DomainReference expectedRef,
                                 FieldDefinitionInterface fieldDef, boolean editable) {
    if (_readOnlyFields?.contains(fieldName)) {
      editable = false
    }
    // Check the field value
    //println "$fieldName - $fieldDef"
    if (fieldDef?.format instanceof ChildListFieldFormat) {
      checkInlineGridValues(fieldName, (Collection) expectedRef.value, fieldDef)
    } else if (fieldDef?.format instanceof DomainRefListFieldFormat) {
      // Do nothing.
    } else if (fieldDef?.format instanceof BooleanFieldFormat) {
      // Check the check-box status.
      def checkbox = _tester.$('div.webix_el_checkbox', view_id: "${fieldName}").find('button')
      def checked = (checkbox.attr('aria-checked') == 'true')
      def expectedValue = expectedRef.value ? true : false  // Treat null as false.
      assert checked == expectedValue, "Field Value for $fieldName is not correct. Found $checked, expected $expectedValue"
    } else {
      def valueText
      if (editable) {
        def css = 'webix_el_text'
        if (isComboBoxField(fieldDef?.format)) {
          css = 'webix_el_combo'
        }
        valueText = _tester.$("div.$css", view_id: "${fieldName}").find('input').value() ?: ''
      } else {
        valueText = _tester.$('div.webix_el_label', view_id: "${fieldName}").text()
      }
      def expectedValue = formatExpectedValueForShow(expectedRef.value)
      // Special case used to trigger a test failure for testing this CRUDGUITester.
      expectedValue = fixValueToForceFailure(fieldName, expectedValue)

      assert valueText == expectedValue, "Field Value for $fieldName is not correct. Found '$valueText', expected '$expectedValue'"
    }
  }

  /**
   * Fixes the value to intentionally trigger a test error.  Used to help test error detection.
   * If a value of 'XYZZY' for the field 'title' is detected, then the returned value is altered to force the error.
   * @param fieldName The field.
   * @param expectedValue The expected value.
   * @return The (possibly) altered expected value.
   */
  String fixValueToForceFailure(String fieldName, Object expectedValue) {
    if (fieldName == 'title' && expectedValue == 'XYZZY') {
      expectedValue = expectedValue + "-always fails for test"
      log.warn('Altered title to "{}" to intentionally fail for test.', expectedValue)
    }
    return expectedValue
  }

  /**
   * Fills in the field value.
   * @param fieldName The field name.
   * @param value The value to be filled in the input field. If null, then nothing is changed.
   * @param fieldDef The field definition.
   */
  protected void fillInField(String fieldName, Object value, FieldDefinitionInterface fieldDef) {
    if (value == null) {
      return
    }
    // Check the field value
    if (fieldDef?.format instanceof ChildListFieldFormat) {
      if (value instanceof Collection) {
        for (row in value) {
          addInlineGridRow(fieldName, (Map) row, fieldDef)
        }
      }
    } else if (fieldDef?.format instanceof BooleanFieldFormat) {
      // Check the check-box status.
      def checkbox = _tester.$('div.webix_el_checkbox', view_id: "${fieldName}").find('button')
      def checked = (checkbox.attr('aria-checked') == 'true')
      if (checked != value) {
        checkbox.click()
      }
    } else if (fieldDef?.format instanceof DomainRefListFieldFormat) {
      _tester.setCombobox(fieldName, value.toString())
    } else {
      // Simple text input field.

      // Need to search with the CSS class for the view to speed up search.
      def displayValue = formatExpectedValueForShow(value)
      def css = "webix_el_text"
      if (fieldDef?.format instanceof DateFieldFormat || fieldDef?.format instanceof DateOnlyFieldFormat) {
        css = "webix_el_datepicker"
      }
      if (fieldDef?.format instanceof EnumFieldFormat) {
        _tester.setCombobox(fieldName, value.toString())
        return
      } else if (isComboBoxField(fieldDef?.format) && value) {
        _tester.setCombobox(fieldName, value.uuid.toString())
        return
      } else if (fieldDef?.format instanceof DomainReferenceFieldFormat && value) {
        _tester.setCombobox(fieldName, value.uuid.toString())
        return
      }
      _tester.$("div.$css", view_id: "${fieldName}").find('input').value(displayValue)
    }
  }

  /**
   * Adds a single row to the given inline grid.
   *
   * @param fieldName The grid.
   * @param rowData The row values.
   * @param fieldDef The grid's field definition.
   */
  void addInlineGridRow(String fieldName, Map rowData, FieldDefinitionInterface fieldDef) {
    // Add the row first
    //    addRowButton {  }
    _tester.$("div.webix_el_button", view_id: "${fieldName}Add").find('button').click()

    // Now, tab through the fields, setting the values are defined in the rowData
    def childDomainClass = fieldDef.referenceType
    def fieldOrder = DomainUtils.instance.getStaticFieldOrder(childDomainClass)
    for (field in fieldOrder) {
      def cellValue = rowData[field]
      if (cellValue != null) {
        if (cellValue instanceof Boolean) {
          if (cellValue) {
            // A boolean checkbox is set by pressing space to toggle the value.
            _tester.sendKey(Keys.SPACE)
          }
        } else {
          def s = formatExpectedValueForShow(cellValue)
          _tester.sendKey(s)
        }
      }

      _tester.sendKey(Keys.TAB)
    }


  }

  /**
   * Format the expected value to a display format (string usually).
   * @param object The object to format/localize.
   * @return The display value.
   */
  Object formatExpectedValueForShow(Object object) {
    if (object == null) {
      return ''
    }
    if (object.getClass().isEnum()) {
      object = GlobalUtils.toStringLocalized(object, currentLocale)
    } else if (object instanceof DateOnly) {
      object = DateUtils.formatDate((DateOnly) object, currentLocale)
    } else if (object instanceof Date) {
      object = DateUtils.formatDate((Date) object, currentLocale)
    } else if (Number.isAssignableFrom(object.getClass())) {
      //noinspection GroovyAssignabilityCheck
      object = NumberUtils.formatNumber((Number) object, currentLocale, true)
    } else if (EncodedTypeInterface.isAssignableFrom(object.getClass())) {
      object = object.toStringLocalized()
    } else if (DomainUtils.instance.isDomainEntity(object.getClass())) {
      object = TypeUtils.toShortString(object, true)
    } else if (object instanceof Collection) {
      def sb = new StringBuilder()
      for (o in object) {
        if (sb) {
          sb << ","
        }
        sb << o.toString()
      }
      object = sb.toString()
    }
    return object
  }


  /**
   * A list of string that indicate a piece of text might not be properly internationalized.
   */
  static protected List<String> possibleBadLabelSuffixes = ['.label', '.tooltip', '.title']

  /**
   * Verifies that the given label is looked up and found in the messages.properties.
   * @param label The displayed label text.
   */
  void checkLookedUpLabel(String label) {
    for (s in possibleBadLabelSuffixes) {
      assert !label.contains(s), "Column Header $label doesn't appear to be looked up in the .properties file."
    }
  }

  /**
   * Verifies that the given toolbar button works.
   * @param page The page this toolbar starts on.
   * @param id The ID for the button (view).
   * @param destinationPage The page this button should display.
   */
  void checkToolbarButton(String page, String id, String destinationPage) {
    go(page)
    waitForCompletion()

    clickButton(id)
    waitForCompletion()

    assert _tester.currentUrl.contains(destinationPage), "Toolbar button '${id}' on page '$page' does not redirect to '$destinationPage'."

  }

  /**
   * Finds teh domain record in the DB for the given parameter set.  Searches by key field.
   * @param params The parameters for the record to be found.
   */
  protected Object findRecord(Map params) {
    def keys = DomainUtils.instance.getKeyFields(_domain)
    //println "keys ${keys} = ${params[keys[0]]}, $params "
    return DomainUtils.instance.findDomainRecord(_domain, (String) params[keys[0]])
  }


  /**
   * Checks to see if the record's values matches the expected values.
   * @param record The record to check.
   * @param expectedValues The expected values, typically the Map of values defined for the test (e.g. recordParams).
   * @param ignoreDateFields If true, no checks are made on the date fields.
   */
  @SuppressWarnings("unused")
  protected void checkRecord(Object record, Map expectedValues, boolean ignoreDateFields = false) {
    log.trace('checkRecord: record {} expectedValue {}', record, expectedValues)

    for (String fieldName in expectedValues.keySet()) {
      def fieldDef = fieldDefinitions[fieldName]
      // Compare as strings to avoid issues with internal time supporting fractions of seconds.
      def value = formatExpectedValueForShow(record[fieldName])
      def expectedValue = formatExpectedValueForShow(expectedValues[fieldName])
      if (fieldName == 'title' && expectedValue == 'XYZZY') {
        expectedValue = ''
        log.warn('Altered title from "{}" to "{}" to avoid errors in checkRecord().', 'XYZZY', expectedValue)
      }

      log.trace('  Checking db field "{}" for expected value "{}". Found "{}"', fieldName, expectedValue, value)
      if (fieldName == 'password' && record instanceof User) {
        // Special case password checks to account for encryption.
        def matches = record.passwordMatches((String) expectedValue)
        assert matches, "Field $fieldName expected '$expectedValue', found (encrypted)'${value}'"
      } else if (fieldDef?.format instanceof ChildListFieldFormat) {
        checkChildRecordValues(fieldName, record, determineExpectedRowsAfterSave(fieldName))
      } else if (fieldDef?.format instanceof DomainRefListFieldFormat) {
        // Check the uuid of the list against the values in the expected array
        def uuidList = expectedValue?.tokenize(',')
        // Make sure each uuid is in the list from the DB.
        def dbList = record[fieldName]
        def uuidInDBList = dbList ? dbList*.uuid*.toString() : []
        def c1 = fieldDef.referenceType?.simpleName
        for (uuid in uuidList) {
          assert uuidInDBList.contains(uuid), "Field $fieldName expected '$uuid'($c1) in DB list '${uuidInDBList}'."
        }
      } else {
        def c1 = expectedValue?.getClass()?.simpleName
        def c2 = value?.getClass()?.simpleName
        assert value == expectedValue, "Field $fieldName expected '$expectedValue'($c1), found '${value}' ($c2)"
      }
    }
  }

  /**
   * Checks the inline grid displayed values against the expected values for the list.
   * @param fieldName The child element name (e.g. 'sampleChildren').  This is the name of the child field (collection).
   * @param expectedValues The expected value(s).  This is a List<Map>.
   * @param fieldDef The field definition.
   */
  @SuppressWarnings("SpellCheckingInspection")
  void checkInlineGridValues(String fieldName, Collection expectedValues, FieldDefinitionInterface fieldDef) {
    def childDomainClass = fieldDef.referenceType
    def fieldOrder = DomainUtils.instance.getStaticFieldOrder(childDomainClass)
    for (int row = 0; row < expectedValues?.size(); row++) {
      def rowData = expectedValues[row]
      for (int col = 0; col < fieldOrder.size(); col++) {
        def field = fieldOrder[col]
        if (rowData[field]) {
          //    cell { row, col -> $("div.webix_dtable", view_id: field).find('div.webix_column', column: "$col").find('div.webix_cell', row) }
          def cell = _tester.$("div.webix_dtable", view_id: fieldName).find('div.webix_column', column: "$col").find('div.webix_cell', row)
          checkListValue(cell, rowData[field], fieldName)
        }
      }
    }
  }

  /**
   * Checks the child record values against the input values.
   * @param fieldName The child element name (e.g. 'sampleChildren').  This is the name of the child field (collection).
   * @param record The parent record.
   * @param expectedValues The list of values.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  void checkChildRecordValues(String fieldName, Object record, Collection expectedValues) {
    def records = (Collection) record[fieldName]
    assert records?.size() == expectedValues?.size(), "Child list '$fieldName' expected ${expectedValues?.size()} rows, found ${records?.size()}"
    for (int i = 0; i < records.size(); i++) {
      def childRecord = records[i]
      def value = (Map) expectedValues[i]
      for (key in value.keySet()) {
        def msg = "Child list '$fieldName'[$i].${key} expected '${value[key]}', found '${childRecord[key]}'"
        assert childRecord[key] == value[key], msg
      }
    }
  }

  /**
   * Combines the minimal and record params values for child records into the list expected
   * after create/edit is finished.
   * @param fieldName The child element name (e.g. 'sampleChildren').  This is the name of the child field (collection).
   * @return The combined list of child records.
   */
  Collection determineExpectedRowsAfterSave(String fieldName) {
    def list = []

    if (!createMode) {
      if (_minimalParams[fieldName]) {
        list.addAll(_minimalParams[fieldName])
      }
    }
    if (_recordParams[fieldName]) {
      list.addAll(_recordParams[fieldName])
    }

    return list
  }

  /**
   * Defines the list of record created for class.  The inner list is a list of record IDs (Long).
   */
  Map<Class, List> recordsCreated = [:]

  /**
   * Creates a single domain record for the given set of parameters.
   * @param params The parameters.
   * @return The top-level record created.
   */
  def createRecord(Map params) {
    def res = null
    _domain.withTransaction {
      def object = _domain.newInstance()
      DomainBinder.build().bind(object, params)
      object.save()
      def list = recordsCreated[_domain]
      if (list == null) {
        list = []
        recordsCreated[_domain] = list
      }
      list << object.uuid
      log.trace("Created record {}({}) id = {} ", object, _domain, object.uuid)
      res = object
    }

    return res

  }

  /**
   * Clean up any records create by this test.
   */
  void cleanupCreatedRecords() {
    _domain.withTransaction {
      for (clazz in recordsCreated.keySet()) {
        def list = recordsCreated[clazz]
        for (id in list) {
          def record = clazz.findByUuid(id)
          if (record) {
            record.delete()
            log.trace("Deleted record {}({}) id = {}", record, clazz, record.uuid)
          }
        }
      }
    }
    recordsCreated = [:]
  }


  /**
   * Sends the browser to the given page (relative to the domain's controller top level).
   * @param subURI The page to visit (e.g. 'index' to visit 'controller/index').
   * @return The title of the page we are on.
   */
  void go(String subURI) {
    def language = currentLocale.toString()
    log.debug("go({},lang: {})", "$_uri$subURI", language)
    _tester.go("$_uri$subURI")
    if (Holders.configuration.testDelay) {
      sleep(Holders.configuration.testDelay)
    }
  }

  /**
   * Waits for any outstanding Ajax calls to finish.
   */
  void waitForCompletion() {
    _tester.waitFor() {
      def res = _tester.driver.executeScript("return tk.docState()")
      return res.size() == 0
    }
  }


  /**
   * Lookup a code from the message bundle.
   * @param code The message code (e.g. search.label).
   * @param args The optional arguments for the lookup.
   * @return The looked up value.
   */
  String lookup(String code, List args = null) {
    return GlobalUtils.lookup(code, currentLocale, args as Object[])
  }

  /**
   * Lookup a code from the message bundle, add the required flag (*).
   * @param code The message code (e.g. search.label).
   * @param args The optional arguments for the lookup.
   * @return The looked up value.
   */
  String lookupRequired(String code, List args = null) {
    return '*' + lookup(code, args)
  }

  protected static final List<String> missingPropertySuffixes = ['.label', '.text', '.tooltip', '.title']

  /**
   * Verifies that the given text has been localized and the text was found in the properties file.
   * @param text The text to check.  Checks for .label, .text, .tooltip or .title in the string.  If found, fails the test.
   * @param element The name of the element (used for error message).
   * @param page The page type (e.g. 'list') (used for error message).
   */
  void assertLocalized(String text, String element, String page) {
    for (suffix in missingPropertySuffixes) {
      assert !text.contains(suffix), "$element '$text' for '$page' page not found in .properties file.  The value contains '$suffix'."
    }
  }

  /**
   * Check for command-line overrides to run single test.
   */
  void checkDOptions() {
    def validOptions = ['list', 'show', 'create', 'edit']
    if (validOptions.contains(_dashDOption)) {
      log.warn("Testing ${_dashDOption} Only.  -D testOnly set.  ")
      _enableListTests = false
      _enableShowTests = false
      _enableCreateTests = false
      _enableEditTests = false

      def fieldName = "_enable${NameUtils.uppercaseFirstLetter((String) _dashDOption)}Tests"
      this[fieldName] = true

    }
  }

  /**
   * Click on the given panel to make sure it is displayed.
   * @param panel The panel name.
   */
  void clickPanel(String panel) {
    //println "panel = $panel"
    log.trace('Clicking panel "{}"', panel)
    _tester.$('div.webix_item_tab', button_id: "${panel}Body").click()
  }

  /**
   * Click on the given button.
   * @param id The button's (view) ID.
   */
  void clickButton(String id) {
    _tester.$('div.webix_el_button', view_id: id).find('button').click()
  }

  /**
   * Returns true if the given field format is normally displayed in a combobox.
   * @param fieldFormat The field format.
   * @return
   */
  boolean isComboBoxField(FieldFormatInterface fieldFormat) {
    return fieldFormat instanceof EncodedTypeFieldFormat ||
      fieldFormat instanceof DomainReferenceFieldFormat ||
      fieldFormat instanceof EnumFieldFormat
  }

}
