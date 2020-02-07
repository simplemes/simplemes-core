/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/**
 * Tests the JSON custom field serializer.
 */
class CustomFieldSerializerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain, SampleParent]

  @Rollback
  def "verify that serialize handles multiple custom fields"() {
    given: 'a domain object with custom fields'
    buildCustomField([[fieldName: 'custom1', domainClass: SampleParent],
                      [fieldName: 'custom2', domainClass: SampleParent]])
    def (SampleParent sampleParent) = DataGenerator.generate {
      domain SampleParent
    }
    sampleParent.setFieldValue('custom1', 'abc')
    sampleParent.setFieldValue('custom2', 'xyz')

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(sampleParent)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is correct'
    def json = new JsonSlurper().parseText(s)
    json.custom1 == 'abc'
    json.custom2 == 'xyz'
  }

  @Rollback
  def "verify that serialize handles empty values"() {
    given: 'a domain object with custom fields'
    buildCustomField(fieldName: 'custom1', domainClass: SampleParent)
    def (SampleParent sampleParent) = DataGenerator.generate {
      domain SampleParent
    }

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(sampleParent)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is correct'
    def json = new JsonSlurper().parseText(s)
    json.custom1 == null
  }

  def "verify that serialize handles supported field types"() {
    given: 'a domain object with custom fields'
    buildCustomField(fieldName: 'custom1', domainClass: SampleParent, fieldFormat: format.instance,
                     valueClassName: valueClass?.name)
    def (SampleParent sampleParent) = DataGenerator.generate {
      domain SampleParent
    }

    and: 'a domain for a foreign reference custom value'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
    }

    when: 'the custom field is set'
    if (valueClass == AllFieldsDomain) {
      value = allFieldsDomain
    }
    sampleParent.setFieldValue('custom1', value)

    and: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(sampleParent)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is correct'
    //def json = new JsonSlurper().parseText(s)
    def sampleParent2 = Holders.objectMapper.readValue(s, SampleParent)
    sampleParent2.getFieldValue('custom1') == value

    where: 'supports all field formats'
    format                     | valueClass             | value
    StringFieldFormat          | null                   | 'abc'
    IntegerFieldFormat         | null                   | 437
    LongFieldFormat            | null                   | 1337L
    BigDecimalFieldFormat      | null                   | 12.2
    BooleanFieldFormat         | null                   | true
    DateOnlyFieldFormat        | null                   | new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    DateFieldFormat            | null                   | new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    EnumFieldFormat            | ReportTimeIntervalEnum | ReportTimeIntervalEnum.YESTERDAY
    EncodedTypeFieldFormat     | BasicStatus            | DisabledStatus.instance
    DomainReferenceFieldFormat | AllFieldsDomain        | _
  }

}
