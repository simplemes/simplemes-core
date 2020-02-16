/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format


import org.simplemes.eframe.custom.domain.FlexField
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.RMA

/**
 * Tests.
 */
class ConfigurableTypeDomainFormatSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, SERVER]

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
    flexType.fields << new FlexField(flexType: flexType, fieldName: 'Field1')
    flexType.save()

    when: 'a record with the Configurable Type is saved'
    def rma = new RMA(rma: 'ABC', rmaType: flexType)
    rma.setRmaTypeValue('Field1', 'XYZZY')
    rma.save()

    then: 'the fields values are pulled from storage'
    rma.getRmaTypeValue('Field1') == 'XYZZY'
  }

  @Rollback
  def "verify that getCurrentFields returns the list of fields for a flex type"() {
    given: 'a flex type with a field'
    def flexType = new FlexType(flexType: 'XYZ')
    flexType.fields << new FlexField(flexType: flexType, fieldName: 'Field1')
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
