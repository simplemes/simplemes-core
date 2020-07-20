/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.openqa.selenium.Keys
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.web.ui.UIDefaults
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent
import sample.page.AllFieldsDomainCreatePage
import sample.page.AllFieldsDomainListPage
import sample.page.SampleParentListPage
import spock.lang.IgnoreIf

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings("GroovyAssignabilityCheck")
class DefinitionListWidgetGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, AllFieldsDomain]

  def "verify that default sorting works"() {
    given: 'some domain records'
    def records = DataGenerator.generate {
      domain AllFieldsDomain
      count UIDefaults.PAGE_SIZE * 3
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    } as List<AllFieldsDomain>

    when: 'the list page is displayed'
    login()
    to AllFieldsDomainListPage

    then: 'the page is sorted in default order'
    waitFor {
      def cell0 = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
      cell0.text() == records[0].name
    }

    and: 'the default sort order is marked in the column header'
    allFieldsDomainGrid.sortAsc.text() == lookup('name.label', currentLocale)

    and: 'make sure the dumpElement and reportFailure works'
    def cell0 = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
    dumpElement(cell0)
    reportFailure('not-a-failure')
  }

  def "verify that descending sort order works"() {
    given: 'some domain records'
    def records = DataGenerator.generate {
      domain AllFieldsDomain
      count UIDefaults.PAGE_SIZE * 3
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    } as List<AllFieldsDomain>

    when: 'the list page is displayed'
    login()
    to AllFieldsDomainListPage

    and: 'the list is re-sorted on first column for descending order'
    allFieldsDomainGrid.headers[getColumnIndex(AllFieldsDomain, 'name')].click()
    waitForCompletion()

    then: 'the page is sorted in descending order'
    def cell0 = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
    cell0.text() == records[29].name

    and: 'the descending sort order is marked in the column header'
    allFieldsDomainGrid.sortDesc.text() == lookup('name.label', currentLocale)
  }


  def "verify that the pager buttons are correct and work"() {
    given: 'some domain records'
    def records = DataGenerator.generate {
      domain AllFieldsDomain
      count UIDefaults.PAGE_SIZE * 3
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    } as List<AllFieldsDomain>

    when: 'the list page is displayed'
    login()
    to AllFieldsDomainListPage

    then: 'the right number of pages are shown in the pager - number of pages plus 2 for extra buttons on the end'
    def pagerButtons = allFieldsDomainGrid.pagerButtons
    pagerButtons.size() == 2 + NumberUtils.divideRoundingUp(records.size(), UIDefaults.PAGE_SIZE)

    when: 'the second pager button is clicked'
    pagerButtons[2].click()
    waitForCompletion()

    then: 'the second page is displayed'
    def cell0 = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
    cell0.text() == records[UIDefaults.PAGE_SIZE].name
  }

  def "verify that the pager buttons work with non-default sort order"() {
    given: 'some domain records'
    def records = DataGenerator.generate {
      domain AllFieldsDomain
      count UIDefaults.PAGE_SIZE * 3
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    } as List<AllFieldsDomain>

    when: 'the list page is displayed'
    login()
    to AllFieldsDomainListPage

    and: 'the list is re-sorted on first column for descending order'
    allFieldsDomainGrid.headers[getColumnIndex(AllFieldsDomain, 'name')].click()
    waitForCompletion()

    and: 'the second pager button is clicked'
    allFieldsDomainGrid.pagerButtons[2].click()
    waitForCompletion()

    then: 'the second page is displayed'
    def cell0 = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
    cell0.text() == records[(UIDefaults.PAGE_SIZE * 2) - 1].name
  }

  def "verify that dangerous HTML code is escaped correctly"() {
    given: 'some domain records'
    def records = DataGenerator.generate {
      domain AllFieldsDomain
      count 1
      values name: 'ABC-$i', title: '<script>abc</script>-$r'
    } as List<AllFieldsDomain>

    when: 'the list page is displayed'
    login()
    to AllFieldsDomainListPage

    then: 'the column displays the value escaped'
    def cell0 = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'title'))
    cell0.text() == records[0].title
  }

  // The moveToElement() and drag does not work with Chrome.
  @IgnoreIf({ System.getProperty("geb.env")?.contains("chrome") })
  def "verify that the resized column width is used on the next display"() {
    given: 'some domain records'
    DataGenerator.generate {
      domain AllFieldsDomain
      count 1
      values name: 'ABC-$i', title: 'abc-$r'
    } as List<AllFieldsDomain>

    when: 'the list page is displayed'
    login()
    to AllFieldsDomainListPage
    def headers = allFieldsDomainGrid.headers
    def origWidth = headers[1].width

    and: 'the column is resized by +50 pixels'
    interact {
      def offset = -5 - (headers[2].width / 2) as int
      moveToElement(headers[2], offset, 10)
      clickAndHold()
      moveByOffset(50, 0)
      release()
    }
    waitFor {
      nonZeroRecordCount(UserPreference)
    }

    headers = allFieldsDomainGrid.headers
    def newWidth = headers[1].width

    then: 'the column is resized by roughly the right amount'
    Math.abs(newWidth - origWidth - 50) < 5

    when: 'the page is re-displayed'
    to AllFieldsDomainListPage

    then: 'the new column width is used'
    def headers2 = allFieldsDomainGrid.headers
    def finalWidth = headers2[1].width
    Math.abs(finalWidth - origWidth - 50) < 5
  }

  def "verify that the column sorting works with simple case"() {
    given: 'some domain records'
    def records = DataGenerator.generate {
      domain AllFieldsDomain
      count 4
      values name: 'ABC-$i', title: 'abc-$r'
    } as List<AllFieldsDomain>

    when: 'the list page is displayed'
    login()
    to AllFieldsDomainListPage
    def headers = allFieldsDomainGrid.headers

    and: 'the list is sorted by title'
    interact {
      moveToElement(headers[1])
      click()
    }

    // Wait for the records for the column size to appear in DB
    waitFor {
      nonZeroRecordCount(UserPreference)
    }

    then: 'header is flagged as the sort column'
    allFieldsDomainGrid.sortAsc.text() == lookup('title.label')

    and: 'actual data is sorted'
    def cell0 = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
    cell0.text() == records[3].name

    when: 'the page is re-displayed from the user preferences'
    to AllFieldsDomainListPage

    then: 'header is flagged as the sort column'
    allFieldsDomainGrid.sortAsc.text() == lookup('title.label')

    and: 'actual data is sorted'
    def cell1 = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
    cell1.text() == records[3].name
  }

  def "verify that the simple domain references will display the key field value"() {
    given: 'some domain records'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC'
    }
    DataGenerator.generate {
      domain SampleParent
      values allFieldsDomain: allFieldsDomain
    }

    when: 'the list page is displayed'
    login()
    to SampleParentListPage

    then: 'domain reference column shows just the key field'
    def cell0 = sampleParentList.cell(0, getColumnIndex(SampleParent, 'allFieldsDomain'))
    cell0.text() == allFieldsDomain.name

    and: 'the rows are valid'
    sampleParentList.rows(0).size() == 1
  }

  def "verify that search will use search helper for filtering - search in DB mode - no real search engine"() {
    given: 'some domain records'
    def recordsA = DataGenerator.generate {
      domain AllFieldsDomain
      count UIDefaults.PAGE_SIZE * 2
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    } as List<AllFieldsDomain>

    def recordsX = DataGenerator.generate {
      domain AllFieldsDomain
      count UIDefaults.PAGE_SIZE * 2
      values name: 'XYZ-$i', title: 'xyz-$r', qty: 1.0, count: 10, enabled: true
    } as List<AllFieldsDomain>

    when: 'the list page is displayed'
    login()
    to AllFieldsDomainListPage
    waitFor {
      def cell0 = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
      cell0.text() == recordsA[0].name
    }

    then: 'the page is sorted in default order'
    def cell0 = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
    cell0.text() == recordsA[0].name

    when: 'the results are filtered'
    searchField.input.value('XYZ')
    standardGUISleep()
    sendKey(Keys.ENTER)
    waitFor {
      def cell = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
      cell.text() == recordsX[0].name
    }

    then: 'the filtered fields are shown'
    def cell0X = allFieldsDomainGrid.cell(0, getColumnIndex(AllFieldsDomain, 'name'))
    cell0X.text() == recordsX[0].name
  }

  def "verify that the create button works"() {
    when: 'the list page is displayed'
    login()
    to AllFieldsDomainListPage

    and: 'the create button is clicked'
    createButton.click()

    then: 'the create page is displayed'
    at AllFieldsDomainCreatePage
  }

}
