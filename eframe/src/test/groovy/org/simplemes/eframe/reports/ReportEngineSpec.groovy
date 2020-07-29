/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports

import ch.qos.logback.classic.Level
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import net.sf.jasperreports.engine.JasperReport
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.HTMLTestUtils
import org.simplemes.eframe.test.MockPreferenceHolder
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.MockSecurityUtils
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum

import java.sql.Timestamp

/**
 * Tests report engine access to external report engine.
 */
class ReportEngineSpec extends BaseSpecification {

  /**
   * The sample report.
   */
  static final String SAMPLE_REPORT = "reports/sample/SampleReport.jrxml"

  /**
   * The sample report with no parameters.
   */
  static final String SAMPLE_REPORT_NO_PARAM = "reports/sample/SampleReportNoParam.jrxml"

  /**
   * The sample report that has hyperlinks and barcodes.
   */
  static final String SAMPLE_PARENT_REPORT = "reports/sample/SampleParent.jrxml"

  /**
   * A sample report that has a sub-report.
   */
  static final String SAMPLE_WITH_SUB_REPORT = "reports/sample/SampleMaster.jrxml"

  /**
   * The sample report folder.
   */
  static final String SAMPLE_REPORT_FOLDER = "reports/sample"

  def setup() {
    // Mock a security utils that allows all access.
    new MockSecurityUtils(this, HttpStatus.OK, true).install()

    // Most tests will need empty preferences
    new MockPreferenceHolder(this, []).install()
  }

  def "verify that compile works"() {
    when: 'the report is compiled'
    def reportDetails = ReportEngine.instance.compile(new Report(SAMPLE_REPORT))

    then: 'the report is compiled'
    reportDetails.compiledReport instanceof JasperReport

    and: 'the resource bundle name requested by the report is populated'
    reportDetails.resourceBundleName == 'sample_report'
  }

  def "verify that compile fails with bad report spec"() {
    when: 'the bad report is compiled'
    ReportEngine.instance.compile(new Report(SAMPLE_REPORT + "bad"))

    then: 'an exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['find', SAMPLE_REPORT, 'bad', '110'])
  }

  def "verify that writeReport fails with missing loc"() {
    when: 'the bad report is compiled'
    def report = new Report([:], new MockPrincipal())
    report.data = []
    ReportEngine.instance.compile(report)
    ReportEngine.instance.fill(report)
    ReportEngine.instance.writeReport(report, new StringWriter())

    then: 'an exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['loc', 'null'])
  }

  def "verify that fill works"() {
    given: 'a mocked data source'
    def params = [loc: SAMPLE_REPORT]
    def report = new Report(params, new MockPrincipal())
    def data = []
    for (i in 0..10) {
      data << [metric_name: 'ABC', date_created: new Timestamp(System.currentTimeMillis()), average: 12.6, sample_count: (long) i]
    }
    report.data = data

    and: 'a compiled report'
    ReportEngine.instance.compile(report)

    when: 'the report is filled'
    ReportEngine.instance.fill(report)

    then: 'the report has the right data in it'
    report.filledReport.pages.size() == 1
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that writeReport works with simple requests - live report engine use on real report"() {
    given: 'a mocked data source'
    def report = new Report([loc: SAMPLE_REPORT], new MockPrincipal())
    report.data = [[name: 'ABC-XYZ', date_created: new Timestamp(System.currentTimeMillis()), title: 'ABC-title']]

    and: 'the controller has a report parameter in it and the location parameter'
    [loc: SAMPLE_REPORT].MetricName = 'Dummy.method247'
    [loc: SAMPLE_REPORT].loc = SAMPLE_REPORT

    when: 'the write report method is called'
    def writer = new StringWriter()
    ReportEngine.instance.writeReport(report, writer)

    then: 'the stubbed HTML output is used'
    writer.toString().contains('ABC-XYZ')
  }

  def "verify that exportToHTML works with one page report"() {
    given: 'a compiled/filled report'
    Report report = ReportEngine.instance.compile(new Report(SAMPLE_REPORT))
    report.data = [[name: 'ABC-XYZ', date_created: new Timestamp(System.currentTimeMillis()), title: 'ABC-title']]
    ReportEngine.instance.fill(report)

    when: 'the HTML is exported'
    def writer = new StringWriter()
    ReportEngine.instance.exportReportToHTML(report, writer)

    then: 'the output type is set to HTML'
    report.contentType == MediaType.TEXT_HTML

    and: 'the data is used in the report'
    def page = writer.toString()
    page.contains('ABC-XYZ')

    and: 'the HTML is closed out correctly - simple check'
    page.contains('</html>')
    page.contains('</body>')

    and: 'the images are handled correctly'
    page.contains("""<img src="/report/image?image=${SAMPLE_REPORT_FOLDER}_img_0_0_0.jpg""")
    page.contains("""<img src="/report/image?image=${SAMPLE_REPORT_FOLDER}_img_0_0_3.png""")

    and: 'the right CSS is used in the page'
    page.contains("""<link rel="stylesheet" href="/assets/eframe.css?compile=false"/>""")

    and: 'the filter panel is displayed'
    page.contains('<div id="ReportHeader" class="report-header">')
  }

  def "verify that exportToHTML displays a message when no data found"() {
    given: 'a compiled/filled report'
    Report report = ReportEngine.instance.compile(new Report(SAMPLE_REPORT))
    report.data = []
    ReportEngine.instance.fill(report)

    when: 'the HTML is exported'
    def writer = new StringWriter()
    ReportEngine.instance.exportReportToHTML(report, writer)

    then: 'the not data found message is displayed'
    def page = writer.toString()
    page.contains(lookup('report.noDataFound.message'))
  }

  def "verify that exportToHTML builds a pager section"() {
    given: 'a compiled/filled multi-page report'
    Report report = ReportEngine.instance.compile(new Report(SAMPLE_REPORT))
    def data = []
    def timestamp = new Timestamp(System.currentTimeMillis())
    for (i in 0..100) {
      data << [name: "ABC-$i".toString(), date_created: timestamp, title: "ABC-title"]
    }
    report.data = data
    ReportEngine.instance.fill(report)

    when: 'the HTML is exported'
    def writer = new StringWriter()
    ReportEngine.instance.exportReportToHTML(report, writer)

    then: 'the data is used in the report'
    def page = writer.toString()
    page.contains('ABC-27')
    !page.contains('ABC-87')

    and: 'the header is correct'
    page.contains('<link rel="stylesheet" href="/assets/eframe.css?compile=false"/>')

    and: 'the pager is correct'
    page.contains('<div id="pagination">')
    def linkText = TextUtils.findLine(page, 'page=2')
    //http://localhost:8090/report?loc=reports/MetricSummary.jrxml&format=pdf&metricName=%&endDateTime=2018-08-26T17:14:43.273Z&reportTimeInterval=TODAY&startDateTime=1970-01-01T00:00:00.100Z
    linkText.contains("report?loc=${SAMPLE_REPORT}&amp;")
    linkText.contains('class="step')

    and: 'the page links contain the parameters used'
    linkText.contains('&amp;name=%')
    linkText.contains('&amp;reportTimeInterval=TODAY')
  }

  def "verify that fill uses the parameters passed in"() {
    given: 'the parameter used in the report'

    and: 'a compiled/filled multi-page report'
    def params = [name: 'ABC-XYZ', loc: SAMPLE_REPORT]
    def report = new Report(params, new MockPrincipal())
    ReportEngine.instance.compile(report)
    def data = []
    def timestamp = new Timestamp(System.currentTimeMillis())
    for (i in 0..10) {
      data << [name: "ABC-$i".toString(), date_created: timestamp, title: 'ABC-title']
    }
    report.data = data
    ReportEngine.instance.fill(report)

    when: 'the HTML is exported'
    def writer = new StringWriter()
    ReportEngine.instance.exportReportToHTML(report, writer)

    then: 'the parameter is used in the report'
    def page = writer.toString()
    page.contains('Parameter: ABC-XYZ')

    and: 'the filter panel has the parameters/values displayed'
    page.contains("${lookup('name.label') - '.label'}: ABC-XYZ")

    and: 'the filter panel has the change filter hyperlink'
    def filerText = TextUtils.findLine(page, 'report/filter?loc')
    filerText.contains("""&amp;name=ABC-XYZ""")
    filerText.contains(""">${lookup('changeFilter.label')}</a>""")

    and: 'the filter panel has the PDF output hyperlink'
    def pdfLink = TextUtils.findLine(page, '&amp;format=pdf')
    def pdfHref = "/report?loc=${SAMPLE_REPORT}&amp;format=pdf"
    pdfLink.contains("""<a href="${pdfHref}""")
    pdfLink.contains(">${lookup('pdf.label')}<")

    and: 'the filter panel has a home page link'
    page.contains("""<a href="/" class="report-filter-action">""")
  }

  def "verify that fill uses the default parameters if none passed in"() {
    given: 'a compiled/filled multi-page report'
    Report report = ReportEngine.instance.compile(new Report(SAMPLE_REPORT))
    def data = []
    def timestamp = new Timestamp(System.currentTimeMillis())
    for (i in 0..10) {
      data << [name: "ABC-$i".toString(), date_created: timestamp, title: 'ABC-title']
    }
    report.data = data
    ReportEngine.instance.fill(report)

    when: 'the HTML is exported'
    def writer = new StringWriter()
    ReportEngine.instance.exportReportToHTML(report, writer)

    then: 'the default parameter value is used in the report'
    def page = writer.toString()
    page.contains('Parameter: %')

    and: 'the filter panel has the parameters/values displayed'
    page.contains("${lookup('name.label') - '.label'}: %")
  }

  def "verify that fill works if no parameters are defined"() {
    given: 'a compiled/filled multi-page report'
    Report report = ReportEngine.instance.compile(new Report(SAMPLE_REPORT_NO_PARAM))
    def data = []
    def timestamp = new Timestamp(System.currentTimeMillis())
    for (i in 0..10) {
      data << [metric_name: "ABC-$i".toString(), date_created: timestamp, average: 12.6, sample_count: (long) i]
    }
    report.data = data
    ReportEngine.instance.fill(report)

    when: 'the HTML is exported'
    def writer = new StringWriter()
    ReportEngine.instance.exportReportToHTML(report, writer)

    then: 'no parameters are shown'
    def page = writer.toString()
    !page.contains("""<span id="filterValues">""")

    and: 'no change filter link is shown'
    !page.contains("""<a href="report/filter?""")
  }

  def "verify that fill uses the reportTimeInterval if passed in"() {
    given: 'the parameter used in the report'
    def params = [metricName: 'ABC-XYZ', reportTimeInterval: 'TODAY', loc: SAMPLE_REPORT]

    and: 'a compiled/filled report'
    Report report = ReportEngine.instance.compile(new Report(params, new MockPrincipal()))
    def data = []
    def timestamp = new Timestamp(System.currentTimeMillis())
    for (i in 0..10) {
      data << [metric_name: "ABC-$i".toString(), date_created: timestamp, average: 12.6, sample_count: (long) i]
    }
    report.data = data
    ReportEngine.instance.fill(report)

    when: 'the HTML is exported'
    def writer = new StringWriter()
    ReportEngine.instance.exportReportToHTML(report, writer)

    then: 'the reportTimeInterval is used to set the start/end date times'
    def page = writer.toString()
    def dateRange = ReportTimeIntervalEnum.TODAY.determineRange(report.reportDate)
    def startDate = dateRange.start.toString()
    def endDate = dateRange.end.toString()
    page.contains("Start: $startDate")
    page.contains("End: $endDate")
  }

  def "verify that fill uses the specific start and end dates if reportTimeInterval is custom"() {
    given: 'the start/end dates to use'
    def startDate = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    def endDate = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS + 3600000)

    and: 'the parameter used in the report'
    def params = [:]
    params.metricName = 'ABC-XYZ'
    params.reportTimeInterval = 'CUSTOM_RANGE'
    params.startDateTime = ISODate.format(startDate)
    params.endDateTime = ISODate.format(endDate)
    params.loc = SAMPLE_REPORT

    and: 'a compiled/filled report'
    Report report = ReportEngine.instance.compile(new Report(params, new MockPrincipal()))
    def data = []
    def timestamp = new Timestamp(System.currentTimeMillis())
    for (i in 0..10) {
      data << [metric_name: "ABC-$i".toString(), date_created: timestamp, average: 12.6, sample_count: (long) i]
    }
    report.data = data
    ReportEngine.instance.fill(report)

    when: 'the HTML is exported'
    def writer = new StringWriter()
    ReportEngine.instance.exportReportToHTML(report, writer)

    then: 'the reportTimeInterval is used to set the start/end date times'
    def page = writer.toString()
    page.contains("${lookup('startDateTime.label')}: ${DateUtils.formatDate(startDate)}")
    page.contains("${lookup('endDateTime.label')}: ${DateUtils.formatDate(endDate)}")
  }

  def "verify that exportToPDF works with one page report"() {
    given: 'disabled WARN message from library'
    LogUtils.getLogger('net.sf.jasperreports.engine.export.PdfGlyphRenderer').level = Level.ERROR

    and: 'a compiled/filled report'
    Report report = ReportEngine.instance.compile(new Report(SAMPLE_REPORT))
    report.data = [[metric_name: 'ABC-XYZ', date_created: new Timestamp(System.currentTimeMillis()), average: 12.6, sample_count: 255L]]
    ReportEngine.instance.fill(report)

    when: 'the report is exported'
    def outStream = new ByteArrayOutputStream()
    ReportEngine.instance.exportReportToPDF(report, outStream)

    then: 'the output type is set to PDF'
    report.contentType == "application/pdf"

    and: 'the data is used in the report'
    def page = outStream.toString()
    //println "page = $page"
    page.contains('%PDF-')
  }

  def "verify that buildHTMLHeader builds the right header"() {
    given: 'a compiled report'
    def reportDetails = ReportEngine.instance.compile(new Report(SAMPLE_REPORT))

    when: 'the header is built'
    def header = ReportEngine.instance.buildHTMLHeader(reportDetails)

    then: 'the header contains the framework CSS'
    header.contains('<link rel="stylesheet" href="/assets/eframe.css?compile=false"/>')

    and: 'contains the basic header elements'
    UnitTestUtils.assertContainsAllIgnoreCase(header, ['html', 'head', 'body'])

    and: 'the full HTML is valid'
    def footer = """</td></tr>
                                   </table>
                                   </body>
                                   </html>"""
    HTMLTestUtils.checkHTML(header + footer, ['<!DOCTYPE', '&nbsp;'])

    and: 'the HTML language is set to the default locale'
    def htmlTagText = TextUtils.findLine(header, '<html')
    htmlTagText.contains(""" lang="${Locale.default.language}""")
  }

  def "verify that checkForMissingRoles works when user does not have the role"() {
    given: 'the compiled report'
    def params = [loc: SAMPLE_REPORT]
    params.loc = SAMPLE_REPORT

    and: 'a compiled/filled report'
    def principal = new MockPrincipal()
    Report report = ReportEngine.instance.compile(new Report(params, principal))

    and: 'a mocked security engine that always fails the role check'
    def securityUtils = Stub(SecurityUtils)
    securityUtils.isAllGranted('ADMIN', principal) >> false
    SecurityUtils.instance = securityUtils

    expect: 'the missing role is detected'
    ReportEngine.instance.checkForMissingRoles(report) == 'ADMIN'

    cleanup:
    SecurityUtils.instance = new SecurityUtils()
  }

  def "verify that writeReport returns the missing roles when user does not have the role"() {
    given: 'the compiled report'
    def params = [loc: SAMPLE_REPORT]
    params.loc = SAMPLE_REPORT

    and: 'a compiled/filled report'
    def principal = new MockPrincipal()
    Report report = new Report(params, principal)

    and: 'a mocked security engine that always fails the role check'
    def securityUtils = Stub(SecurityUtils)
    securityUtils.isAllGranted('ADMIN', principal) >> false
    SecurityUtils.instance = securityUtils

    expect: 'the missing role is detected'
    ReportEngine.instance.writeReport(report, new StringWriter()) == 'ADMIN'

    cleanup:
    SecurityUtils.instance = new SecurityUtils()
  }

  def "verify that buildHTMLHeader the PDF link with the correct formatted arguments"() {
    given: 'a compiled report'
    def reportDetails = ReportEngine.instance.compile(new Report(SAMPLE_REPORT))
    reportDetails.params = [startDateTime: UnitTestUtils.SAMPLE_ISO_TIME_STRING]

    when: 'the header is built'
    def header = ReportEngine.instance.buildHTMLHeader(reportDetails)

    then: 'the PDF link has the right formatted date/times'
    def pdfLink = TextUtils.findLine(header, '&amp;format=pdf')
    pdfLink.contains("reportTimeInterval=TODAY")
  }

  def "verify that buildHTMLHeader creates the filter panel with the correct parameters"() {
    given: 'a compiled report'
    def params = [startDateTime: UnitTestUtils.SAMPLE_ISO_TIME_STRING,
                  name         : 'ABC-XYZ',
                  loc          : SAMPLE_REPORT]
    def report = ReportEngine.instance.compile(new Report(params, new MockPrincipal()))

    when: 'the header is built'
    def header = ReportEngine.instance.buildHTMLHeader(report)

    then: 'the change filter link has the right formatted date/times and other params'
    def changeFilterLink = TextUtils.findLine(header, 'report/filter')
    changeFilterLink.contains("reportTimeInterval=TODAY")
    changeFilterLink.contains("name=ABC-XYZ")
    changeFilterLink.contains("startDateTime=${UnitTestUtils.SAMPLE_ISO_TIME_STRING}")
    changeFilterLink.contains("endDateTime=")

    and: 'the header section displays the parameters in the correct format'
    def paramsText = HTMLTestUtils.extractTag(header, '<div id="ReportHeader"', true)
    paramsText.contains("${lookup('name.label') - '.label'}: ABC-XYZ")
    paramsText.contains("${lookup('reportTimeInterval.label') - '.label'}: TODAY")
  }

  def "verify that the filter panel shows the start and end dates if the report interval is custom"() {
    given: 'a compiled report'
    def reportDetails = ReportEngine.instance.compile(new Report(SAMPLE_REPORT))
    reportDetails.params = [startDateTime     : UnitTestUtils.SAMPLE_ISO_TIME_STRING,
                            reportTimeInterval: ReportTimeIntervalEnum.CUSTOM_RANGE.name()]

    when: 'the header is built'
    def header = ReportEngine.instance.buildHTMLHeader(reportDetails)

    then: 'header section shows the date time values'
    def paramsText = HTMLTestUtils.extractTag(header, '<div id="ReportHeader"', true)
    def startDateText = DateUtils.formatDate(new Date(UnitTestUtils.SAMPLE_TIME_MS))
    paramsText.contains("${lookup('startDateTime.label')}: ${startDateText}")
    paramsText.contains("${ReportTimeIntervalEnum.CUSTOM_RANGE.name()}")
  }

  def "verify that the hyperlink extension works"() {
    given: 'a compiled/filled report'
    Report report = ReportEngine.instance.compile(new Report(SAMPLE_PARENT_REPORT))
    report.data = [[ordr: 'ABC-XYZ', 'COUNT': 255L, 'AVERAGE': 12.37]]
    ReportEngine.instance.fill(report)

    when: 'the HTML is exported'
    def writer = new StringWriter()
    ReportEngine.instance.exportReportToHTML(report, writer)

    then: 'the output type is set to HTML'
    report.contentType == "text/html"

    and: 'the data is used in the report'
    def page = writer.toString()
    page.contains('ABC-XYZ')
    page.contains('12.37')

    and: 'the hyperlink to the child record is correct'
    def hyperlinkText = TextUtils.findLine(page, 'ABC-XYZ')
    hyperlinkText.contains('href="/report?loc=reports/sample/SampleReportChild.jrxml&amp;ordr=ABC-XYZ"')
  }

  def "verify that exportToHTML compiles and fills a report with sub-reports correctly"() {
    given: 'disabled WARN message from library'
    LogUtils.getLogger('net.sf.jasperreports.engine.query.JRJdbcQueryExecuter').level = Level.ERROR

    and: 'a compiled/filled report with no data'
    Report report = new Report([flex_type: 'ABC-XYZ', loc: SAMPLE_WITH_SUB_REPORT], new MockPrincipal())
    report.data = [[flex_type: 'ABC-XYZ']]
    ReportEngine.instance.compile(report)
    ReportEngine.instance.fill(report)


    when: 'the HTML is exported'
    def writer = new StringWriter()
    ReportEngine.instance.exportReportToHTML(report, writer)

    then: 'the output type is set to HTML'
    report.contentType == MediaType.TEXT_HTML

    and: 'the data is used in the report'
    def page = writer.toString()
    page.contains('flex_type')
    page.contains('ABC-XYZ')
  }

  def "verify that formatForURL works with a Long value"() {
    when: 'the value is formatted'
    def s = ReportEngine.instance.formatForURL(new Long(4296))

    then: 'the correct URL form is returned'
    s == '4296'
  }

}
