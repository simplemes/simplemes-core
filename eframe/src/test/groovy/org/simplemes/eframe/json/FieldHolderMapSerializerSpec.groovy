/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.json

import groovy.json.JsonSlurper
import org.simplemes.eframe.custom.FieldHolderMap
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.EnabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum

/**
 * Tests.
 */
class FieldHolderMapSerializerSpec extends BaseSpecification {


  def "verify that basic serialize works - all supported field formats"() {
    given: 'a map to serialize'
    def map = new FieldHolderMap(parsingFromJSON: false)
    map.put('field1', value, fieldDefinition)

    when: 'the map is serialized'
    def s = objectMapper.writeValueAsString(map)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is correct'
    def json = new JsonSlurper().parseText(s)
    if (fieldDefinition) {
      assert fieldDefinition.format.convertFromJsonFormat(json.field1, fieldDefinition) == value
    } else {
      assert json.field1 == value
    }

    where:
    value                              | fieldDefinition
    'ABC'                              | null
    23                                 | null
    237L                               | new SimpleFieldDefinition(format: LongFieldFormat.instance)
    23.2                               | null
    true                               | null
    false                              | null
    'ABC'                              | null
    new DateOnly()                     | new SimpleFieldDefinition(format: DateOnlyFieldFormat.instance)
    new Date()                         | new SimpleFieldDefinition(format: DateFieldFormat.instance)
    ReportTimeIntervalEnum.LAST_7_DAYS | new SimpleFieldDefinition(format: EnumFieldFormat.instance, type: ReportTimeIntervalEnum)
    EnabledStatus.instance             | new SimpleFieldDefinition(format: EncodedTypeFieldFormat.instance, type: BasicStatus)
  }

  def "verify that basic serialize works - _config is serialized"() {
    given: 'a map to serialize'
    def map = new FieldHolderMap(parsingFromJSON: false)
    map.put('field1', 237L)

    when: 'the map is serialized'
    def s = objectMapper.writeValueAsString(map)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is correct'
    def json = new JsonSlurper().parseText(s)
    json._config.field1
  }

}
