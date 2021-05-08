/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.domain.controller

import io.micronaut.http.HttpStatus
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.reports.ReportTimeIntervalEnum
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.DataGenerator
import sample.domain.AllFieldsDomain
import sample.domain.RMA
import sample.domain.SampleChild
import sample.domain.SampleParent

/**
 * Tests.
 */
class DomainControllerSpec extends BaseAPISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain]

  def setup() {
    waitForInitialDataLoad()
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that basic displayFields GET works with a live server"() {
    when: 'the get is triggered'
    login()
    def s = sendRequest(uri: "/domain/displayFields?domain=${RMA.name}")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is valid'
    def map = Holders.objectMapper.readValue(s, Map)
    map.top.size() == 1

    and: 'the top fields is correct'
    def top = map.top[0]
    top.fieldName == 'rma'
    top.fieldLabel == "label.rma"
    top.fieldFormat == StringFieldFormat.instance.id
    !top.fieldDefault
    top.maxLength == 255

    and: 'the bottom fields section is correct'
    def bottom = map.bottom
    bottom.size() > 1

    def names = bottom*.fieldName
    names.contains('status')
    names.contains('product')
    names.contains('qty')
    names.contains('returnDate')
    names.contains('rmaType')
    !names.contains('rmaSummary')
  }

  def "verify that displayFields GET works tabbed panels"() {
    when: 'the get is triggered'
    login()
    def s = sendRequest(uri: "/domain/displayFields?domain=${AllFieldsDomain.name}")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is valid'
    def map = Holders.objectMapper.readValue(s, Map)
    map.top.size() == 1

    and: 'the tabs array is correct'
    def tabs = map.tabs as List
    tabs.size() == 2

    def tabNames = tabs*.tab
    tabNames == ['main', 'details']

    and: 'the first tab contents are correct'
    tabs[0].tabLabel == 'label.main'
    def tab0 = tabs[0]
    def fieldNames0 = tab0.fields*.fieldName
    fieldNames0 == ['title', 'qty', 'count', 'enabled', 'dueDate', 'dateTime']

    and: 'the second tab contents are correct'
    tabs[1].tabLabel == 'label.details'
    def tab1 = tabs[1]
    def fieldNames1 = tab1.fields*.fieldName
    fieldNames1 == ['notes', 'transientField', 'reportTimeInterval', 'order', 'status', 'displayOnlyText']
  }

  def "verify that basic displayFields GET works - child field list for inline grids"() {
    when: 'the get is triggered'
    login()
    def s = sendRequest(uri: "/domain/displayFields?domain=${FlexType.name}")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is valid'
    def map = Holders.objectMapper.readValue(s, Map)
    def bottom = map.bottom
    def fieldList = bottom.find { it.fieldName == 'fields' }
    fieldList.fieldFormat == 'C'
    def fields = fieldList.fields
    fields.size() == 8

    def names = fields*.fieldName
    names.contains('sequence')
    names.contains('fieldName')
    names.contains('fieldLabel')
    names.contains('fieldFormat')
    names.contains('maxLength')
    names.contains('required')
    names.contains('historyTracking')
    names.contains('valueClassName')
  }

  def "verify that displayFields provides the server-side defaults"() {
    when: 'the get is triggered'
    login()
    def s = sendRequest(uri: "/domain/displayFields?domain=${FlexType.name}")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is valid'
    def map = Holders.objectMapper.readValue(s, Map)
    def bottom = map.bottom
    def fieldList = bottom.find { it.fieldName == 'fields' }
    fieldList.fieldFormat == 'C'
    def fields = fieldList.fields

    def sequenceField = fields.find { it.fieldName == 'sequence' }
    sequenceField.defaultValue == "_max('sequence')+10"

    def formatField = fields.find { it.fieldName == 'fieldFormat' }
    formatField.defaultValue == "'S'"

    def historyField = fields.find { it.fieldName == 'historyTracking' }
    historyField.defaultValue == "'NONE'"

    and: 'the top-level default values are correct'
    def categoryField = bottom.find { it.fieldName == 'category' }
    categoryField.defaultValue == "'${FlexType.CATEGORY_BASIC}'"
  }

  def "verify that displayFields provides the server-side defaults - boolean set to true"() {
    when: 'the get is triggered'
    login()
    def s = sendRequest(uri: "/domain/displayFields?domain=${User.name}")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is valid'
    def map = Holders.objectMapper.readValue(s, Map)
    def bottom = map.bottom
    def enabledField = bottom.find { it.fieldName == 'enabled' }
    enabledField.defaultValue == "true"
  }

  def "verify that displayFields provides the server-side defaults - integer set to non-zero"() {
    when: 'the get is triggered'
    login()
    def s = sendRequest(uri: "/domain/displayFields?domain=${SampleChild.name}")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is valid'
    def map = Holders.objectMapper.readValue(s, Map)
    def bottom = map.bottom
    def sequenceField = bottom.find { it.fieldName == 'sequence' }
    sequenceField.defaultValue == "10"
  }

  def "verify that displayFields provides the client-side defaults"() {
    when: 'the get is triggered'
    login()
    def s = sendRequest(uri: "/domain/displayFields?domain=${FlexType.name}")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is valid'
    def map = Holders.objectMapper.readValue(s, Map)
    def bottom = map.bottom
    def fieldList = bottom.find { it.fieldName == 'fields' }
    fieldList.fieldFormat == 'C'
    def fields = fieldList.fields

    def sequenceField = fields.find { it.fieldName == 'sequence' }
    sequenceField.defaultValue == "_max('sequence')+10"
  }

  def "verify that displayFields fails with bad request status with missing domain name"() {
    when: 'the get is triggered'
    login()
    disableStackTraceLogging()
    sendRequest(uri: "/domain/displayFields", status: HttpStatus.BAD_REQUEST)

    then: 'no exceptions'
    notThrown(Exception)
  }

  def "verify that buildFieldElement works on the various types"() {
    when: 'the build method is called'
    def fieldDef = DomainUtils.instance.getFieldDefinitions(AllFieldsDomain)[fieldName]
    def map = new DomainController().buildFieldElement(fieldDef, AllFieldsDomain)

    then: 'the field values are valid'
    map.fieldName == fieldName
    map.fieldFormat == format

    where:
    fieldName            | format
    'title'              | StringFieldFormat.instance.id
    'qty'                | BigDecimalFieldFormat.instance.id
    'intPrimitive'       | IntegerFieldFormat.instance.id
    'enabled'            | BooleanFieldFormat.instance.id
    'reportTimeInterval' | EnumFieldFormat.instance.id
    'order'              | EnumFieldFormat.instance.id
    'status'             | EnumFieldFormat.instance.id
  }

  def "verify that buildFieldElement builds the right valid values - enums"() {
    when: 'the build method is called'
    def fieldDef = DomainUtils.instance.getFieldDefinitions(AllFieldsDomain)['reportTimeInterval']
    def map = new DomainController().buildFieldElement(fieldDef, AllFieldsDomain)
    //println "map = ${TextUtils.prettyFormat(map)}"

    then: 'the valid values are correct'
    map.validValues.size() == ReportTimeIntervalEnum.enumConstants.size()

    map.validValues.find { it.id == 'TODAY' }.value == 'reportTimeIntervalEnum.TODAY'
    map.validValues.find { it.id == 'THIS_MONTH' }.value == 'reportTimeIntervalEnum.THIS_MONTH'
  }

  def "verify that buildFieldElement builds the right valid values - encodedType"() {
    when: 'the build method is called'
    def fieldDef = DomainUtils.instance.getFieldDefinitions(AllFieldsDomain)['status']
    def map = new DomainController().buildFieldElement(fieldDef, AllFieldsDomain)
    //println "map = ${TextUtils.prettyFormat(map)}"

    then: 'the valid values are correct'
    map.validValues.size() == BasicStatus.getValidValues().size()

    map.validValues.find { it.id == 'ENABLED' }.value == 'label.enabledStatus'
    map.validValues.find { it.id == 'DISABLED' }.value == 'label.disabledStatus'
  }

  def "verify that buildFieldElement builds the right valid values - domain ref"() {
    when: 'the build method is called'
    def fieldDef = DomainUtils.instance.getFieldDefinitions(AllFieldsDomain)['order']
    def map = new DomainController().buildFieldElement(fieldDef, AllFieldsDomain)

    then: 'the valid values are correct'
    !map.validValues
    map.validValuesURI == '/order/suggest'
  }

  def "verify that buildFieldElement builds the right valid values - list of refs"() {
    given: 'some records for the valid values'
    DataGenerator.generate {
      domain AllFieldsDomain
      count 10
    }

    when: 'the build method is called'
    def fieldDef = DomainUtils.instance.getFieldDefinitions(SampleParent)['allFieldsDomains']
    def map = new DomainController().buildFieldElement(fieldDef, SampleParent)
    //println "map = ${TextUtils.prettyFormat(map)}"

    then: 'the valid values are correct'
    map.validValues.size() == 10

    map.validValues.find { it.value == 'ABC001' }
    map.validValues.find { it.value == 'ABC010' }
  }

  // configurable type

}
