package org.simplemes.eframe.web.ui.webix.widget


import org.openqa.selenium.Keys
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import sample.domain.Order
import sample.domain.SampleChild
import sample.domain.SampleParent
import sample.page.SampleParentCreatePage
import sample.page.SampleParentEditPage
import sample.page.SampleParentShowPage
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings("GroovyAssignabilityCheck")
class GridWidgetGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, AllFieldsDomain, Order]

  def "verify that a readOnly grid is displayed with the correct values - show mode"() {
    given: 'a domain object for a foreign reference'
    def (Order order) = DataGenerator.generate {
      domain Order
    }

    and: 'some test dates'
    def dueDate = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def dateTime = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)

    and: 'a domain object to show'
    def sampleParent = null
    SampleParent.withTransaction {
      sampleParent = new SampleParent(name: 'ABC')
      def sampleChild = new SampleChild(key: 'C1', sequence: 300, title: 'title1', qty: 12.2, enabled: true,
                                        dueDate: dueDate, dateTime: dateTime, format: DomainReferenceFieldFormat.instance,
                                        reportTimeInterval: ReportTimeIntervalEnum.LAST_YEAR, order: order)
      sampleParent.addToSampleChildren(sampleChild)
      sampleParent.save()
    }

    when: 'the create page is displayed'
    login()
    to SampleParentShowPage, sampleParent

    then: 'the values in the grid are correct'
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'key')).text() == 'C1'
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'sequence')).text() == '300'
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'title')).text() == 'title1'
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'qty')).text().startsWith('12.2')
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'dueDate')).text() == DateUtils.formatDate(dueDate)
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'dateTime')).text() == DateUtils.formatDate(dateTime)
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'format')).text() == DomainReferenceFieldFormat.instance.toStringLocalized()
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'reportTimeInterval')).text() == ReportTimeIntervalEnum.LAST_YEAR.toStringLocalized()
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'order')).text() == order.order
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'enabled')).find('input').@checked == 'true'
  }

  def "verify that all field types in a inline grid can be saved - create mode"() {
    given: 'a domain object for a foreign reference'
    def (Order order) = DataGenerator.generate {
      domain Order
    }

    and: 'some test dates'
    def dueDate = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def dateTime = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)

    when: 'the create page is displayed'
    login()
    to SampleParentCreatePage

    and: 'the key field is set'
    name.input.value('XYZ')

    and: 'a new row is added to the inline grid'
    sampleChildren.addRowButton.click()

    and: 'values are entered'
    sendKey('C1')   // Key field
    sendKey(Keys.TAB)

    sendKey('11')   // Sequence field
    sendKey(Keys.TAB)

    sendKey('TitleC1')   // Title field
    sendKey(Keys.TAB)

    sendKey('12.2')   // Quantity field
    sendKey(Keys.TAB)

    sendKey(Keys.SPACE)   // Enabled boolean field
    sendKey(Keys.TAB)

    sendKey(DateUtils.formatDate(dueDate))   // Due Date field
    sendKey(Keys.TAB)

    sendKey(DateUtils.formatDate(dateTime))   // DateTime field
    sendKey(Keys.TAB)

    sendKey(EnumFieldFormat.instance.toStringLocalized())   // Format field
    sendKey(Keys.TAB)

    sendKey(ReportTimeIntervalEnum.YESTERDAY.toStringLocalized())   // report Time Interval field
    sendKey(Keys.TAB)

    sendKey(order.order)   // Order field
    sendKey(Keys.TAB)

    and: 'the record is saved'
    createButton.click()
    waitForNonZeroRecordCount(SampleParent)

    then: 'the value is shown'
    at SampleParentShowPage

    and: 'the child record is correct'
    SampleParent.withTransaction {
      def record = SampleParent.findByName('XYZ')
      assert record.sampleChildren.size() == 1
      assert record.sampleChildren[0].key == 'C1'
      assert record.sampleChildren[0].sequence == 11
      assert record.sampleChildren[0].title == 'TitleC1'
      assert record.sampleChildren[0].qty == 12.2
      assert record.sampleChildren[0].enabled
      assert record.sampleChildren[0].dueDate == dueDate
      assert record.sampleChildren[0].dateTime == dateTime
      assert record.sampleChildren[0].format == EnumFieldFormat.instance
      assert record.sampleChildren[0].reportTimeInterval == ReportTimeIntervalEnum.YESTERDAY
      assert record.sampleChildren[0].order == order
      true
    }
  }

  def "verify that the inline grid can be saved with a row delete - edit mode"() {
    given: 'a domain object to edit'
    def sampleParent = null
    SampleParent.withTransaction {
      sampleParent = new SampleParent(name: 'ABC')
      sampleParent.addToSampleChildren(new SampleChild(key: 'C1', sequence: 100))
      sampleParent.addToSampleChildren(new SampleChild(key: 'C2', sequence: 200))
      sampleParent.save()
    }

    when: 'the edit page is displayed'
    login()
    to SampleParentEditPage, sampleParent

    and: 'the first row is changed'
    sampleChildren.cell(0, 0).click()
    sendKey('NEW1')
    sendKey(Keys.TAB)

    and: 'a row is deleted'
    sampleChildren.cell(1, 0).click()
    sampleChildren.removeRowButton.click()

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(sampleParent)

    then: 'the value is shown'
    at SampleParentShowPage

    and: 'the child record is correct'
    SampleParent.withTransaction {
      def record = SampleParent.findByName('ABC')
      assert record.sampleChildren.size() == 1
      assert record.sampleChildren[0].key == 'NEW1'
      true
    }
  }

  def "verify that the add row hot key works"() {
    when: 'the create page is displayed'
    login()
    to SampleParentCreatePage

    and: 'the key field is set'
    name.input.value('XYZ')

    and: 'a new row is added to the inline grid'
    sampleChildren.addRowButton.click()

    and: 'the first row required fields are entered'
    sendKey('C1')   // Key field
    sendKey(Keys.TAB)

    and: 'a second row is added with the hot key'
    sendKey(Keys.ESCAPE)
    interact {
      // We can't use the Keys.chord() for some reason.
      keyDown Keys.ALT
      sendKeys('a')
      keyUp Keys.ALT
    }

    and: 'the second row required fields are entered'
    sendKey('C2')   // Key field
    sendKey(Keys.TAB)

    and: 'the record is saved'
    createButton.click()
    waitForNonZeroRecordCount(SampleParent)

    then: 'the value is shown'
    at SampleParentShowPage

    and: 'the child record is correct - the default sequence uses the new row default logic'
    SampleParent.withTransaction {
      def record = SampleParent.findByName('XYZ')
      assert record.sampleChildren.size() == 2
      assert record.sampleChildren[0].key == 'C1'
      assert record.sampleChildren[0].sequence == 10
      assert record.sampleChildren[1].key == 'C2'
      assert record.sampleChildren[1].sequence == 20
      true
    }
  }

  def "verify that the combobox popup has works"() {
    given: 'a domain object for a foreign reference'
    def (Order order) = DataGenerator.generate {
      domain Order
    }

    when: 'the create page is displayed'
    login()
    to SampleParentCreatePage

    and: 'the key field is set'
    name.input.value('XYZ')

    and: 'a new row is added to the inline grid'
    sampleChildren.addRowButton.click()

    and: 'the required field is filled in'
    sendKey('C1')
    sendKey(Keys.TAB)

    and: 'the format field is clicked to open the combobox list popup - the list is correct'
    assert sampleChildren.headers[getColumnIndex(SampleChild, 'format')].text() == lookup('format.label')
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'format')).click()
    def format = EncodedTypeFieldFormat.instance
    waitFor {
      getComboListItem(format.id).displayed
    }
    // Make sure the list is wide enough and the display values are used
    assert $('div.webix_popup').has('div.webix_list_item', webix_l_id: format.id).width > 200
    assert getComboListItem(format.id).text() == format.toStringLocalized()
    getComboListItem(format.id).click()
    sendKey(Keys.TAB)


    and: 'the report enum field is clicked to open the combobox list popup - the list is correct'
    assert sampleChildren.headers[getColumnIndex(SampleChild, 'reportTimeInterval')].text() == lookup('reportTimeInterval.label')
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'reportTimeInterval')).click()
    def intervalEnum = ReportTimeIntervalEnum.LAST_7_DAYS
    waitFor {
      getComboListItem(intervalEnum.toString()).displayed
    }
    assert getComboListItem(intervalEnum.toString()).text() == intervalEnum.toStringLocalized()
    getComboListItem(intervalEnum.toString()).click()
    sendKey(Keys.TAB)

    and: 'the dome ref - order - field is clicked to open the combobox list popup - the list is correct'
    assert sampleChildren.headers[getColumnIndex(SampleChild, 'order')].text() == lookup('order.label')
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'order')).click()
    waitFor {
      getComboListItem(order.id.toString()).displayed
    }
    assert getComboListItem(order.id.toString()).text() == order.order
    getComboListItem(order.id.toString()).click()
    sendKey(Keys.TAB)

    and: 'the record is saved'
    createButton.click()
    waitForNonZeroRecordCount(SampleParent)

    then: 'the value is shown'
    at SampleParentShowPage

    and: 'the child record is correct - the default sequence uses the new row default logic'
    SampleParent.withTransaction {
      def record = SampleParent.findByName('XYZ')
      assert record.sampleChildren.size() == 1
      assert record.sampleChildren[0].key == 'C1'
      assert record.sampleChildren[0].format == EncodedTypeFieldFormat.instance
      assert record.sampleChildren[0].reportTimeInterval == ReportTimeIntervalEnum.LAST_7_DAYS
      true
    }
  }

  def "verify that the resized column width is used on the next display - create page"() {
    when: 'the page is displayed'
    login()
    to SampleParentCreatePage
    def headers = sampleChildren.headers
    def origWidth = headers[1].width

    and: 'the column is resized by +50 pixels'
    interact {
      def offset = -5 - (headers[2].width / 2) as int
      moveToElement(headers[2], offset, 10)
      clickAndHold()
      moveByOffset(50, 0)
      release()
    }
    waitForNonZeroRecordCount(UserPreference)

    headers = sampleChildren.headers
    def newWidth = headers[1].width

    then: 'the column is resized by roughly the right amount'
    Math.abs(newWidth - origWidth - 50) < 5

    when: 'the page is re-displayed'
    to SampleParentCreatePage

    then: 'the new column width is used'
    def headers2 = sampleChildren.headers
    def finalWidth = headers2[1].width
    Math.abs(finalWidth - origWidth - 50) < 5
  }

  def "verify that the column sorting works with simple case - edit mode"() {
    given: 'a domain object to edit'
    def sampleParent = null
    SampleParent.withTransaction {
      sampleParent = new SampleParent(name: 'ABC')
      sampleParent.addToSampleChildren(new SampleChild(key: 'C1', sequence: 300))
      sampleParent.addToSampleChildren(new SampleChild(key: 'C2', sequence: 200))
      sampleParent.addToSampleChildren(new SampleChild(key: 'C3', sequence: 100))
      sampleParent.save()
    }

    when: 'the list page is displayed'
    login()
    to SampleParentEditPage, sampleParent
    def headers = sampleChildren.headers

    and: 'the list is sorted by sequence'
    interact {
      moveToElement(headers[1])
      click()
    }

    // Wait for the records for the column sort preference to appear in DB
    waitForNonZeroRecordCount(UserPreference)

    then: 'header is flagged as the sort column'
    sampleChildren.sortAsc.text() == lookup('sequence.label')

    and: 'actual data is sorted'
    def cellA = sampleChildren.cell(0, getColumnIndex(SampleChild, 'key'))
    cellA.text() == 'C3'

    when: 'the page is re-displayed from the user preferences'
    to SampleParentEditPage, sampleParent

    then: 'header is flagged as the sort column'
    sampleChildren.sortAsc.text() == lookup('sequence.label')

    and: 'actual data is sorted'
    def cellB = sampleChildren.cell(0, getColumnIndex(SampleChild, 'key'))
    cellB.text() == 'C3'
  }

  def "verify that keyboard tab navigation and focus work"() {
    given: 'a domain object to edit'
    def sampleParent = null
    SampleParent.withTransaction {
      sampleParent = new SampleParent(name: 'ABC')
      sampleParent.addToSampleChildren(new SampleChild(key: 'C1', sequence: 300))
      sampleParent.addToSampleChildren(new SampleChild(key: 'C2', sequence: 200))
      sampleParent.addToSampleChildren(new SampleChild(key: 'C3', sequence: 100))
      sampleParent.save()
    }

    when: 'the list page is displayed'
    login()
    to SampleParentEditPage, sampleParent

    then: 'the first row is selected'
    sampleChildren.isSelected(0)
    !sampleChildren.isSelected(1)

    when: 'the last cell is edited on the first row'
    sampleChildren.cell(0, getColumnIndex(SampleChild, 'order')).click()

    and: 'we tab to the next element - focus should be on the second row'
    sendKey(Keys.TAB)

    and: 'the key field is changed'
    sendKey('NEW2')
    sendKey(Keys.TAB)

    and: 'the sequence field is changed - proves the tab moves between cells'
    sendKey('999')
    sendKey(Keys.TAB)

    then: 'the key field is changed - means the focus was in the right field'
    sampleChildren.cell(1, getColumnIndex(SampleChild, 'key')).text() == 'NEW2'
    sampleChildren.cell(1, getColumnIndex(SampleChild, 'sequence')).text() == '999'

    when: 'we edit the last cell on the last row'
    sampleChildren.cell(2, getColumnIndex(SampleChild, 'order')).click()

    and: 'we tab to the next element'
    // Try up to 10 times to tab to the next element.
    // Some browsers take a few extra tabs (firefox needs 7!).
    for (int i = 0; i < 10; i++) {
      sendKey(Keys.TAB)
      //sleep(100)
      if (sampleChildren.addRowButton.focused) {
        break
      }
    }

    then: 'the add button is focused'
    sampleChildren.addRowButton.focused

  }

  def "verify that validation failure on child record is handled gracefully - create mode"() {
    when: 'the create page is displayed'
    login()
    to SampleParentCreatePage

    and: 'the key field is set'
    name.input.value('XYZ')

    and: 'a new row is added to the inline grid'
    sampleChildren.addRowButton.click()

    and: 'no value is entered'
    sendKey(Keys.TAB)

    and: 'the record is saved'
    createButton.click()

    then: 'the create page is still displayed'
    at SampleParentCreatePage

    and: 'an error is displayed'
    //blank={0} is missing ({2})
    messages.text().contains(lookup('blank', null, lookup('key.label'), '', 'SampleChild'))
  }

}
