package org.simplemes.eframe.reports

import groovy.util.logging.Slf4j
import org.simplemes.eframe.data.FieldDefinitions
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Provides helper methods for finding and using reports from the external report engine.
 */
@Slf4j
class ReportHelper {

  /**
   * A singleton-style instance for this class.  Used to make testing easier.
   */
  static ReportHelper instance = new ReportHelper()

  /**
   * The page that serves up the reports.  Used mainly for user preference storage.
   */
  static final String REPORT_PAGE = '/report/index'

  /**
   * Finds the list of available built-in reports from the file system and/or the class path.
   * @return The list of location strings.
   */
  List<String> determineBuiltinReports() {
    def res = []
    // Check the built-in location first
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
    def resources = resolver.getResources('classpath*:reports/*.jrxml')
    for (resource in resources) {
      log.debug('determineBuiltinReports: resource: {}, file: ', resource, resource.filename)
      res << "reports/$resource.filename".toString()
    }

    return res
  }

  /**
   * Opens an input stream for the report's jrxml file.  Supports built-in reports found on the classpath.
   * @param reportDetails The report details.
   * @return The input stream.  Can be null if not found.
   */
  @SuppressWarnings("JavaIoPackageAccess")
  InputStream getInputStream(Report reportDetails) {
    def resourcePath = "$reportDetails.reportFolder/$reportDetails.reportName"
    return this.class.getClassLoader().getResourceAsStream(resourcePath)
  }

  /**
   * Determines the base file name for the given report path.  For example,
   * 'reports/Metrics.jrxml' returns 'Metrics'.
   * @param fullPath The full path of the report.
   * @return The base file name.
   */
  String determineReportBaseName(String fullPath) {
    def res = ''

    def loc = fullPath?.lastIndexOf('/')
    if (loc >= 0) {
      res = fullPath[(loc + 1)..-1] - '.jrxml'
    } else if (fullPath) {
      res = fullPath - '.jrxml'
    }

    return res
  }

  /**
   * Builds the FieldDefinitions from the list of report parameters.  This is used for display purposes.
   * @param report The report.
   * @return The FieldDefinitions.
   */
  FieldDefinitions buildFieldDefinitionsFromParameters(Report report) {
    def fieldDefinitions = new FieldDefinitions()
    //def compiledReport = ReportEngine.instance.compile(report)

    for (param in report.reportParameters) {
      fieldDefinitions << param
    }

    return fieldDefinitions
  }


}

