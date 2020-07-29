/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports

import org.simplemes.eframe.misc.ClassPathScanner
import org.simplemes.eframe.misc.ClassPathScannerFactory
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
    reports.contains('reports/eframe/ArchiveLog.jrxml')
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

  def "verify that determineBuiltinReports filters out the reports that start with _"() {
    given: 'a mocked scanner'
    List results = [new URL('jar:file:/C:/Users/mes-core-0.5.jar!/reports/detail/ProductionForDate.jrxml'),
                    new URL('jar:file:/C:/Users/mes-core-0.5.jar!/reports/detail/_ProductionSubReport.jrxml'),
                    'reports/app/_TravellerOperations.jrxml']
    def mockScanner = Mock(ClassPathScanner)
    def mockFactory = Mock(ClassPathScannerFactory)
    ClassPathScanner.factory = mockFactory
    1 * mockScanner.scan() >> results
    1 * mockFactory.buildScanner(_) >> mockScanner

    when: 'the reports are found'
    def reports = ReportHelper.instance.determineBuiltinReports()

    then: 'the reports list contains the one shipped with the module'
    reports.size() == 1
    reports.contains('jar:file:/C:/Users/mes-core-0.5.jar!/reports/detail/ProductionForDate.jrxml')

    cleanup:
    ClassPathScanner.factory = new ClassPathScannerFactory()
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

  def "verify that getInputStream open a stream from a .jar file"() {
    given: 'the location of a .jar file based resource'
    def scanner = new ClassPathScanner('io/micronaut/data/jdbc/annotation/*.class')
    def list = scanner.scan()
    def url = list.find { it.toString().endsWith('io/micronaut/data/jdbc/annotation/JdbcRepository.class') }
    def path = url.toString() - "/io/micronaut/data/jdbc/annotation/JdbcRepository.class"

    and: 'a ReportDetails object with faked-out path/file name to simulate a .jar file read'
    // This is intentionally not a .jrxml file since we have none in .jar files for this module to use.
    // Instead, we will use a .class file from Micronaut to test opening a stream from a .jar file.
    def reportDetails = new Report("")
    reportDetails.reportFolder = path
    reportDetails.reportName = "io/micronaut/data/jdbc/annotation/JdbcRepository.class"

    when: 'the report can be opened as a input stream'
    def inputStream = ReportHelper.instance.getInputStream(reportDetails)

    then: 'the input stream is correct'
    inputStream != null

    and: 'this is the right class file contents'
    def bytes = new byte[500]
    inputStream.read(bytes)
    def s = new String(bytes)
    s.contains('Repository')

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

