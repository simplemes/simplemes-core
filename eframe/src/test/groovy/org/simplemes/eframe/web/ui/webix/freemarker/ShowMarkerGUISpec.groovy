/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent
import sample.page.AllFieldsDomainListPage
import sample.page.AllFieldsDomainShowPage
import sample.page.SampleParentCreatePage
import sample.page.SampleParentListPage
import sample.page.SampleParentShowPage
import spock.lang.IgnoreIf

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings("GroovyAssignabilityCheck")
class ShowMarkerGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain, SampleParent, FieldExtension, FieldGUIExtension]

  def "verify that basic page display works - tabbed panels"() {
    given: 'some domain records'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10,
             enabled: true, dueDate: new DateOnly(), dateTime: new Date()
    }

    when: 'the show page is displayed'
    login()
    to AllFieldsDomainShowPage, allFieldsDomain
    mainPanel.click()  // Make sure the main panel is displayed.

    then: 'the show page has the key field'
    name.label == lookupRequired('name.label')
    name.value == allFieldsDomain.name

    and: 'the main panel fields'
    titleField.label == lookup('title.label')
    titleField.value == allFieldsDomain.title

    // Not worried about the format of the field values in this test (TextFieldWidget handles that).
    qty.label == lookup('qty.label')
    qty.value
    count.label == lookup('count.label')
    count.value
    enabled.label == lookup('enabled.label')
    // enabled.value  // Ignore the checkbox value for this test.
    dueDate.label == lookup('dueDate.label')
    dueDate.value
    dateTime.label == lookup('dateTime.label')
    dateTime.value

    when: 'the details panel is shown'
    detailsPanel.click()

    then: 'the values on the details panel are correct'
    transientField.label == lookup('transientField.label')
    transientField.value == allFieldsDomain.transientField
  }

  /**
   * Builds a single SampleParent record.
   * @return The record.
   */
  SampleParent buildSampleParent() {
    SampleParent sampleParent = null
    SampleParent.withTransaction {
      sampleParent = new SampleParent(name: 'ABC', title: 'abc-title', notes: 'abc-notes').save()
    }
    return sampleParent
  }

  def "verify that basic page display works - no tabbed panels"() {
    given: 'some domain records'
    def sampleParent = buildSampleParent()

    when: 'the show page is displayed'
    login()
    to SampleParentShowPage, sampleParent

    then: 'the show page has the key field'
    name.label == lookupRequired('name.label')
    name.value == sampleParent.name

    and: 'the fields are correct'
    titleField.label == lookup('title.label')
    titleField.value == sampleParent.title
    notes.label == lookup('notes.label')
    notes.value == sampleParent.notes
  }

  def "verify that list toolbar button works"() {
    given: 'some domain records'
    def sampleParent = buildSampleParent()

    when: 'the show page is displayed'
    login()
    to SampleParentShowPage, sampleParent

    and: 'the list toolbar button is clicked'
    listButton.click()

    then: 'the list page is shown'
    at SampleParentListPage
  }

  def "verify that create toolbar button works"() {
    given: 'some domain records'
    def sampleParent = buildSampleParent()

    when: 'the show page is displayed'
    login()
    to SampleParentShowPage, sampleParent

    and: 'the toolbar button is clicked'
    createButton.click()

    then: 'the correct page is shown'
    at SampleParentCreatePage
  }

  def "verify that edit toolbar button works"() {
    given: 'some domain records'
    def sampleParent = buildSampleParent()

    when: 'the show page is displayed'
    login()
    to SampleParentShowPage, sampleParent

    and: 'the toolbar button is clicked'
    editButton.click()

    then: 'the correct page is shown'
    currentUrl.contains('/sampleParent/edit')
    currentUrl.contains("/${sampleParent.uuid}")
  }

  def "verify that tab selection is retained when page is re-displayed"() {
    given: 'some domain records'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10,
             enabled: true, dueDate: new DateOnly(), dateTime: new Date()
    }

    when: 'the show page is displayed'
    login()
    to AllFieldsDomainShowPage, allFieldsDomain

    and: 'the details panel is displayed'
    detailsPanel.click()

    and: 'the page is redisplayed'
    to AllFieldsDomainShowPage, allFieldsDomain

    then: 'the details tab fields are visible'
    transientField.label == lookup('transientField.label')
    transientField.value == allFieldsDomain.transientField
  }

  def "verify that the delete dialog works"() {
    given: 'some domain records'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC<script>'
    }

    when: 'the show page is displayed'
    login()
    to AllFieldsDomainShowPage, allFieldsDomain

    and: 'the delete is attempted'
    moreMenuButton.click()
    waitFor { deleteButton.displayed }

    and: 'the delete button is pressed'
    deleteButton.click()
    waitFor { dialog0.exists }

    then: 'the dialog has the correct values'
    dialog0.title == lookup('delete.confirm.title')
    def shortString = TypeUtils.toShortString(allFieldsDomain) // The value is HTML escaped, but the GEB text() method reverts it back to the HTML.
    dialog0.templateContent.text() == lookup('delete.confirm.message', null, 'AllFieldsDomain', shortString)

    when: 'the Ok button is clicked'
    dialog0.okButton.click()
    waitFor { !dialog0.exists }


    then: 'the list page is displayed'
    at AllFieldsDomainListPage

    and: 'the record is deleted'
    AllFieldsDomain.withTransaction {
      assert AllFieldsDomain.count() == 0
      true
    }

    and: 'the info message is displayed'
    def domainName = GlobalUtils.lookup("${NameUtils.lowercaseFirstLetter(AllFieldsDomain.simpleName)}.label")
    def s = TypeUtils.toShortString(allFieldsDomain)
    def msg = GlobalUtils.lookup('deleted.message', null, domainName, s)

    messages.text() == msg
    messages.find('div').classes().contains('info-message')
  }

  def "verify that the delete fails gracefully when the record cannot be found"() {
    given: 'some domain records'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
    }

    when: 'the show page is displayed'
    login()
    to AllFieldsDomainShowPage, allFieldsDomain

    and: 'the record is deleted by another user'
    AllFieldsDomain.withTransaction {
      AllFieldsDomain.findByName(allFieldsDomain.name).delete()
    }

    and: 'the delete is attempted'
    moreMenuButton.click()
    waitFor { deleteButton.displayed }

    and: 'the delete button is pressed'
    deleteButton.click()
    waitFor { dialog0.exists }

    and: 'the Ok button is clicked'
    dialog0.okButton.click()
    waitFor { !dialog0.exists }

    then: 'the list page is displayed'
    at AllFieldsDomainListPage

    and: 'the error message is displayed'
    def msg = GlobalUtils.lookup('error.105.message', null, allFieldsDomain.uuid)
    messages.text() == msg
    messages.find('div').classes().contains('error-message')
  }

  def "verify that the delete dialog can be canceled"() {
    given: 'some domain records'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
    }

    when: 'the show page is displayed'
    login()
    to AllFieldsDomainShowPage, allFieldsDomain

    and: 'the delete is attempted'
    moreMenuButton.click()
    waitFor { deleteButton.displayed }

    and: 'the delete button is pressed'
    deleteButton.click()
    waitFor { dialog0.exists }

    and: 'the cancel button is clicked'
    dialog0.cancelButton.click()
    waitFor { !dialog0.exists }

    then: 'the show page is still displayed'
    at AllFieldsDomainShowPage

    and: 'the record is not deleted'
    AllFieldsDomain.withTransaction {
      assert AllFieldsDomain.count() == 1
      true
    }
  }

  def "verify that basic page display works - custom field added"() {
    given: 'a custom field'
    FieldExtension.withTransaction {
      new FieldExtension(fieldName: 'custom1', domainClassName: SampleParent.name,
                         fieldFormat: BigDecimalFieldFormat.instance).save()
      def fg = new FieldGUIExtension(domainName: SampleParent.name)
      fg.adjustments = [new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')]
      fg.save()
    }

    and: 'a domain record'
    SampleParent sampleParent = null
    SampleParent.withTransaction {
      sampleParent = new SampleParent(name: 'ABC', title: 'abc-title', notes: 'abc-notes')
      sampleParent.setFieldValue('custom1', 1.2)
      sampleParent.save()
    }

    when: 'the show page is displayed'
    login()
    to SampleParentShowPage, sampleParent

    then: 'the show page has the custom field'
    getFieldLabel('custom1') == 'custom1'
    getReadonlyFieldValue('custom1') == NumberUtils.formatNumber(1.2)
  }


}
