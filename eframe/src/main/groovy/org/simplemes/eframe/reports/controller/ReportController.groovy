/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports.controller

import groovy.util.logging.Slf4j
import io.micronaut.core.io.Writable
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.views.ViewsRenderer
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.reports.Report
import org.simplemes.eframe.reports.ReportEngine
import org.simplemes.eframe.reports.ReportHelper
import org.simplemes.eframe.reports.ReportResourceCache
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.web.task.TaskMenuItem
import org.simplemes.eframe.web.view.FreemarkerWrapper

import javax.annotation.Nullable
import java.security.Principal

/**
 * Support for report engine-style reports.  Used to display HTML and PDF reports.
 */
@Slf4j
@Secured(["isAuthenticated()"])
@Controller("/report")
class ReportController extends BaseController {

  /**
   * Defines the standard end-user task entry points that this controller handles.
   * This method finds all of the report engine files available for display and adds them
   * in the reports folder (sequence 5500-5999).
   */

  /**
   * The number of buttons per row in the reportActivity (Dashboard activity).
   */
  public static final int DEFAULT_BUTTONS_PER_ROW = 5

  List<TaskMenuItem> getTaskMenuItems() {
    def res = []

    def list = ReportHelper.instance.determineBuiltinReports()

    // Sort on the basic file name for consistent display.
    list.sort { a, b ->
      ReportHelper.instance.determineReportBaseName(a) <=> ReportHelper.instance.determineReportBaseName(b)
    }

    def displayOrder = 5001
    for (path in list) {
      def name = ReportHelper.instance.determineReportBaseName(path)
      res << new TaskMenuItem(folder: 'reports:5000', name: name,
                              uri: "/report?loc=${path}", displayOrder: displayOrder)
      displayOrder += 1
    }

    return res
  }

  /**
   * Displays a single report using the third-party report engine.
   *  <h3>HTTP Parameters</h3>
   * <ul>
   *   <li><b>loc</b> - The report path (e.g. '/reports/Metrics.jrxml'). (<b>Required</b>)</li>
   *   <li><b>page</b> - The page to display for HTML reports  (<b>Default:</b> First page - 1). </li>
   *   <li><b>format</b> - The format.  HTML or PDF. (<b>Default:</b> HTML). </li>
   *   <li><i>other parameters</i> - The report parameters passed to the report engine.</li>
   * </ul>
   *
   */
  @Get(value = "/")
  HttpResponse index(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    log.debug("index() params = {}", params)

    def report = new Report(params, principal)
    def out
    if (report.pdf) {
      out = new ByteArrayOutputStream(4000)
    } else {
      out = new StringWriter()
    }
    def missingRoles = null
    FieldExtension.withTransaction {
      // Need to be in a transaction to avoid connection leaks.
      // Transaction commits seem to clean up the connection correctly.
      missingRoles = ReportEngine.instance.writeReport(report, out)
    }
    if (missingRoles) {
      return buildDeniedResponse(request, "Missing role $missingRoles for ${params.loc}", principal)
    } else {
      // Ok for display
      def body = report.pdf ? out.toByteArray() : out.toString()
      return HttpResponse.status(HttpStatus.OK).contentType(report.contentType).body(body)
    }
  }

  /**
   * Serves the report images needed for the report engine.
   * See {@link org.simplemes.eframe.reports.ReportResourceHandler} for details.
   */
  @Get(value = "/image")
  @SuppressWarnings("unused")
  HttpResponse image(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    log.debug("image() params = $params")
    String imageName = params.image
    def data = ReportResourceCache.instance.getResource(imageName)
    if (data) {
      String contentType = MediaType.IMAGE_JPEG
      if (imageName.contains('.png')) {
        contentType = MediaType.IMAGE_PNG
      }
      return HttpResponse.status(HttpStatus.OK).contentType(contentType).body(data)
    } else {
      return HttpResponse.status(HttpStatus.NOT_FOUND)
    }
  }

  /**
   * Displays the change filter page for the report engine interface.
   * This page expects the report parameters on the URL along with the report 'loc'.  See index() above.
   */
  @Get(value = "/filter")
  @Produces(MediaType.TEXT_HTML)
  HttpResponse filter(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    log.debug("filter(): params = {}", params)

    def loc = params.loc
    def report = new Report((String) loc)
    report.params = params
    ReportEngine.instance.compile(report)
    def fieldDefinitions = ReportHelper.instance.buildFieldDefinitionsFromParameters(report)

    // Determine if the GUI should handle the special report time behavior for custom time interval.
    def reportField = fieldDefinitions[ReportEngine.REPORT_TIME_INTERVAL_NAME]
    def startField = fieldDefinitions[ReportEngine.REPORT_START_DATE_TIME_NAME]
    def endField = fieldDefinitions[ReportEngine.REPORT_END_DATE_TIME_NAME]
    def reportTimeFound = reportField && startField && endField
    //println "reportDetails = $reportDetails"
    // Find all of the date/time fields we need to handle specially once the user submits the filter changes.
    def dateTimeParams = new StringBuilder()
    for (parameter in fieldDefinitions) {
      if (parameter.format == DateFieldFormat.instance) {
        if (dateTimeParams.size()) {
          dateTimeParams << ","
        }
        dateTimeParams << parameter.name
      }
    }

    def modelAndView = new StandardModelAndView('report/filter', principal, this)
    def model = modelAndView.model.get()
    model.put('reportName', report.reportName - '.jrxml')
    model.put('reportTimeFound', reportTimeFound)
    model.put('dateTimeParams', dateTimeParams.toString())
    model.put('loc', loc)
    model.put('reportFilterValues', params)
    model.put('reportFields', new FreemarkerWrapper(fieldDefinitions))

    log.debug('filter(): model {}', modelAndView)
    def renderer = Holders.applicationContext.getBean(ViewsRenderer)
    Writable writable = renderer.render(modelAndView.view.get(), model)
    return HttpResponse.status(HttpStatus.OK).body(writable)
  }

  /**
   * Handles the user's changes to the filter parameters and re-directs to the report display
   * with the correct values.  Also re-formats any date/times into ISO format.
   */
  @Post(value = "/filterUpdate")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @SuppressWarnings("unused")
  HttpResponse filterUpdate(HttpRequest request, @Body bodyParams, @Nullable Principal principal) {
    //def params = ControllerUtils.instance.convertToMap(request.parameters)
    log.debug("filterUpdate(): bodyParams = {}", (Object) bodyParams)

    String location = bodyParams.loc
    ArgumentUtils.checkMissing(location, 'parameters.loc')

    // Build the params needed to display the report with the new filter values.
    def forwardParams = [:]
    def report = new Report(location)
    ReportEngine.instance.compile(report)
    def fieldDefinitions = ReportHelper.instance.buildFieldDefinitionsFromParameters(report)
    for (fieldDefinition in fieldDefinitions) {
      // Check each parameter that the report needs.
      def name = fieldDefinition.name
      def s = bodyParams[name]
      if (s) {
        def value = fieldDefinition.format.parseForm((String) s, null, fieldDefinition)
        forwardParams[name] = fieldDefinition.format.encode(value, fieldDefinition)
      }
    }

    // Now persist the parameters for the next time the user displays the report.
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page ReportHelper.REPORT_PAGE
        user SecurityUtils.currentUserName
        element location
      }

      // clear any existing params
      preference.settings = []
      forwardParams.each() { k, v ->
        if (v) {
          preference.setPreference(new SimpleStringPreference(key: k, value: v))
        }
      }
      preference.save()
    }

    // Now, add any params that are not part of the persisted user preferences.
    forwardParams.loc = location

    def uri = ControllerUtils.instance.buildURI('/report', forwardParams)
    return HttpResponse.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, uri)
  }

  /**
   * Displays the report  dashboard activity.
   * @param request The request.
   * @param principal The user.
   * @return The page.
   */
  @Get("/reportActivity")
  @Produces(MediaType.TEXT_HTML)
  @SuppressWarnings(["unused", "UnnecessaryQualifiedReference"])
  StandardModelAndView reportActivity(HttpRequest request, @Nullable Principal principal) {
    def modelAndView = new StandardModelAndView("report/reportActivity", principal, this)
    def params = modelAndView.model.get().params

    // build the addition to the URI for the passed in parameters
    def otherParams = new StringBuilder()
    params.each { k, v ->
      if (!k.startsWith('_')) {
        otherParams << "&${k}=${v}"
      }
    }
    modelAndView.model.get().otherParams = otherParams
    modelAndView.model.get().newWindow = !Holders.environmentTest

    def reports = ReportHelper.instance.determineBuiltinReports()
    def nRows = NumberUtils.divideRoundingUp(reports.size(), DEFAULT_BUTTONS_PER_ROW)

    def rows = []
    for (int nRow = 0; nRow < nRows; nRow++) {
      def list = []
      for (int i = 0; i < DEFAULT_BUTTONS_PER_ROW; i++) {
        int index = i + nRow * DEFAULT_BUTTONS_PER_ROW
        if (index < reports.size()) {
          def path = reports[index]
          def name = ReportHelper.instance.determineReportBaseName(path)
          list << [name: name, uri: "/report?loc=$path"]
        }
      }
      rows << list
    }
    modelAndView.model.get().rows = rows

    return modelAndView
  }

}

