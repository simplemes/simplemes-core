/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.micronaut.http.MediaType
import net.sf.jasperreports.engine.JRDataSource
import net.sf.jasperreports.engine.JRParameter
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.FileUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.security.SecurityUtils

import java.security.Principal

/**
 * Holds the report details needed to generate a report using the third-party report engine.
 * Provides methods to write the report in HTML or PDF format.
 */
@Slf4j
@EqualsAndHashCode(includes = ['reportFolder', 'reportName', 'params'])
@ToString(includeNames = true, includePackage = false,
  includes = ['reportFolder', 'reportName', 'params', 'resourceBundleName'])
class Report {

  /**
   * The prefix for sub-report parameter names.
   */
  static final String SUB_REPORT_PREFIX = 'SubReport'

  /**
   * The folder the reports are stored in.
   */
  String reportFolder

  /**
   * This report/s file name (including extension).
   */
  String reportName

  /**
   * The input parameters from the controller.
   */
  Map params = [:]

  /**
   * The principal (user) producing the report.
   */
  Principal principal

  /**
   * The compiled report.
   */
  Object compiledReport

  /**
   * The report with the data filled in.
   */
  Object filledReport

  /**
   * The filter parameters in URL format.  Suitable for use on href, etc...
   */
  String filterParams = ""

  /**
   * The date/time the report was generated.   This is used when working with the reportTimeInterval for consistent
   * reports.
   */
  Date reportDate = new Date()

  /**
   * The default values, from the compiled report.
   */
  private Map<String, Object> defaultValues = null

  /**
   * The report parameters, from the compiled report.
   */
  private List definedReportParameters = null

  /**
   * The effective report parameters, from the compiled report and the controller parameters.
   */
  private List reportParameters = null

  /**
   * The name of the resource bundle requested by the report.
   */
  String resourceBundleName

  /**
   * The report locale to use.
   */
  Locale locale

  /**
   * The content type for this report's output.
   */
  String contentType = MediaType.TEXT_HTML

  /**
   * The Report Engine data container (mainly used for unit testing).
   */
  JRDataSource jrDataSource


  /**
   * Main constructor for use from a controller.
   * @param params The request parameters.
   * @param principal The user logged in.
   */
  Report(Map params, Principal principal) {
    this((String) params?.loc)
    this.params = determineReportParams(params)
    this.principal = principal
  }

  /**
   * Convenience constructor.
   * @param reportLocation The report location (with folder).
   */
  Report(String reportLocation) {
    if (reportLocation) {
      def s = FileUtils.resolveRelativePath(reportLocation)
      def i = s.lastIndexOf('/')
      if (i < 0) {
        throw new IllegalArgumentException("Invalid report reportLocation.  No '/' in '${reportLocation}'")
      }
      this.reportName = s[(i + 1)..-1]
      this.reportFolder = s[0..(i - 1)]
      this.params.loc = reportLocation
    }
  }

  /**
   * Convenience constructor.
   * @param reportLocation The report location (with folder).
   * @param options The other options to be set in this class.
   */
  Report(String reportLocation, Map options) {
    this(reportLocation)

    options.each { k, v ->
      this[(String) k] = v
    }
  }

  /**
   * Returns the compiled report. Compiles the report, if needed.
   * @return The compiled report.
   */
  Object getCompiledReport() {
    if (!compiledReport) {
      ReportEngine.instance.compile(this)
    }
    return compiledReport
  }

  /**
   * Finds the defined report parameters.
   * @return A list of JRParameter objects.
   */
  List getDefinedReportParameters() {
    if (definedReportParameters == null) {
      ArgumentUtils.checkMissing(getCompiledReport(), 'compiledReport')
      definedReportParameters = []
      for (parameter in compiledReport.parameters) {
        if (parameter.forPrompting && !parameter.systemDefined) {
          definedReportParameters << parameter
        }
      }
    }
    return definedReportParameters
  }

  /**
   * Finds the default values for the report parameters.
   */
  Map<String, Object> getDefaultParameters() {
    if (defaultValues == null) {
      defaultValues = ReportEngine.instance.evaluateParameterDefaultValues(this)
    }
    return defaultValues
  }

  /**
   * Finds the effective report parameters to use.  This uses the controller params and the default values
   * from the compiled report.  List is sorted in display sequence.
   */
  List<ReportFieldDefinition> getReportParameters() {
    if (reportParameters == null) {
      reportParameters = []
      for (parameter in getDefinedReportParameters()) {
        def reportFieldDefinition = new ReportFieldDefinition((JRParameter) parameter, this)
        reportFieldDefinition.sequence = determineParameterSequence(parameter)
        reportParameters << reportFieldDefinition
      }
      reportParameters.sort() { a, b -> a.sequence <=> b.sequence }
    }
    return reportParameters
  }


  /**
   * Returns the list of sub-reports that need to be compiled.
   * Finds parameters that start with 'SubReport' and extracts the report name from the default value expression.
   * The default value must be in the form: '$P{BaseDir}+"/SampleSubReport.jrxml"'.
   * @return A map of the sub-report.  The key is the parameter name for the sub report.
   */
  Map<String, String> getSubReports() {
    Map<String, String> res = [:]
    for (parameter in compiledReport?.parameters) {
      if (!parameter.systemDefined /*&& parameter.name.startsWith(SUB_REPORT_PREFIX)*/) {
        String name = parameter.name
        if (name.startsWith(SUB_REPORT_PREFIX)) {
          String expression = parameter?.defaultValueExpression?.text
          def start = expression.indexOf('"')
          def end = expression.lastIndexOf('"')
          if (start < 0 || end < 0 || (start == end)) {
            throw new IllegalArgumentException("sub-report parameter $name does not have a string in the default expression: '$expression'")
          }
          def s = expression[(start + 1)..(end - 1)]
          res[name] = s
        }
      }
    }

    return res
  }

  /**
   * Determines the effective display sequence to use for the given report engine parameter.
   * Uses the custom property 'sequence' if available.  Otherwise, uses the
   * documented Parameter Display Sequence from the framework Guide.
   * @param parameter The parameter to determine the display sequence for.
   * @return The display sequence.  Never null.
   */
  Integer determineParameterSequence(Object parameter) {
    // Use the custom property, if possible.
    def s = (String) parameter.propertiesMap?.getProperty('sequence')
    if (s && NumberUtils.isNumber(s)) {
      return Integer.valueOf(s)
    }

    switch (parameter.name) {
      case 'reportTimeInterval':
        return 100
      case 'startDateTime':
        return 110
      case 'endDateTime':
        return 120

      default:
        return 50
    }
  }

  /**
   * Determine the effective row limit to use for the report.   Can be overridden by the application.yml
   * setting: org.simplemes.eframe.report.rowLimit
   * @return The row limit.
   */
  Integer getEffectiveRowLimit() {
    Integer i = Holders.configuration.report.rowLimit
    if (i > 0) {
      return i
    }
    return EFrameConfiguration.REPORT_ROW_COUNT
  }

  /**
   * Builds the base input parameters to pass to the report.  This mainly builds values such as BaseDir
   * the the resource bundle needed by most reports.
   */
  Map<String, Object> buildBaseParametersForReport() {
    Map<String, Object> res = [:]

    res.BaseDir = reportFolder
    res.REPORT_LOCALE = GlobalUtils.requestLocale
    res.REPORT_MAX_COUNT = effectiveRowLimit
    if (resourceBundleName) {
      res.REPORT_RESOURCE_BUNDLE = ResourceBundle.getBundle(getBundleRelativePath())
    }

    return res
  }

  /**
   * Returns the relative path for a bundle reference.  Will adjust the path to a resource classloader relative
   * path if from a .jar file.
   * @return The adjusted bundle path to use to load the bundle.
   */
  String getBundleRelativePath() {
    if (reportFolder.startsWith("jar:")) {
      def s = "$reportFolder/$resourceBundleName"
      def loc = s.indexOf("!/")
      if (loc >= 0) {
        s = s[(loc + 2)..-1]
      }
      return FileUtils.resolveRelativePath(s)
    } else {
      return FileUtils.resolveRelativePath("$reportFolder/$resourceBundleName")
    }
  }


  /**
   * Determines the effective report parameters to use.  Check the URL (controller) params and the user preferences as a
   * fall-back.  This uses the controller params and adds any parameters from the user preferences that are not
   * in the controller params.
   * @param inputParams The parameters from the URL.
   * @return The effective parameters.
   */
  Map determineReportParams(Map inputParams) {
    Map res = (Map) inputParams.clone()
    String loc = res.loc
    PreferenceHolder preference = PreferenceHolder.find {
      page ReportHelper.REPORT_PAGE
      user SecurityUtils.currentUserName
      element loc ?: '?'
    }

    // Copy the params not on the URL to the effective params.
    for (setting in preference.settings) {
      if (setting instanceof SimpleStringPreference) {
        def name = setting.key
        def value = setting.value
        if (res[name] == null) {
          res[name] = value
        }
      }
    }

    return res
  }

  /**
   * Sets the content type for this report.
   * @param contentType The content type.
   */
  def setContentType(String contentType) {
    this.contentType = contentType
  }

  /**
   * Sets the report data for the report. Unit tests only.
   * @param data The data.
   */
  void setData(List<Map> data) {
    jrDataSource = new JRMapCollectionDataSource(data)
  }

  /**
   * Returns true if the report output in PDF is requested.
   * @return True for PDF.
   */
  boolean isPdf() {
    return params?.format?.equalsIgnoreCase('pdf')
  }
}
