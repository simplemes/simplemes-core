/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports


import org.simplemes.eframe.test.BaseSpecification

/**
 * Tests.
 */
class ReportHelperSpec extends BaseSpecification {

  def setupSpec() {
    //mockDomains UserPreference
  }

  /**
   * The dummy sample report.
   */
  static final String SAMPLE_GOOD_REPORT = "reports/sample/SampleReport.jrxml"

  void cleanup() {
    ReportEngine.instance = new ReportEngine()
  }


  def "verify that determineBuiltinReports can find the available reports"() {
    when: 'the reports are found'
    def reports = ReportHelper.instance.determineBuiltinReports()

    then: 'the reports list contains the one shipped with the module'
    reports.size() > 0
    reports.contains('reports/ArchiveLog.jrxml')
  }

  def "verify that determineReportBaseName extracts the correct report base name"() {
    expect: 'the baseName is calculated correctly'
    ReportHelper.instance.determineReportBaseName(path) == result

    where:
    path                     | result
    'reports/Metrics.jrxml'  | 'Metrics'
    'reportsMetrics.jrxml'   | 'reportsMetrics'
    '/abc/reports/xyz.jrxml' | 'xyz'
    ''                       | ''
    null                     | ''

  }

  def "verify that getInputStream can find the report from the built-in reports folder"() {
    when: 'the report can be opened as a input stream'
    def reportDetails = new Report(SAMPLE_GOOD_REPORT, [params: [:]])
    def inputStream = ReportHelper.instance.getInputStream(reportDetails)

    then: 'the input stream is correct'
    inputStream != null

    and: 'this is a report file'
    def bytes = new byte[500]
    inputStream.read(bytes)
    def s = new String(bytes)
    s.contains('<jasperReport')

    cleanup:
    inputStream?.close()
  }

  def "verify that buildFieldDefinitionsFromParameters builds the right parameters"() {
    when: 'the fields are created'
    def report = new Report(SAMPLE_GOOD_REPORT, [params: [loc: SAMPLE_GOOD_REPORT]])
    def fieldDefinitions = ReportHelper.instance.buildFieldDefinitionsFromParameters(report)

    then: 'field list is correct'
    fieldDefinitions.size() == 5

    and: 'contains the expected fields'
    fieldDefinitions.reportDate.sequence == 1
    fieldDefinitions.name.sequence == 50
    fieldDefinitions.reportTimeInterval.sequence == 100
    fieldDefinitions.startDateTime.sequence == 110
    fieldDefinitions.endDateTime.sequence == 120

  }

}

