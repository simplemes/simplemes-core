/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom

import ch.qos.logback.classic.Level
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.data.format.CustomChildListFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.EnabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.SampleParent

/**
 * Tests.
 */
class FieldHolderMapSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static specNeeds = SERVER

  def "verify that round-trip to-from JSON works"() {
    given: 'a map'
    def map = new FieldHolderMap(parsingFromJSON: false)
    map.pdq = 'def'
    map.abc = 'xyz'

    when: 'the map is serialized and deserialized to JSON'
    def s = map.toJSON()
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def map2 = FieldHolderMap.fromJSON(s)

    then: 'the deserialized works'
    map == map2

    and: 'the map is not the same object'
    !map.is(map2)
  }

  def "verify that native JSON types are not in config map"() {
    given: 'a map'
    def map = new FieldHolderMap(parsingFromJSON: false)
    map.stringValue = 'def'
    map.bigDecimalValue = 237.2
    map.booleanValue = true

    when: 'the map is serialized and deserialized to JSON'
    def s = map.toJSON()
    def map2 = FieldHolderMap.fromJSON(s)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the config is emtpy for these types'
    !map2[FieldHolderMap.CONFIG_ELEMENT_NAME]
  }

  def "verify that put and get preserve the field type for simple supported types"() {
    given: 'a map'
    def map = new FieldHolderMap(parsingFromJSON: false)

    when: 'the value is stored and retrieved'
    map.put('field1', value, fieldDefinition)

    and: 'the JSON round-trip is triggered'
    def map2 = FieldHolderMap.fromJSON(map.toJSON())
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(map.toJSON())}"

    then: 'the get returns the same object'
    map2.field1 == value
    map2.field1.class == value.class


    where:
    value                              | fieldDefinition
    'ABC'                              | null
    23                                 | null
    237L                               | null
    23.2                               | null
    true                               | null
    false                              | null
    'ABC'                              | null
    'ABC'                              | null
    new DateOnly()                     | null
    new Date()                         | null
    ReportTimeIntervalEnum.LAST_7_DAYS | new SimpleFieldDefinition(format: EnumFieldFormat.instance, type: ReportTimeIntervalEnum)
    EnabledStatus.instance             | new SimpleFieldDefinition(format: EncodedTypeFieldFormat.instance, type: BasicStatus)
  }

  @Rollback
  def "verify that put and get preserve the field type for domain reference"() {
    given: 'a map'
    def map = new FieldHolderMap(parsingFromJSON: false)

    and: 'a domain object reference and field definition'
    def sampleParent = new SampleParent(name: 'ABC').save()
    def fieldDefinition = new SimpleFieldDefinition(format: DomainReferenceFieldFormat.instance, type: SampleParent)

    when: 'the value is stored and retrieved'
    map.put('field1', sampleParent, fieldDefinition)

    and: 'the JSON round-trip is triggered'
    def map2 = FieldHolderMap.fromJSON(map.toJSON())
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(map.toJSON())}"

    then: 'the get returns the same object'
    map2.field1 == sampleParent
    map2.field1.class == sampleParent.class
  }

  @Rollback
  def "verify that put and get preserve the field type for custom child list reference"() {
    given: 'a map'
    def map = new FieldHolderMap(parsingFromJSON: false)

    and: 'a domain object reference and field definition'
    def sampleParent1 = new SampleParent(name: 'ABC1').save()
    def sampleParent2 = new SampleParent(name: 'ABC2').save()
    def fieldDefinition = new SimpleFieldDefinition(format: CustomChildListFieldFormat.instance, type: SampleParent)

    when: 'the value is stored and retrieved'
    map.put('field1', [sampleParent1, sampleParent2], fieldDefinition)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(map.toJSON())}"

    and: 'the JSON round-trip is triggered'
    def map2 = FieldHolderMap.fromJSON(map.toJSON())

    then: 'the get returns the same object'
    map2.field1 == [sampleParent1, sampleParent2]
    map2.field1.class == [].class
  }

  def "verify that put sets the dirty and parsingFromJSON flags correctly"() {
    when: 'the default map is created - as Jackson will create it'
    def map1 = new FieldHolderMap()
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(map1.toJSON())}"

    then: 'it is not dirty and is marked as parsing from JSON'
    !map1.isDirty()
    map1.isParsingFromJSON()

    when: 'a map is loaded from JSON'
    def map2 = FieldHolderMap.fromJSON('{"field1": "XYZ"}')

    then: 'it is not marked as dirty and not marked as parsing from JSON'
    !map2.isDirty()
    !map2.isParsingFromJSON()

    when: 'a value is stored and retrieved'
    map2.put("field1", "ABC")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(map2.toJSON())} ${map2.isDirty()} ${map2.dirty}"

    then: 'it is marked as dirty and not marked as parsing from JSON'
    map2.isDirty()
    !map2.isParsingFromJSON()
  }

  def "verify that put fails on child list field format"() {
    given: 'a map'
    def map = new FieldHolderMap(parsingFromJSON: false)

    and: 'a field definition'
    def fieldDefinition = new SimpleFieldDefinition(format: ChildListFieldFormat.instance, referenceType: SampleParent)

    when: 'the value is stored and retrieved'
    map.put('field1', [], fieldDefinition)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['ChildListFieldFormat', 'not', 'supported'])
  }

  def "verify that get logs warning when developer uses dirty as an element in dev or test mode"() {
    // The syntax: map.dirty will return an element value, not call the isDirty() method.
    // So we warning if this is found in dev/test mode.
    given: 'a map'
    def map = new FieldHolderMap()

    and: 'a mock appender for Warning level only'
    def mockAppender = MockAppender.mock(FieldHolderMap, Level.WARN)

    when: 'the flag is accessed by the element syntax'
    map.dirty

    then: 'the log message is written'
    //println "mockAppender = $mockAppender"
    mockAppender.assertMessageIsValid(['WARN', 'dirty', 'isDirty()', 'get()'])
  }

  def "verify that get logs warning when developer uses parsingFromJSON as an element in dev or test mode"() {
    // The syntax: map.dirty will return an element value, not call the isParsingFromJSON() method.
    // So we warning if this is found in dev/test mode.
    given: 'a map'
    def map = new FieldHolderMap()

    and: 'a mock appender for Warning level only'
    def mockAppender = MockAppender.mock(FieldHolderMap, Level.WARN)

    when: 'the flag is accessed by the element syntax'
    map.parsingFromJSON

    then: 'the log message is written'
    //println "mockAppender = $mockAppender"
    mockAppender.assertMessageIsValid(['WARN', 'parsingFromJSON', 'isParsingFromJSON()', 'get()'])
  }

  def "verify that mergeMap preserves original values - config is in original"() {
    given: 'a map with some old values - one with config'
    def origMap = new FieldHolderMap(parsingFromJSON: false)
    def fieldDefinition = new SimpleFieldDefinition(name: 'field1', format: DateOnlyFieldFormat.instance)
    def dateOnly = DateUtils.subtractDays(new DateOnly(), 2)
    origMap.put('field1', dateOnly, fieldDefinition)
    origMap.put('field2', 'xyz')
    origMap.put('field4', new DateOnly(), fieldDefinition)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(origMap.toJSON())}"

    and: 'a second map to be merged into the original'
    def updatedDateOnly = DateUtils.subtractDays(new DateOnly(), 237)
    def src = """
      {
        "field2" : "xyz",
        "field3" : "pdq",
        "field4" : "${updatedDateOnly.toString()}"
      }
      """
    def map = FieldHolderMap.fromJSON(src)

    when: 'the map is merged'
    origMap.mergeMap(map, 'xyzzy')

    then: 'the map contains all of the values'
    origMap.field1 == dateOnly
    origMap.field2 == 'xyz'
    origMap.field3 == 'pdq'
    origMap.field4 == updatedDateOnly

    origMap[FieldHolderMap.CONFIG_ELEMENT_NAME] != null
    origMap[FieldHolderMap.CONFIG_ELEMENT_NAME].field1 != null
  }

  def "verify that mergeMap will merge config values from input"() {
    given: 'a map with some old values - one with config'
    def origMap = new FieldHolderMap(parsingFromJSON: false)
    def fieldDefinition = new SimpleFieldDefinition(name: 'field1', format: DateOnlyFieldFormat.instance)
    def dateOnly = DateUtils.subtractDays(new DateOnly(), 4)
    origMap.put('field1', dateOnly, fieldDefinition)
    origMap.put('field2', 'xyz')
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(origMap.toJSON())}"

    and: 'a second map to be merged into the original'
    def map = new FieldHolderMap(parsingFromJSON: false)
    map.put('newField', ReportTimeIntervalEnum.LAST_7_DAYS,
            new SimpleFieldDefinition(format: EnumFieldFormat.instance, type: ReportTimeIntervalEnum))
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(map.toJSON())}"

    when: 'the map is merged'
    origMap.mergeMap(map, 'xyz')

    then: 'the map contains all of the values'
    origMap.field1 == dateOnly
    origMap.field2 == 'xyz'
    origMap.newField == ReportTimeIntervalEnum.LAST_7_DAYS
    origMap.newField.class == ReportTimeIntervalEnum.LAST_7_DAYS.class

    and: 'the config is correct'
    origMap[FieldHolderMap.CONFIG_ELEMENT_NAME] != null
    origMap[FieldHolderMap.CONFIG_ELEMENT_NAME].field1 != null
    origMap[FieldHolderMap.CONFIG_ELEMENT_NAME].newField != null
  }

  def "verify that mergeMap combines values with no overlap"() {
    given: 'a map with config for one field'
    def origMap = new FieldHolderMap(parsingFromJSON: false)
    def dateOnly = DateUtils.subtractDays(new DateOnly(), 4)
    origMap.put('field1', dateOnly)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(origMap.toJSON())}"

    and: 'a second map to be merged into the original'
    def date = new Date()
    def map = new FieldHolderMap(parsingFromJSON: false)
    map.put('newField', date)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(map.toJSON())}"

    when: 'the map is merged'
    origMap.mergeMap(map, 'xyz')

    then: 'the map contains all of the values'
    origMap.field1 == dateOnly
    origMap.newField == date

    and: 'the config is correct'
    origMap[FieldHolderMap.CONFIG_ELEMENT_NAME].newField != null
    origMap[FieldHolderMap.CONFIG_ELEMENT_NAME].newField.type == DateFieldFormat.instance.id
  }

  def "verify that mergeMap detects change in type and fails gracefully"() {
    given: 'a map with config for one field'
    def origMap = new FieldHolderMap(parsingFromJSON: false)
    def dateOnly = DateUtils.subtractDays(new DateOnly(), 4)
    origMap.put('field1', dateOnly)
    //println "Orig JSON = ${groovy.json.JsonOutput.prettyPrint(origMap.toJSON())}"

    and: 'a second map to be merged into the original'
    def map = new FieldHolderMap(parsingFromJSON: false)
    map.put('field1', new Date())
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(map.toJSON())}"

    when: 'the map is merged'
    origMap.mergeMap(map, 'xyzzy')

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['field1', 'xyzzy'], 212)
  }

  // mergeConfig - changes type - no change

  // mergeMap Config is not overwritten if conflicting values are passed in as src.
  // mergeMap Config can be updated from src if there are no conflicts.
  // TODO: Support history on mergeMap
}
