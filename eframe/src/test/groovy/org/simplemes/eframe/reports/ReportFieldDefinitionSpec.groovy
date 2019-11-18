package org.simplemes.eframe.reports

import net.sf.jasperreports.engine.JRParameter
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Tests.
 */
class ReportFieldDefinitionSpec extends BaseSpecification {

  /**
   * The dummy sample report.
   */
  public static final String SAMPLE_REPORT = "reports/sampleBad/SampleReport.jrxml"


  def "verify that the parameter constructor works"() {
    given: 'stubs for the parameter and default parameters'
    def parameter = Stub(JRParameter)
    parameter.forPrompting >> true
    parameter.systemDefined >> false
    parameter.name >> 'metricName'
    parameter.valueClass >> String

    and: 'a report detail to work from'
    def reportDetails = new Report(SAMPLE_REPORT, [defaultValues          : [metricName: 'ABC-XYZ'],
                                                   definedReportParameters: [parameter]])

    when: 'the constructor is built'
    def reportFieldDefinition = new ReportFieldDefinition(parameter, reportDetails)

    then: 'the field values are correct'
    reportFieldDefinition.name == 'metricName'
    reportFieldDefinition.format == StringFieldFormat.instance

    and: 'the default value is used with no parameters'
    reportFieldDefinition.effectiveValue == 'ABC-XYZ'

    and: 'the toString method works'
    reportFieldDefinition.toString()
  }

  def "verify that the parameter constructor works with a value set in the controller params in the reportDetails"() {
    given: 'stubs for the parameter and default parameters'
    def parameter = Stub(JRParameter)
    parameter.forPrompting >> true
    parameter.systemDefined >> false
    parameter.name >> 'metricName'
    parameter.valueClass >> String

    and: 'a report detail to work from'
    def reportDetails = new Report(SAMPLE_REPORT, [defaultValues          : [metricName: 'ABC-XYZ'],
                                                   definedReportParameters: [parameter],
                                                   params                 : [metricName: 'paramValue']])

    when: 'the constructor is built'
    def reportFieldDefinition = new ReportFieldDefinition(parameter, reportDetails)

    then: 'the controller report parameter value is used'
    reportFieldDefinition.effectiveValue == 'paramValue'
  }

  def "verify that the parameter constructor for dates works with string inputs from controller"() {
    given: 'stubs for the parameter and default parameters'
    def parameter = Stub(JRParameter)
    parameter.forPrompting >> true
    parameter.systemDefined >> false
    parameter.name >> 'startDateTime'
    parameter.valueClass >> Date

    and: 'a report detail to work from'
    def now = new Date()
    def paramDate = now - 1
    def reportDetails = new Report(SAMPLE_REPORT, [defaultValues          : [startDateTime: now],
                                                   definedReportParameters: [parameter],
                                                   params                 : [startDateTime: ISODate.format(paramDate)]])

    when: 'the constructor is built'
    def reportFieldDefinition = new ReportFieldDefinition(parameter, reportDetails)

    then: 'the controller report parameter value is used'
    reportFieldDefinition.effectiveValue == paramDate
  }


  def "verify that the parameter constructor for dates works with date default from compiled report"() {
    given: 'stubs for the parameter and default parameters'
    def parameter = Stub(JRParameter)
    parameter.forPrompting >> true
    parameter.systemDefined >> false
    parameter.name >> 'startDateTime'
    parameter.valueClass >> Date

    and: 'a report detail to work from'
    def now = new Date()
    def reportDetails = new Report(SAMPLE_REPORT, [defaultValues          : [startDateTime: now],
                                                   definedReportParameters: [parameter]])

    when: 'the constructor is built'
    def reportFieldDefinition = new ReportFieldDefinition(parameter, reportDetails)

    then: 'the controller report parameter value is used'
    reportFieldDefinition.effectiveValue == now
  }

  def "verify that the parameter constructor for reportTimeInterval detects the special format"() {
    given: 'stubs for the parameter and default parameters'
    def parameter = Stub(JRParameter)
    parameter.forPrompting >> true
    parameter.systemDefined >> false
    parameter.name >> 'reportTimeInterval'
    parameter.valueClass >> String

    and: 'a report detail to work from'
    def reportDetails = new Report(SAMPLE_REPORT, [defaultValues          : [:],
                                                   definedReportParameters: [parameter]])

    when: 'the constructor is built'
    def reportFieldDefinition = new ReportFieldDefinition(parameter, reportDetails)

    then: 'the right field format is used'
    reportFieldDefinition.format == EnumFieldFormat.instance

    and: 'the right types are set'
    reportFieldDefinition.type == ReportTimeIntervalEnum
    reportFieldDefinition.referenceType == ReportTimeIntervalEnum
  }
}
