/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports.controller

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import net.sf.jasperreports.engine.JRParameter
import net.sf.jasperreports.engine.JRPropertiesMap
import net.sf.jasperreports.engine.JasperReport
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.reports.Report
import org.simplemes.eframe.reports.ReportEngine
import org.simplemes.eframe.reports.ReportHelper
import org.simplemes.eframe.reports.ReportResourceHandler
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockPreferenceHolder
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.MockRenderer
import org.simplemes.eframe.test.UnitTestUtils
import spock.lang.Shared

/**
 * Tests.
 */
class ReportControllerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  /**
   * The dummy sample report.
   */
  public static final String SAMPLE_REPORT = "reports/sample/SampleReport.jrxml"

  @Shared
  def controller

  def setupSpec() {
    controller = new ReportController()
  }

  void cleanup() {
    ReportHelper.instance = new ReportHelper()
    ReportEngine.instance = new ReportEngine()
  }

  /**
   * Utility method to build a stub for the JRParameter for unit tests.
   * @param name The parameter name.
   * @param valueClass The Class for this parameter (default: String)
   * @return The stubbed parameter.
   */
  Object parameterStub(String name, Class valueClass = String) {
    def propertiesMap1 = Stub(JRPropertiesMap)
    propertiesMap1.getProperty('sequence') >> null
    def parameter1 = Stub(JRParameter)
    parameter1.forPrompting >> true
    parameter1.systemDefined >> false
    parameter1.name >> name
    parameter1.valueClass >> valueClass
    parameter1.propertiesMap >> propertiesMap1

    return parameter1
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that index calls the report engine correctly"() {
    given: 'a mock helper'
    def mock = Mock(ReportEngine)
    ReportEngine.instance = mock

    when: 'the index method is called'
    def res = new ReportController().index(mockRequest(), new MockPrincipal())

    then: 'the engine is called correctly'
    //    def missingRoles = ReportEngine.instance.writeReport(report,out)
    1 * mock.writeReport(_ as Report, _ as Writer)

    and: 'the response is good'
    res.status == HttpStatus.OK
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that index calls the report engine correctly - PDF format"() {
    given: 'a mock helper'
    def mock = Mock(ReportEngine)
    ReportEngine.instance = mock

    when: 'the index method is called'
    def res = new ReportController().index(mockRequest([format: 'pdf']), new MockPrincipal())

    then: 'the engine is called correctly'
    1 * mock.writeReport(_ as Report, _ as OutputStream)

    and: 'the response is good'
    res.status == HttpStatus.OK
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that index handles missing roles correctly"() {
    given: 'a mock engine'
    def mock = Mock(ReportEngine)
    ReportEngine.instance = mock

    and: 'a mock renderer'
    def mockRenderer = new MockRenderer(this).install()

    when: 'the index method is called'
    def res = new ReportController().index(mockRequest(accept: MediaType.TEXT_HTML), new MockPrincipal())

    then: 'the engine is called correctly'
    1 * mock.writeReport(_ as Report, _ as Writer) >> 'GIBBERISH'

    and: 'the response is good'
    res.status == HttpStatus.OK

    and: 'the renderer has the right view and msg'
    mockRenderer.view == 'home/denied'
    mockRenderer.model[StandardModelAndView.FLASH].contains('GIBBERISH')
  }

  def "verify that image provides images from the resource cache"() {
    given: 'some test image data'
    def data = "ABC-DEF".bytes

    and: 'a report resource handler to test the cache'
    def handler = new ReportResourceHandler('/path/reports')
    handler.handleResource(image, data)

    when: 'the image is retrieved for display in the browser with a get'
    def res = new ReportController().image(mockRequest([image: "/path/reports_${imageGet}"]), new MockPrincipal())

    then: 'the response is correct'
    if (contentType) {
      res.status == HttpStatus.OK
      res.body().toString() == 'ABC-DEF'
    } else {
      res.status == HttpStatus.NOT_FOUND
    }

    where:
    image         | imageGet      | contentType
    'image1.png'  | 'image1.png'  | 'image/png'
    'image1.jpg'  | 'image1.jpg'  | 'image/jpeg'
    'image1.jpeg' | 'image1.jpeg' | 'image/jpeg'
    'image1.jpg'  | 'image1.bad'  | null
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that getTaskMenuItems uses the reports found by the report engine for the menu items"() {
    given: 'a mock helper with some built-in reports'
    def originalHelper = new ReportHelper()
    def stub = Stub(ReportHelper)
    ReportHelper.instance = stub
    stub.determineBuiltinReports() >> ['reports/Metrics.jrxml', 'reports/AReport.jrxml']

    and: 'a stubbed method that delegates to the non-stubbed determineReportBaseName method'
    stub.determineReportBaseName(_) >> { String path -> originalHelper.determineReportBaseName(path) }

    when: 'the task menu items are checked'
    def taskMenuItems = controller.taskMenuItems

    then: 'the correct reports are found'
    taskMenuItems.size() == 2

    and: 'the first one is correct'
    def item1 = taskMenuItems.find { it.name == 'AReport' }
    item1.folder == 'reports:5000'
    item1.uri == '/report?loc=reports/AReport.jrxml'
    item1.displayOrder == 5001

    and: 'the second one is correct'
    def item2 = taskMenuItems.find { it.name == 'Metrics' }
    item2.folder == 'reports:5000'
    item2.uri == '/report?loc=reports/Metrics.jrxml'
    item2.displayOrder == 5002
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that filter builds the right model and renders the correct view"() {
    given: 'a stubbed compiled report with some parameters'
    def compiledReport = Stub(JasperReport)
    compiledReport.parameters >> ([parameterStub('metricName')] as JRParameter[])

    and: 'a stubbed report engine to build a stubbed compiled report'
    def stub = Mock(ReportEngine)
    ReportEngine.instance = stub
    stub.compile(_ as Report) >> { args ->
      args[0].compiledReport = compiledReport
      args[0]
    }

    and: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    when: 'the filter page is triggered'
    def params = [loc: SAMPLE_REPORT, metricName: 'ABC-XYZ']
    def res = new ReportController().filter(mockRequest(params), new MockPrincipal())
    def model = mock.model

    then: 'the correct model is used'
    model.reportName == 'SampleReport'
    model.loc == SAMPLE_REPORT
    model.reportFields.value.size() == 1
    model.reportFields.value.metricName.effectiveValue == 'ABC-XYZ'
    model.reportTimeFound == false
    model.reportFilterValues.loc == SAMPLE_REPORT

    and: 'the correct view is rendered'
    mock.view == 'report/filter'

    and: 'the status is OK'
    res.status == HttpStatus.OK
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that filter detects the special reportTimeInterval field when present"() {
    given: 'a stubbed compiled report with some parameters'
    def compiledReport = Stub(JasperReport)
    def parameter1 = parameterStub('reportTimeInterval')
    def parameter2 = parameterStub('startDateTime', Date)
    def parameter3 = parameterStub('endDateTime', Date)
    compiledReport.parameters >> ([parameter1, parameter2, parameter3] as JRParameter[])

    and: 'a stubbed report engine to build a stubbed compiled report'
    def stub = Stub(ReportEngine)
    ReportEngine.instance = stub
    stub.compile(_ as Report) >> { args ->
      args[0].compiledReport = compiledReport
      args[0]
    }

    and: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    when: 'the filter page is triggered'
    def params = [loc: SAMPLE_REPORT]
    new ReportController().filter(mockRequest(params), new MockPrincipal())
    def model = mock.model

    then: 'report time field is detected'
    model.reportTimeFound == true

    and: 'the detected date time fields are correct'
    model.dateTimeParams == 'startDateTime,endDateTime'
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that filter does not detect the special reportTimeInterval field when start and end dateTime are not parameters"() {
    given: 'a stubbed compiled report with some parameters'
    def compiledReport = Stub(JasperReport)
    def parameter1 = parameterStub('reportTimeInterval')
    def parameter2 = parameterStub('startDateTimeX', Date)
    def parameter3 = parameterStub('endDateTime', Date)
    compiledReport.parameters >> ([parameter1, parameter2, parameter3] as JRParameter[])

    and: 'a stubbed report engine to build a stubbed compiled report'
    def stub = Stub(ReportEngine)
    ReportEngine.instance = stub
    stub.compile(_ as Report) >> { args ->
      args[0].compiledReport = compiledReport
      args[0]
    }

    and: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    when: 'the filter page is triggered'
    def params = [loc: SAMPLE_REPORT]
    new ReportController().filter(mockRequest(params), new MockPrincipal())
    def model = mock.model

    then: 'report time field is not detected'
    model.reportTimeFound == false

    and: 'the detected date time fields are correct'
    model.dateTimeParams == 'startDateTimeX,endDateTime'
  }

  def "verify that filterUpdate redirects to the report page with the correct parameters"() {
    given: 'Some dates with no milliseconds'
    def date1 = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    def date2 = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS + 3600000)

    and: 'a mock preference holder'
    new MockPreferenceHolder(this, []).install()

    and: 'the locale is set for the test'
    GlobalUtils.defaultLocale = locale

    when: 'the filterUpdate page is triggered'
    def params = [loc               : SAMPLE_REPORT,
                  reportTimeInterval: 'THIS_YEAR',
                  startDateTime     : DateFieldFormat.instance.formatForm(date1, locale, null),
                  endDateTime       : DateFieldFormat.instance.formatForm(date2, locale, null),
                  dateTimeParams    : 'startDateTime,endDateTime']
    def res = new ReportController().filterUpdate(mockRequest(), params, new MockPrincipal())

    then: 'redirect is correct'
    res.status == HttpStatus.FOUND
    def url = URLDecoder.decode(res.headers.get(HttpHeaders.LOCATION), 'utf-8')
    url.startsWith("/report?")

    and: 'the redirect has the correct parameters'
    url.contains("loc=${SAMPLE_REPORT}")
    url.contains("reportTimeInterval=THIS_YEAR")
    url.contains("startDateTime=${ISODate.format(date1)}")
    url.contains("endDateTime=${ISODate.format(date2)}")

    and: 'the non-report parameters are removed'
    !url.contains("dateTimeParams=")
    !url.contains("updateReport=")
    !url.contains("controller=")
    !url.contains("format=")
    !url.contains("action=")

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that filterUpdate gracefully handles bad date format"() {
    when: 'the filterUpdate page is triggered with bad dates'
    def params = [loc           : SAMPLE_REPORT,
                  endDateTime   : 'gibberish',
                  dateTimeParams: 'endDateTime']
    new ReportController().filterUpdate(mockRequest(), params, new MockPrincipal())

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['Unparseable', 'gibberish'])
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that filterUpdate saves the parameters in a user preference for the next display"() {
    given: 'a stubbed compiled report with some parameters'
    def compiledReport = Stub(JasperReport)
    def parameter1 = parameterStub('reportTimeInterval')
    def parameter2 = parameterStub('metricName')
    def parameter3 = parameterStub('endDateTime', Date)
    def parameter4 = parameterStub('unusedParam')
    compiledReport.parameters >> ([parameter1, parameter2, parameter3, parameter4] as JRParameter[])

    and: 'a stubbed report engine to build a stubbed compiled report'
    def stub = Stub(ReportEngine)
    ReportEngine.instance = stub
    stub.compile(_ as Report) >> { args ->
      args[0].compiledReport = compiledReport
      args[0]
    }

    and: 'a date with no milliseconds'
    def date1 = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)

    and: 'the locale is set for the test'
    GlobalUtils.defaultLocale = locale

    and: 'a mock preference holder'
    def mockPreferenceHolder = new MockPreferenceHolder(this, []).install()

    when: 'the filterUpdate is triggered'
    def params = [loc               : SAMPLE_REPORT,
                  format            : 'html',
                  metricName        : 'ABC-XYZ',
                  reportTimeInterval: 'THIS_YEAR',
                  endDateTime       : DateFieldFormat.instance.formatForm(date1, locale, null),
                  dateTimeParams    : 'endDateTime']
    new ReportController().filterUpdate(mockRequest(), params, new MockPrincipal())


    then: 'the settings are stored by the holder'
    mockPreferenceHolder['metricName'].value == 'ABC-XYZ'
    mockPreferenceHolder['reportTimeInterval'].value == 'THIS_YEAR'
    mockPreferenceHolder['endDateTime'].value == ISODate.format(date1)

    and: 'the unused values are not persisted'
    mockPreferenceHolder['unusedParam'] == null

    and: 'the report loc parameter is not saved'
    mockPreferenceHolder['loc'] == null
    mockPreferenceHolder['format'] == null

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

}
