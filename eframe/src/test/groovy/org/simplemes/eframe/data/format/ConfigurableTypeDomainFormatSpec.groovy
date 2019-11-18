package org.simplemes.eframe.data.format

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.domain.FlexField
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.test.BaseSpecification
import sample.domain.RMA

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ConfigurableTypeDomainFormatSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, HIBERNATE]

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    ConfigurableTypeDomainFormat.instance.id == ConfigurableTypeDomainFormat.ID
    ConfigurableTypeDomainFormat.instance.toString() == 'Configurable Type'
    BasicFieldFormat.coreValues.contains(ConfigurableTypeDomainFormat)
  }

  @Rollback
  def "verify that Configurable Type fields can be saved"() {
    given: 'a flex type with a field'
    def flexType = new FlexType(flexType: 'XYZ')
    flexType.addToFields(new FlexField(flexType: flexType, fieldName: 'Field1'))
    flexType.save()

    when: 'a record with the Configurable Type is saved'
    def rma = new RMA(rma: 'ABC', rmaType: flexType)
    // TODO: Change to rma.setRmaTypeValue('Field1','XYZZY')
    ExtensibleFieldHelper.instance.setFieldValue(rma, 'Field1', 'XYZZY')
    rma.save()

    then: 'the fields values are pulled from storage'
    // TODO: Change to rma.getRmaTypeValue('Field1')
    ExtensibleFieldHelper.instance.getFieldValue(rma, 'Field1') == 'XYZZY'
  }

  @Rollback
  def "verify that getCurrentFields returns the list of fields for a flex type"() {
    given: 'a flex type with a field'
    def flexType = new FlexType(flexType: 'XYZ')
    flexType.addToFields(new FlexField(flexType: flexType, fieldName: 'Field1'))
    flexType.save()

    when: 'the current fields are returned'
    def rma = new RMA(rma: 'ABC', rmaType: flexType)
    def fields = ConfigurableTypeDomainFormat.instance.getCurrentFields(rma, 'rmaType')

    then: 'the fields are correct'
    fields.size() == 1
    fields[0].name == 'Field1'
    fields[0].format == StringFieldFormat.instance
    fields[0].configTypeFieldName == 'rmaType'
  }

  @Rollback
  def "verify that getCurrentFields handles null field value gracefully"() {
    when: 'the current fields are returned'
    def rma = new RMA(rma: 'ABC')
    def fields = ConfigurableTypeDomainFormat.instance.getCurrentFields(rma, 'rmaType')

    then: 'the fields are correct'
    fields.size() == 0
  }

  @Rollback
  def "verify that getCurrentFields handles null object value gracefully"() {
    when: 'the current fields are returned'
    def fields = ConfigurableTypeDomainFormat.instance.getCurrentFields(null, 'rmaType')

    then: 'the fields are correct'
    fields.size() == 0
  }
}
