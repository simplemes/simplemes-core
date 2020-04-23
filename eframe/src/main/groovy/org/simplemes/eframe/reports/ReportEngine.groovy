/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports

import groovy.util.logging.Slf4j
import io.micronaut.transaction.jdbc.DataSourceUtils
import net.sf.jasperreports.engine.DefaultJasperReportsContext
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperPrint
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.design.JasperDesign
import net.sf.jasperreports.engine.export.HtmlExporter
import net.sf.jasperreports.engine.export.JRPdfExporter
import net.sf.jasperreports.engine.fill.JRParameterDefaultValuesEvaluator
import net.sf.jasperreports.engine.xml.JRXmlLoader
import net.sf.jasperreports.export.SimpleExporterInput
import net.sf.jasperreports.export.SimpleHtmlExporterConfiguration
import net.sf.jasperreports.export.SimpleHtmlExporterOutput
import net.sf.jasperreports.export.SimpleHtmlReportConfiguration
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.domain.annotation.DomainEntityHelper
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.HTMLUtils
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum

import javax.sql.DataSource

/**
 * Provides low-level access to the external report engine.  This is designed to be replaced by a mock
 * engine for testing purposes.  The external report engine is not designed to be easily mocked, so this is
 * the place to mock the behavior.
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>info</b> - Performance timing. </li>
 *   <li><b>debug</b> - Parameters and arguments. </li>
 * </ul>
 */
@Slf4j
class ReportEngine {

  /**
   * A singleton-style instance for this class.  Used to make testing easier.
   */
  static ReportEngine instance = new ReportEngine()

  /**
   * The JRXML custom property that specifies the roles needed for the given report.
   */
  static final String PROPERTY_ROLES = 'org.simplemes.roles'

  /**
   * The name of the standard report time interval field used for special processing.
   */
  static final String REPORT_TIME_INTERVAL_NAME = 'reportTimeInterval'

  /**
   * The name of the standard report start date/time field used for special processing.
   */
  static final String REPORT_START_DATE_TIME_NAME = 'startDateTime'

  /**
   * The name of the standard report end date/time field used for special processing.
   */
  static final String REPORT_END_DATE_TIME_NAME = 'endDateTime'

  /**
   * Writes the given report to the given output writer.
   * @param report The report to write.
   * @param out The output writer.
   * @return The list of missing roles (if any).  Null/empty means there are no missing roles and the report can be displayed
   *         to the user.
   */
  String writeReport(Report report, Object out) {
    ReportEngine.instance.compile(report)

    def missingRoles = ReportEngine.instance.checkForMissingRoles(report)
    if (missingRoles) {
      return missingRoles
    }

    ReportEngine.instance.fill(report)
    if (report.pdf) {
      ReportEngine.instance.exportReportToPDF(report, (OutputStream) out)
    } else {
      ReportEngine.instance.exportReportToHTML(report, (Writer) out)
    }

    return null
  }

  /**
   * Compile a report for execution.
   * @param reportDetails The details of the report being generated.
   * @return The reportDetails (with a compiled report set).
   */
  Report compile(Report reportDetails) {
    log.debug('compile (top): reportDetails: {}', reportDetails)
    ArgumentUtils.checkMissing(reportDetails.params.loc, 'params.loc')
    def start = System.currentTimeMillis()
    def resourcePath = "$reportDetails.reportFolder/$reportDetails.reportName"
    def inputStream = ReportHelper.instance.getInputStream(reportDetails)
    if (!inputStream) {
      //error.110.message=Could not find {0} {1}
      throw new BusinessException(110, [GlobalUtils.lookup('report.label'), resourcePath])
    }
    JasperReport jasperReport = null
    try {
      JasperDesign jasperDesign = JRXmlLoader.load(inputStream)
      reportDetails.resourceBundleName = jasperDesign.mainDesignDataset.resourceBundle
      jasperReport = JasperCompileManager.compileReport(jasperDesign)
    } finally {
      inputStream?.close()
    }
    log.info("Compile Elapsed: {}ms for {}", (System.currentTimeMillis() - start), resourcePath)
    reportDetails.compiledReport = jasperReport
    log.debug("compile (done): reportDetails = {}", reportDetails)
    return reportDetails
  }

  /**
   * Compiles any sub-reports found in the current report.  This stores the compiled sub-report in the parameters
   * for use by the main report.
   * @param reportDetails The report being created (master report).
   * @param parameters The parameters that will be passed to the master report.
   */
  void compileSubReports(Report reportDetails, Map<String, Object> parameters) {
    reportDetails.subReports.each { parameterName, subReport ->
      def loc = "$reportDetails.reportFolder/$subReport"
      log.debug("compileSubReports: loc = {}", loc)
      def compiled = compile(new Report(loc)).compiledReport
      parameters.put(parameterName, compiled)
    }

  }

  /**
   * Fills a report result using the given report and data.
   * @param report The compiled report details to fill in with data.
   * @param data If provided, test data for the report.  If not given, then will use the dataSource bean to make a connection.
   * @return The report Details.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  Report fill(Report report) {
    log.debug('fill: reportDetails: {}', report)
    def start = System.currentTimeMillis()

    Map<String, Object> parameters = report.buildBaseParametersForReport()

    def reportParameters = report.reportParameters
    for (parameter in reportParameters) {
      parameters.put(parameter.name, parameter.effectiveValue)
    }

    compileSubReports(report, parameters)

    // Check for special case parameter reportTimeInterval.  Overrides the start/end date times.
    def reportTimeIntervalParam = reportParameters.find() { it.name == REPORT_TIME_INTERVAL_NAME }
    if (reportTimeIntervalParam) {
      def s = reportTimeIntervalParam.effectiveValue
      if (s) {
        def reportTimeInterval = ReportTimeIntervalEnum.valueOf((String) s)
        def range = reportTimeInterval.determineRange(report.reportDate)
        if (range) {
          parameters.put(REPORT_START_DATE_TIME_NAME, range.start)
          parameters.put(REPORT_END_DATE_TIME_NAME, range.end)
        }
      }
    }

    log.debug('fill: parameters: {}', parameters)

    def dataOrConn = report.jrDataSource
    if (!dataOrConn) {
      // Need to get a real DB connection
      // Avoid connection leaks by enforcing use of a txn around this fill() method.
      DomainEntityHelper.instance.checkForTransaction()
      DataSource dataSource = Holders.applicationContext.getBean(DataSource.class)
      dataOrConn = DataSourceUtils.getConnection(dataSource)
    }
    JasperPrint jasperPrint = JasperFillManager.fillReport(report.compiledReport, parameters, dataOrConn)
    report.filledReport = jasperPrint

    log.info("Fill Elapsed: {}ms", (System.currentTimeMillis() - start))
    return report
  }

  /**
   * Checks the current user against the required roles from the given compiled report.
   * Returns a list of missing roles.
   * @param report The report details (including the compiled report).
   * @return A comma-delimited list of missing roles.
   */
  String checkForMissingRoles(Report report) {
    String rolesNeeded = report.compiledReport.getProperty(PROPERTY_ROLES)
    def sb = new StringBuilder()
    if (rolesNeeded) {
      for (String role in rolesNeeded.tokenize()) {
        if (!SecurityUtils.instance.isAllGranted(role, report.principal)) {
          if (sb.length() > 0) {
            sb.append(',')
          }
          sb.append(role)
        }
      }
    }
    return sb.toString()
  }

  /**
   * Exports the given report to the HTML output stream.
   * @param report The details on the compiled/filled report.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  void exportReportToHTML(Report report, Writer out) {
    def page = ArgumentUtils.convertToInteger(report.params.page) ?: 1
    report.setContentType("text/html")

    HtmlExporter exporter = new HtmlExporter()
    SimpleHtmlReportConfiguration reportConfig = new SimpleHtmlReportConfiguration()
    reportConfig.setPageIndex(page - 1)
    exporter.setConfiguration(reportConfig)

    //session.setAttribute(ImageServlet.DEFAULT_JASPER_PRINT_SESSION_ATTRIBUTE, print)

    exporter.setExporterInput(new SimpleExporterInput(report.filledReport))
    SimpleHtmlExporterOutput output = new SimpleHtmlExporterOutput(out)
    //output.setImageHandler(new WebHtmlResourceHandlerX("reports/image?image={0}"))
    output.setImageHandler(new ReportResourceHandler(report.reportFolder))
    exporter.setExporterOutput(output)

    SimpleHtmlExporterConfiguration exporterConfig = new SimpleHtmlExporterConfiguration()

    def nPages = report.filledReport.pages?.size() ?: 1

    report.locale = GlobalUtils.getRequestLocale()
    def header = buildHTMLHeader(report)
    if (report.filledReport.pages?.size() == 0) {
      header += GlobalUtils.lookup('report.noDataFound.message')
    }
    exporterConfig.setHtmlHeader(header)

    def uri = "report?loc=${report.reportFolder}/${report.reportName}${report.filterParams}"
    def footer = HTMLUtils.buildPager(page, nPages, uri)
    exporterConfig.setHtmlFooter("$footer</td></tr>\n" +
                                   "</table><div id='ReportFooter'></div>\n" +
                                   "</body>\n" +
                                   "</html>")
    exporter.setConfiguration(exporterConfig)

    exporter.exportReport()
  }

  /**
   * Builds the HTML header for reports generated by the report engine.
   * This also includes the framework CSS file.
   * @param reportDetails The details on the compiled/filled report.
   * @return The header.
   */
  String buildHTMLHeader(Report reportDetails) {
    def title = GlobalUtils.lookup('report.label')
    if (reportDetails.reportName) {
      title = reportDetails.reportName - '.jrxml'
    }
    def locale = reportDetails.locale ?: Locale.default

    def filter = buildHTMLFilterSection(reportDetails)
    return """
      <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
      <html lang="${locale.language}">
      <head>
        <title>$title</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <style type="text/css">
          a {text-decoration: none}
        </style>
        <link rel="stylesheet" href="/assets/eframe.css?compile=false"/>
        <link rel="icon" id="favicon" type="image/x-icon" href="/assets/favicon.ico"/>
      </head>
      <body text="#000000" link="#000000" alink="#000000" vlink="#000000" style="background-color:#fff">
      $filter
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr><td width="2%">
      &nbsp;
      </td><td align="center">
      """
  }

  /**
   * Builds the HTML filter section div for reports generated by the report engine.
   * This shows the filter fields (all parameters) and the other GUI tools such as PDF formats.
   * @param reportDetails The details on the compiled/filled report.
   * @return The filter section HTML.
   */
  String buildHTMLFilterSection(Report reportDetails) {
    StringBuilder displayText = new StringBuilder()
    StringBuilder filterParams = new StringBuilder()

    def suppressDates = false
    def reportParameters = reportDetails.reportParameters
    def reportTimeIntervalParam = reportParameters.find() { it.name == REPORT_TIME_INTERVAL_NAME }
    if (reportTimeIntervalParam) {
      suppressDates = (reportTimeIntervalParam.effectiveValue != null)
      if (reportTimeIntervalParam.effectiveValue == ReportTimeIntervalEnum.CUSTOM_RANGE.name()) {
        suppressDates = false
      }
    }

    for (parameter in reportParameters) {
      def name = parameter.name
      def value = parameter.effectiveValue
      def isDate = (parameter.format == DateOnlyFieldFormat.instance) || (parameter.format == DateFieldFormat.instance)
      def suppressDisplay = false
      if (suppressDates && isDate && (name == REPORT_START_DATE_TIME_NAME || name == REPORT_END_DATE_TIME_NAME)) {
        suppressDisplay = true
      }
      def label = GlobalUtils.lookup("${name}.label") - '.label'

      if (value) {
        //println "$parameter.name value = $value"
        if (!suppressDisplay) {
          if (displayText.size()) {
            displayText << ", "
          }
          displayText << "$label: ${formatForDisplay(value)}"
        }
        filterParams << "&amp;${name}=${formatForURL(value)}"
      }
    }

    reportDetails.filterParams = filterParams.toString()
    def reportPath = "${reportDetails.reportFolder}/${reportDetails.reportName}"
    def changeLabel = GlobalUtils.lookup('changeFilter.label')
    def values = "&nbsp;"
    def filterLink = ""
    if (displayText.size()) {
      values = """<span id="filterValues">$displayText</span>"""
      filterLink = """<a href="/report/filter?loc=$reportPath$filterParams" id="FilterLink" class="report-filter-action">$changeLabel</a>"""
    }

    def pdf = GlobalUtils.lookup('pdf.label')
    def hRef = "/report?loc=$reportPath&amp;format=pdf$filterParams"
    def target = HTMLUtils.buildTargetForLink()
    return """ <div id="ReportHeader" class="report-header">$values  
                 <a href="/" class="report-filter-action">${GlobalUtils.lookup('home.label')}</a>
                 <a href="$hRef" id="PDFLink" class="report-filter-action" $target>$pdf</a>
                 $filterLink
               </div>
    """
  }

  /**
   * Formats the given value for use on a URL.  Mainly used for dates.
   * @param value The value.
   * @return The value formatted for URL use.
   */
  String formatForURL(Object value) {
    if (value instanceof Date) {
      return ISODate.format(value)
    } else {
      return URLEncoder.encode(value?.toString(), 'UTF-8')
    }
  }

  /**
   * Formats the given value for use in the HTML page.  Mainly used for dates.
   * @param value The value.
   * @return The value formatted for display.
   */
  String formatForDisplay(Object value) {
    if (value instanceof Date) {
      return DateUtils.formatDate(value)
    } else {
      return value
    }
  }

  /**
   * Exports the given report to the PDF output stream.
   * @param report The report details (compiled/filled report).
   * @param controller The controller handling the request.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  void exportReportToPDF(Report report, OutputStream outStream) {
    report.contentType = "application/pdf"

    JRPdfExporter exporter = new JRPdfExporter(DefaultJasperReportsContext.getInstance())
    exporter.setExporterInput(new SimpleExporterInput(report.filledReport))

    def output = new SimpleOutputStreamExporterOutput(outStream)
    //output.setImageHandler(new ReportResourceHandler(reportFolder))
    exporter.setExporterOutput(output)
    exporter.exportReport()
  }

  /**
   * Determines the default values for the compiled report.
   * @param reportDetails The report details (compiled/filled report).
   * @return The default values.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  Map evaluateParameterDefaultValues(Report reportDetails) {
    Map<String, Object> parameters = reportDetails.buildBaseParametersForReport()
    if (reportDetails.compiledReport) {
      return JRParameterDefaultValuesEvaluator.evaluateParameterDefaultValues(reportDetails.compiledReport, parameters)
    } else {
      return null
    }
  }

}

