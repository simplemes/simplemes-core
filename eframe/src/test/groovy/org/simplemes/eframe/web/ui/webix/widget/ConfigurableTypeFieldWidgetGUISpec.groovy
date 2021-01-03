/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import sample.domain.RMA
import sample.page.RMACreatePage
import sample.page.RMAEditPage
import sample.page.RMAShowPage
import spock.lang.IgnoreIf

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings("unused")
class ConfigurableTypeFieldWidgetGUISpec extends BaseGUISpecification {

  static dirtyDomains = [RMA, FlexType]

  def "verify that the widget can create a record - no default value"() {
    given: 'a value for the drop-down'
    def flexType = DataGenerator.buildFlexType()

    when: 'a page is displayed with the widget'
    login()
    to RMACreatePage

    and: 'the combobox is set'
    setCombobox((Object) rmaType, flexType.uuid.toString())
    waitFor {
      FIELD1.displayed
    }

    and: 'the custom field value is set'
    FIELD1.input.value('VALUE1')

    and: 'the record is saved'
    rma.input.value('ABC')
    createButton.click()
    waitForNonZeroRecordCount(RMA)

    then: 'the record in the database is correct'
    def rma = null
    RMA.withTransaction {
      rma = RMA.findByRma('ABC')
      assert rma.rmaType == flexType
      assert rma.getFieldValue('FIELD1') == 'VALUE1'
      true
    }

    and: 'the show page displays the record'
    at RMAShowPage
    FIELD1.value == 'VALUE1'
  }

  def "verify that the widget supports a default value"() {
    given: 'a value for the drop-down'
    def flexType = DataGenerator.buildFlexType(defaultFlexType: true)

    when: 'a page is displayed with the widget'
    login()
    to RMACreatePage

    then: 'the combobox is set to default value'
    rmaType.input.value() == TypeUtils.toShortString(flexType, true)

    and: 'the configurable input field is displayed'
    FIELD1.displayed

    when: 'the custom field value is set'
    FIELD1.input.value('VALUE1')

    and: 'the record is saved'
    rma.input.value('ABC')
    createButton.click()
    waitForNonZeroRecordCount(RMA)

    then: 'the record in the database is correct'
    def rma = null
    RMA.withTransaction {
      rma = RMA.findByRma('ABC')
      assert rma.rmaType == flexType
      assert rma.getFieldValue('FIELD1') == 'VALUE1'
      true
    }
  }


  def "verify that the widget can change the setting and enter values to update a record"() {
    given: 'a value for the drop-down'
    DataGenerator.buildFlexType(flexType: 'FLEX1', fieldName: 'FIELD1')
    def flexType2 = DataGenerator.buildFlexType(flexType: 'FLEX2', fieldName: 'FIELD2')
    def flexType3 = DataGenerator.buildFlexType(flexType: 'FLEX3', fieldName: 'FIELD3')

    and: 'a domain to edit'
    def rma = null
    RMA.withTransaction {
      rma = new RMA(rma: 'ABC', rmaType: flexType2)
      rma.setFieldValue('FIELD2', 'VALUE2')
      rma.save()
    }

    when: 'a page is displayed with the widget'
    login()
    to RMAEditPage, rma

    then: 'the combobox is set to default value'
    rmaType.input.value() == TypeUtils.toShortString(flexType2, true)

    and: 'the configurable input field is displayed'
    FIELD2.input.value() == 'VALUE2'

    when: 'the combobox is changed'
    setCombobox((Object) rmaType, flexType3.uuid.toString())
    waitFor {
      FIELD3.displayed
    }

    and: 'the custom field value is set'
    FIELD3.input.value('VALUE3')

    and: 'the record is saved'
    status.input.value('abc')
    updateButton.click()
    waitForRecordChange(rma)

    then: 'the record in the database is correct'
    def rma2 = null
    RMA.withTransaction {
      rma2 = RMA.findByRma('ABC')
      assert rma2.rmaType == flexType3
      assert rma2.getFieldValue('FIELD3') == 'VALUE3'
      true
    }

    and: 'the show page displays the record'
    at RMAShowPage
    FIELD3.value == 'VALUE3'
  }

}
