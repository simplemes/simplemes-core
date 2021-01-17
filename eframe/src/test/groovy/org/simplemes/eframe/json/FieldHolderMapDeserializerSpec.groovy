/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.json

import org.simplemes.eframe.custom.FieldHolderMap
import org.simplemes.eframe.custom.FieldHolderMapInterface
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.reports.ReportTimeIntervalEnum
import org.simplemes.eframe.test.BaseSpecification

/**
 *  Tests.
 */
class FieldHolderMapDeserializerSpec extends BaseSpecification {

  def "verify that basic deserialize works for new Map - basic JSON types"() {
    given: 'json to deserialize'
    def s = """
      {
        "fieldString": "ABC",             
        "fieldNumber": 3.2,             
        "fieldInt": 437,             
        "fieldLong": 437437437437,             
        "fieldBoolean": true,             
        "fieldNull": null,
        "_config": {               
          "weight": {
            "type": "I",
            "tracking": "A",
            "history": [           
            ]
          }
        }             
      }
    """

    when: 'the map is deserialized'
    def map = objectMapper.readValue(s, FieldHolderMapInterface)
    //println "map2 = $map"

    then: 'the new map is correct'
    map.fieldString == "ABC"
    map.fieldNumber == 3.2
    map.fieldNumber.class == BigDecimal
    map.fieldInt == 437
    map.fieldLong == 437437437437L
    map.fieldLong.class == Long
    map.fieldBoolean == true
    map.fieldNull == null

    and: 'the config sub-map is correct'
    def config = map._config
    config.weight
    config.weight.type == 'I'
    config.weight.tracking == 'A'
    config.weight.history != null
  }

  def "verify that deserialize works for update of existing Map - basic JSON types"() {
    given: 'an original map with some values in it'
    def originalJSON = """
      {
        "fieldString": "ABC",             
        "fieldNumber": 3.2,             
        "fieldNull": null,
        "_config": {               
          "weight": {
            "type": "I",
            "tracking": "A",
            "history": [
              {
                "weight": 2.3,
                "user": "PWB",
                "dateTime": "2009-02-13T18:31:30.000-05:00"
              },
              {
                "weight": 2.1,
                "user": "RLB",
                "dateTime": "2009-02-14T17:23:53.000-05:00"
              }           
            ]
          }
        }             
      }
    """
    def originalMap = objectMapper.readValue(originalJSON, FieldHolderMapInterface)

    and: 'the json to process in an update action'
    def s = """
      {
        "fieldString": "XYZ",             
        "fieldNumber": 3.2,             
        "fieldNull": "a value"
      }
    """

    when: 'the map is deserialized'
    def map = objectMapper.readerForUpdating(originalMap).readValue(s, FieldHolderMapInterface)

    then: 'the updated map is correct'
    map.fieldString == "XYZ"
    map.fieldNumber == 3.2
    map.fieldNull == "a value"

    and: 'the config sub-map is correct'
    def config = map._config
    config.weight
    config.weight.type == 'I'
    config.weight.tracking == 'A'
    config.weight.history.size() == 2
  }

  def "verify that deserialize can accept the custom field with a type - create"() {
    given: 'json to deserialize'
    //    ReportTimeIntervalEnum.LAST_7_DAYS | new SimpleFieldDefinition(format: EnumFieldFormat.instance, type: ReportTimeIntervalEnum)
    def s = """
      {
        "field1": "${ReportTimeIntervalEnum.LAST_7_DAYS}",
        "_config": {
          "field1": {
            "type": "E",
            "tracking": "A",
            "valueClassName": "${ReportTimeIntervalEnum.name}"
          }
        }
      }
    """

    when: 'the map is deserialized'
    def map = objectMapper.readValue(s, FieldHolderMapInterface)

    then: 'the new map is correct'
    map.field1 == ReportTimeIntervalEnum.LAST_7_DAYS
  }

  def "verify that deserialize preserves the _config when updating with _config - new field"() {
    given: 'a field holder with another existing field and type'
    def originalMap = new FieldHolderMap(parsingFromJSON: false)
    def dateOnly = new DateOnly()
    originalMap.put('fieldDateOnly', dateOnly)

    and: 'json to add ne field with config'
    def s = """
      {
        "field1": "${ReportTimeIntervalEnum.LAST_7_DAYS}",
        "_config": {
          "field1": {
            "type": "E",
            "tracking": "A",
            "valueClassName": "${ReportTimeIntervalEnum.name}"
          }
        }
      }
    """

    when: 'the map is deserialized'
    def map = objectMapper.readerForUpdating(originalMap).readValue(s, FieldHolderMapInterface)

    then: 'the new map is correct'
    map.field1 == ReportTimeIntervalEnum.LAST_7_DAYS

    and: 'original config is in the merged result'
    map.fieldDateOnly == dateOnly
  }


  // preserves unchanged values
  // preserves history?
}
