package org.simplemes.eframe.custom.domain

import org.openqa.selenium.Keys
import org.simplemes.eframe.custom.domain.page.FlexTypeEditPage
import org.simplemes.eframe.custom.domain.page.FlexTypeShowPage
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.CRUDGUITester
import org.simplemes.eframe.test.DataGenerator
import spock.lang.Ignore
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the CRUD pages for the Alert.  Mainly tests the relationships between the GUI elements and the Domain class.
 */
@IgnoreIf({ !sys['geb.env'] })
class FlexTypeGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [FlexType]

  def "verify that the standard GUI definition pages work"() {
    expect: 'the constraints are enforced'
    CRUDGUITester.test {
      tester this
      domain FlexType
      recordParams flexType: 'ABC',
                   fields: [[fieldName: 'F1', fieldLabel: 'f1', maxLength: 23, sequence: 20]]
      minimalParams flexType: 'XYZ',
                    fields: [[fieldName: 'F2', fieldLabel: 'f2', maxLength: 33, sequence: 30]]
      listColumns 'flexType,category,title,defaultFlexType,fieldSummary'
      unlabeledFields 'fields'
    }
  }

  @Ignore('Enabled when CRUD is done')
  def "verify that the add row action defaults the sequence correctly"() {
    given: 'a domain record'
    def flexType = DataGenerator.buildFlexType(flexType: 'XYZ')

    when: 'the create page is displayed'
    login()
    to FlexTypeEditPage, flexType

    and: 'a new row is added to the inline grid'
    fields.addRowButton.click()

    and: 'skip the default sequence field'
    sendKey(Keys.TAB)

    and: 'a field name is set'
    sendKey('F2')

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(flexType)

    then: 'the value is shown'
    at FlexTypeShowPage

    and: 'the sequence is the expected value'
    FlexType.withTransaction {
      def record = FlexType.findByFlexType('XYZ')
      assert record.fields.size() == 2
      assert record.fields[0].sequence == 10
      assert record.fields[1].sequence == 20
      true
    }
  }


}
