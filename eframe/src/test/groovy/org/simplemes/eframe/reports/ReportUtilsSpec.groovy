/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator

/**
 * Tests.
 */
class ReportUtilsSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static dirtyDomains = [FlexType]

  def "verify that format handles supported simple cases."() {
    given: 'a flex type with some fields'
    def flexType = DataGenerator.buildFlexType([[fieldName: 'FIELD1', fieldLabel: 'Field1', fieldFormat: StringFieldFormat.instance],
                                                [fieldName: 'INT2', fieldLabel: 'Int2', fieldFormat: IntegerFieldFormat.instance],
                                                [fieldName: 'DATE_ONLY3', fieldLabel: 'Date3', fieldFormat: DateOnlyFieldFormat.instance]])

    and: 'the data formatted for JSON'
    def values = [:]
    def name = 'assemblyDataType'
    fields.each { k, v ->
      values["${name}_$k"] = v
    }
    def json = Holders.objectMapper.writeValueAsString(values)

    expect: 'the format works'
    ReportUtils.formatFields(json, flexType.uuid.toString(), name, highlight, max) == result

    where:
    fields                        | highlight | max | result
    [FIELD1: "value1", INT2: 237] | true      | 50  | '<b>Field1</b>: value1 <b>Int2</b>: 237'
    [FIELD1: "value1", INT2: 237] | true      | 20  | '<b>Field1</b>: value1 ...'
    [FIELD1: "value1", INT2: 237] | false     | 50  | 'Field1: value1 Int2: 237'
  }
}
