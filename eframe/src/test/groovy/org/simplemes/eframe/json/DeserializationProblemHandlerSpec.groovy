/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.CustomOrderComponent
import sample.domain.Order
import sample.domain.RMA

/**
 * Tests.
 */
class DeserializationProblemHandlerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [FlexType]

  @Rollback
  def "verify that deserialize of custom child lists supports child records with domain references"() {
    given: 'foreign reference'
    def flexType = DataGenerator.buildFlexType()

    and: 'a domain with a custom field'
    def order = new Order(order: 'M1001').save()
    order.customComponents << new CustomOrderComponent(sequence: 237, assyDataType: flexType)
    order.save()

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(order)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the object is deleted'
    order.delete()
    assert CustomOrderComponent.list().size() == 0

    and: 'the object is re-created'
    def order2 = Holders.objectMapper.readValue(s, Order)

    then: 'the object is correct'
    order2.customComponents.size() == 1
    //noinspection GroovyAssignabilityCheck
    order2.customComponents[0].assyDataType == flexType
  }

  @Rollback
  def "verify that deserialize of configurable type fields works"() {
    given: 'foreign reference'
    def flexType = DataGenerator.buildFlexType(fieldCount: 2)

    and: 'the json for a flex type field value'
    def request = """{
      "rmaType": {"uuid": "$flexType.uuid"},
      "rmaType_FIELD1": "ACME-101",
      "rmaType_FIELD2": "ACME-102"
    }
    """

    when: 'the JSON is read'
    def rma = Holders.objectMapper.readValue(request, RMA)

    then: 'the object is correct'
    rma.getRmaTypeValue('FIELD1') == 'ACME-101'
    rma.getRmaTypeValue('FIELD2') == 'ACME-102'
  }

  def "verify that deserialize of configurable type fields works - supported field types"() {
    given: 'foreign reference'
    def flexType = DataGenerator.buildFlexType(fieldFormat: format.instance)

    and: 'the json for a flex type field value'
    def valueString = JavascriptUtils.formatForObject(value, (FieldFormatInterface) format.instance)
    def request = """{
      "rmaType": {"uuid": "$flexType.uuid"},
      "rmaType_FIELD1": $valueString
    }
    """

    when: 'the JSON is read'
    def rma = Holders.objectMapper.readValue(request, RMA)

    then: 'the object is correct'
    rma.getRmaTypeValue('FIELD1') == result

    where:
    value                                           | format                | result
    '1.2'                                           | BigDecimalFieldFormat | 1.2
    '12'                                            | IntegerFieldFormat    | 12
    'ABC'                                           | StringFieldFormat     | 'ABC'
    'A"BC'                                          | StringFieldFormat     | 'A"BC'
    "A'BC"                                          | StringFieldFormat     | "A'BC"
    'true'                                          | BooleanFieldFormat    | true
    'false'                                         | BooleanFieldFormat    | false
    '23527'                                         | LongFieldFormat       | 23527L
    new Date(UnitTestUtils.SAMPLE_TIME_MS)          | DateFieldFormat       | new Date(UnitTestUtils.SAMPLE_TIME_MS)
    new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS) | DateOnlyFieldFormat   | new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
  }
}
