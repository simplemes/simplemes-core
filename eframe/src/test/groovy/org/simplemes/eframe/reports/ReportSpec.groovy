package org.simplemes.eframe.reports

import net.sf.jasperreports.engine.JRExpression
import net.sf.jasperreports.engine.JRParameter
import net.sf.jasperreports.engine.JRPropertiesMap
import net.sf.jasperreports.engine.JasperReport
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockPreferenceHolder
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Tests.
 */
class ReportSpec extends BaseSpecification {
  /**
   * The dummy sample report.
   */
  public static final String SAMPLE_REPORT = "reports/sampleBad/SampleReport.jrxml"

  void cleanup() {
    ReportEngine.instance = new ReportEngine()
  }

  /**
   * Utility method to build a stub for the JRParameter for unit tests.
   * @param name The parameter name.
   * @param sequence The display sequence.
   * @return The stubbed parameter.
   */
  Object parameterStub(String name, Object sequence = null) {
    def propertiesMap1 = Stub(JRPropertiesMap)
    propertiesMap1.getProperty('sequence') >> sequence
    def parameter1 = Stub(JRParameter)
    parameter1.forPrompting >> true
    parameter1.systemDefined >> false
    parameter1.name >> name
    parameter1.valueClass >> String
    parameter1.propertiesMap >> propertiesMap1

    return parameter1
  }

  /**
   * Utility method to build a stub for the JRParameter for unit tests.
   * @param name The parameter name.
   * @param value The parameter's default value.
   * @param sequence The display sequence.
   * @return The stubbed parameter.
   */
  Object parameterStub(String name, String value, Object sequence) {
    def propertiesMap1 = Stub(JRPropertiesMap)
    propertiesMap1.getProperty('sequence') >> sequence
    def parameter1 = Stub(JRParameter)
    parameter1.forPrompting >> true
    parameter1.systemDefined >> false
    parameter1.name >> name
    parameter1.valueClass >> String
    def expression = Stub(JRExpression)
    expression.text >> value
    parameter1.defaultValueExpression >> expression
    parameter1.propertiesMap >> propertiesMap1

    return parameter1
  }

  def "verify that the location constructor works correctly"() {
    when: 'the constructor is used'
    def reportDetails = new Report(SAMPLE_REPORT)

    then: 'the location is parsed into the folder/name correctly'
    reportDetails.reportFolder == 'reports/sampleBad'
    reportDetails.reportName == 'SampleReport.jrxml'
  }

  @SuppressWarnings("UnusedObject")
  def "verify that the location constructor fails with bad location"() {
    when: 'the constructor is used'
    //noinspection GroovyResultOfObjectAllocationIgnored
    new Report('badReport')

    then: 'an exception is thrown correctly'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['badReport'])
  }

  def "verify that getDefinedReportParameters works correctly"() {
    given: 'a stubbed compiled report'
    def compiledReport = Stub(JasperReport)
    compiledReport.parameters >> [parameterStub('metricName')]

    and: 'the reportDetails with the stubbed report'
    def reportDetails = new Report(SAMPLE_REPORT)
    reportDetails.compiledReport = compiledReport

    when: 'the defined parameters are retrieved'
    def definedReportParameters = reportDetails.definedReportParameters

    then: 'the correct parameter is found'
    definedReportParameters.size() == 1
    definedReportParameters[0].name == 'metricName'
    definedReportParameters[0].valueClass == String
  }

  def "verify that getDefaultParameters works correctly"() {
    given: 'a stubbed compiled report'
    def compiledReport = Stub(JasperReport)

    and: 'the reportDetails with the stubbed report'
    def reportDetails = new Report(SAMPLE_REPORT)
    reportDetails.compiledReport = compiledReport

    and: 'a stubbed report engine to determine the default values from the report'
    def stub = Stub(ReportEngine)
    ReportEngine.instance = stub
    stub.evaluateParameterDefaultValues(reportDetails) >> [metricName: 'defaultABC']

    when: 'the default parameters are retrieved'
    def defaultParameters = reportDetails.defaultParameters

    then: 'the default is used'
    defaultParameters.metricName == 'defaultABC'
  }

  def "verify that getReportParameters works correctly with no controller params value"() {
    given: 'a stubbed compiled report'
    def compiledReport = Stub(JasperReport)
    compiledReport.parameters >> [parameterStub('metricName')]

    and: 'the reportDetails with the stubbed report'
    def reportDetails = new Report(SAMPLE_REPORT)
    reportDetails.compiledReport = compiledReport

    and: 'a stubbed report engine to determine the default values from the report'
    def stub = Stub(ReportEngine)
    ReportEngine.instance = stub
    stub.evaluateParameterDefaultValues(reportDetails) >> [metricName: 'defaultABC']

    when: 'the defined parameters are retrieved'
    def reportParameters = reportDetails.reportParameters

    then: 'the correct parameter is found using the default value'
    reportParameters.size() == 1
    reportParameters[0].name == 'metricName'
    reportParameters[0].format == StringFieldFormat.instance
    reportParameters[0].effectiveValue == 'defaultABC'
  }

  def "verify that getReportParameters works correctly with a controller params value"() {
    given: 'a stubbed compiled report'
    def compiledReport = Stub(JasperReport)
    compiledReport.parameters >> [parameterStub('metricName')]

    and: 'the reportDetails with the stubbed report and a controller params value'
    def reportDetails = new Report(SAMPLE_REPORT)
    reportDetails.compiledReport = compiledReport
    reportDetails.params = [metricName: 'ABC-XYZ']

    and: 'a stubbed report engine to determine the default values from the report'
    def stub = Stub(ReportEngine)
    ReportEngine.instance = stub
    stub.evaluateParameterDefaultValues(reportDetails) >> [metricName: 'defaultABC']

    when: 'the defined parameters are retrieved'
    def reportParameters = reportDetails.reportParameters

    then: 'the correct parameter is found'
    reportParameters.size() == 1
    reportParameters[0].name == 'metricName'
    reportParameters[0].effectiveValue == 'ABC-XYZ'
  }

  def "verify that getReportParameters sorts using the provided sequence custom property from the report"() {
    given: 'a stubbed compiled report with some parameters'
    def compiledReport = Stub(JasperReport)
    compiledReport.parameters >> ([parameterStub('metricName', '237'), parameterStub('alpha1', '37')] as JRParameter[])

    and: 'the reportDetails with the stubbed report and a controller params value'
    def reportDetails = new Report(SAMPLE_REPORT)
    reportDetails.compiledReport = compiledReport

    and: 'a stubbed report engine to determine the default values from the report'
    def stub = Stub(ReportEngine)
    ReportEngine.instance = stub
    stub.evaluateParameterDefaultValues(reportDetails) >> [:]

    when: 'the defined parameters are retrieved'
    def reportParameters = reportDetails.reportParameters

    then: 'the correct parameter is found using the default value'
    reportParameters.size() == 2
    reportParameters[0].name == 'alpha1'
    reportParameters[0].sequence == 37
    reportParameters[1].name == 'metricName'
    reportParameters[1].sequence == 237
  }

  def "verify that getReportParameters assigns correct default sequences when no custom property found"() {
    given: 'a stubbed compiled report with some parameters'
    def compiledReport = Stub(JasperReport)
    compiledReport.parameters >> ([parameterStub('endDateTime'),
                                   parameterStub('startDateTime'),
                                   parameterStub('reportTimeInterval'),
                                   parameterStub('metricName')] as JRParameter[])

    and: 'the reportDetails with the stubbed report and a controller params value'
    def reportDetails = new Report(SAMPLE_REPORT)
    reportDetails.compiledReport = compiledReport

    and: 'a stubbed report engine to determine the default values from the report'
    def stub = Stub(ReportEngine)
    ReportEngine.instance = stub
    stub.evaluateParameterDefaultValues(reportDetails) >> [:]

    when: 'the defined parameters are retrieved'
    def reportParameters = reportDetails.reportParameters

    then: 'the correct parameter is found using the default value'
    reportParameters.size() == 4
    reportParameters[0].name == 'metricName'
    reportParameters[1].name == 'reportTimeInterval'
    reportParameters[2].name == 'startDateTime'
    reportParameters[3].name == 'endDateTime'
  }

  def "verify that getReportParameters gracefully handles non-numeric sequences "() {
    given: 'a stubbed compiled report with some parameters'
    def compiledReport = Stub(JasperReport)
    compiledReport.parameters >> ([parameterStub('metricName', 'xyz'), parameterStub('alpha1', '37')] as JRParameter[])

    and: 'the reportDetails with the stubbed report and a controller params value'
    def reportDetails = new Report(SAMPLE_REPORT)
    reportDetails.compiledReport = compiledReport

    and: 'a stubbed report engine to determine the default values from the report'
    def stub = Stub(ReportEngine)
    ReportEngine.instance = stub
    stub.evaluateParameterDefaultValues(reportDetails) >> [:]

    when: 'the defined parameters are retrieved'
    def reportParameters = reportDetails.reportParameters

    then: 'the correct parameter is found using the default value'
    reportParameters.size() == 2
    reportParameters[0].name == 'alpha1'
    reportParameters[0].sequence == 37
    reportParameters[1].name == 'metricName'
    reportParameters[1].sequence == 50
  }

  def "verify that buildBaseParametersForReport builds the correct values for resource bundle and base dir"() {
    given: 'a ReportDetails with the folder and resource names filled in'
    // Needs to use a report sample report so the resource bundle can be found
    def reportDetails = new Report("reports/sample/SampleReport.jrxml", [resourceBundleName: 'sample_report'])

    when: 'the buildBaseParametersForReport builds the parameter values'
    def params = reportDetails.buildBaseParametersForReport()

    then: 'the right parameter values are used'
    params.BaseDir == reportDetails.reportFolder
    ResourceBundle resourceBundle = (ResourceBundle) params.REPORT_RESOURCE_BUNDLE
    resourceBundle.baseBundleName == "reports/sample/sample_report"
    params.REPORT_LOCALE == GlobalUtils.requestLocale
    params.REPORT_MAX_COUNT == EFrameConfiguration.REPORT_ROW_COUNT
  }

  def "verify that buildBaseParametersForReport uses the current locale correctly"() {
    given: 'a ReportDetails with the folder and resource names filled in'
    // Needs to use a report sample report so the resource bundle can be found
    def reportDetails = new Report("reports/sample/SampleReport.jrxml", [resourceBundleName: 'sample_report'])

    and: 'a simulated request locale'
    GlobalUtils.defaultLocale = Locale.GERMANY

    when: 'the buildBaseParametersForReport builds the parameter values'
    def params = reportDetails.buildBaseParametersForReport()

    then: 'the right parameter values are used'
    params.REPORT_LOCALE == Locale.GERMANY

    cleanup: 'the default request locale is reset'
    GlobalUtils.defaultLocale = Locale.US
  }

  def "verify that the constructor converts simple relative directory paths as expected"() {
    when: 'the report details are built'
    def reportDetails = new Report("reports/sample/detail/../SampleReport.jrxml")

    then: 'the path is adjusted without the relative directory'
    reportDetails.reportFolder == 'reports/sample'
  }

  def "verify that buildBaseParametersForReport handles simple relative directory paths for the resource bundle"() {
    given: 'a ReportDetails with the folder and resource names filled in'
    // Needs to use a report sample report so the resource bundle can be found
    def reportDetails = new Report("reports/sample/detail/../SampleReport.jrxml", [resourceBundleName: 'sample_report'])

    when: 'the buildBaseParametersForReport builds the parameter values'
    def params = reportDetails.buildBaseParametersForReport()

    then: 'the right parameter values are used'
    ResourceBundle resourceBundle = (ResourceBundle) params.REPORT_RESOURCE_BUNDLE
    resourceBundle.baseBundleName == "reports/sample/sample_report"
  }

  def "verify that getSubReports detects valid sub-report parameters"() {
    given: 'a stubbed compiled report with some parameters'
    def compiledReport = Stub(JasperReport)
    compiledReport.parameters >> ([parameterStub('SubReportABC', '$P{BaseDir}+"reports/sampleBad/SampleSub.jrxml"', '1')] as JRParameter[])

    and: 'the reportDetails with the stubbed report and a controller params value'
    def reportDetails = new Report(SAMPLE_REPORT)
    reportDetails.compiledReport = compiledReport

    when: 'the sub-reports are retrieved'
    def subReports = reportDetails.subReports

    then: 'the correct sub-reports are found'
    subReports == [SubReportABC: 'reports/sampleBad/SampleSub.jrxml']
  }

  def "verify that getSubReports gracefully handles bad default expression for the parameter - missing quotes"() {
    given: 'a stubbed compiled report with some parameters'
    def compiledReport = Stub(JasperReport)
    compiledReport.parameters >> ([parameterStub('SubReportABC', '$P{BaseDir}+reports/sampleBad/SampleSub.jrxml"', '1')] as JRParameter[])

    and: 'the reportDetails with the stubbed report and a controller params value'
    def reportDetails = new Report(SAMPLE_REPORT)
    reportDetails.compiledReport = compiledReport

    when: 'the sub-reports are retrieved'
    reportDetails.subReports

    then: 'a valid exception is triggered'
    def ex = thrown(Exception)
    //sub-report parameter $name does not have a string in the default expression: '$expression'
    UnitTestUtils.assertExceptionIsValid(ex, ['SubReportABC', 'sampleBad', 'parameter', 'expression'])
  }

  def "verify that getEffectiveRowLimit uses the configured value from application yml"() {
    given: 'a ReportDetails for a report'
    def reportDetails = new Report(SAMPLE_REPORT)

    and: 'the config value is set'
    Holders.configuration.report.rowLimit = 9

    expect: 'the correct value is used'
    reportDetails.effectiveRowLimit == 9

    cleanup: 'the config value is reset'
    Holders.configuration.report.rowLimit = EFrameConfiguration.REPORT_ROW_COUNT
  }

  def "verify that getEffectiveRowLimit uses default row limit"() {
    given: 'a ReportDetails '
    def reportDetails = new Report("reports/sample/SampleReport.jrxml")

    expect: 'the buildBaseParametersForReport builds the parameter values'
    reportDetails.getEffectiveRowLimit() == EFrameConfiguration.REPORT_ROW_COUNT
  }

  def "verify that determineReportParams uses URL parameters from user preferences has that same parameter value"() {
    given: 'some report params stored in the user preferences'
    new MockPreferenceHolder(this, [new SimpleStringPreference(key: 'metricName', value: 'ABC-XYZ')]).install()

    and: 'a report with the parameter is passed on the URL'
    def report = new Report([:], new MockPrincipal())

    when: 'the effective params are determined'
    def params = report.determineReportParams([loc: SAMPLE_REPORT, metricName: 'ABC-PDQ'])

    then: 'the params from the URL are used'
    params.metricName == 'ABC-PDQ'
  }

  def "verify that determineReportParams uses values from user preferences when not passed on report URL"() {
    given: 'some report params stored in the user preferences'
    new MockPreferenceHolder(this, [new SimpleStringPreference(key: 'metricName', value: 'ABC-XYZ')]).install()

    and: 'a report with with no parameter passed on the URL'
    def report = new Report([loc: SAMPLE_REPORT], new MockPrincipal())

    when: 'the effective params are determined'
    def params = report.determineReportParams([:])

    then: 'the params from the user preference are used'
    params.metricName == 'ABC-XYZ'
  }


}
