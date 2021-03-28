package org.simplemes.eframe.client.web

import org.openqa.selenium.Keys
import org.simplemes.eframe.custom.domain.FlexField
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.custom.domain.page.FlexTypeCrudPage
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the base CRUD components.  Uses real production pages for the tests (e.g. FlexType).
 */
@IgnoreIf({ !sys['geb.env'] })
class CrudTableGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [FlexType]


  def "verify that the crud list is correct"() {
    given: 'some domain records'
    DataGenerator.generate {
      domain FlexType
      count 20
      values flexType: 'XYZ$r', fields: [new FlexField(sequence: 1, fieldName: 'F1_$i', fieldLabel: 'f1-$i')]
    } as List<FlexType>

    when: 'the crud page is displayed'
    login()
    to FlexTypeCrudPage

    then: 'the cells are correct'
    crudList.cell(0, 0).text() == 'XYZ001'
    crudList.cell(0, 1).text() == 'BASIC'
    crudList.cell(0, 2).text() == 'abc001'
    crudList.cell(0, 3).text() == 'F1_020'
    crudList.cell(1, 0).text() == 'XYZ002'

    and: 'the headers are correct'
    def headers = crudList.headers as List
    headers[0].text() == lookup("label.flexType")
    headers[1].text() == lookup("label.category")
    headers[2].text() == lookup("label.title")
    headers[3].text() == lookup("label.fields")
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that the crud list can be sorted and paged"() {
    given: 'some domain records'
    DataGenerator.generate {
      domain FlexType
      count 20
      values flexType: 'XYZ$r', fields: [new FlexField(sequence: 1, fieldName: 'F1_$i', fieldLabel: 'f1-$i')]
    } as List<FlexType>

    when: 'the crud page is displayed'
    login()
    to FlexTypeCrudPage

    then: 'the cells are sorted by the default order'
    crudList.cell(0, 0).text() == 'XYZ001'

    when: 'the table is sorted ascending on flex type'
    crudList.headers[0].click()
    waitForAjaxCompletion()

    then: 'list is sorted'
    crudList.cell(0, 0).text() == 'XYZ001'

    when: 'the table is sorted descending on flex type'
    crudList.headers[0].click()
    waitForAjaxCompletion()

    then: 'list is sorted'
    crudList.cell(0, 0).text() == 'XYZ020'

    when: 'the second page is displayed'
    crudList.pagerButtons[1].click()
    waitForAjaxCompletion()

    then: 'the correct data is shown'
    crudList.cell(0, 0).text() == 'XYZ010'
  }

  def "verify that the crud list can be searched"() {
    given: 'some domain records'
    DataGenerator.generate {
      domain FlexType
      count 20
      values flexType: 'XYZ$r', fields: [new FlexField(sequence: 1, fieldName: 'F1_$i', fieldLabel: 'f1-$i')]
    } as List<FlexType>

    when: 'the crud page is displayed'
    login()
    to FlexTypeCrudPage

    and: 'the search filter value is entered'
    crudList.searchField.value('009')
    sendKey(Keys.TAB)
    waitForAjaxCompletion()

    then: 'the list is filtered'
    crudList.rows.size() == 1

    and: 'the valid row is shown'
    crudList.cell(0, 0).text() == 'XYZ009'
  }

  def "verify that dangerous HTML code is escaped correctly"() {
    given: 'some domain records'
    def (FlexType flexType) = DataGenerator.generate {
      domain FlexType
      values flexType: 'XYZ$r', title: '<script>abc</script>-$r', fields: [new FlexField(sequence: 1, fieldName: 'F1_$i', fieldLabel: 'f1-$i')]
    }

    when: 'the list page is displayed'
    login()
    to FlexTypeCrudPage

    then: 'the column displays the value escaped'
    crudList.cell(0, 2).text() == flexType.title
  }


  // TODO: Add rest of tests
  /*
    Create button works
    Edit button works.
    Delete action works.
    More actions works.
    Add to FlexTypeGUISpec:
      Verify that column label localizations.
      Verify that FlexTypeService is used. - Content check.
   */

}
