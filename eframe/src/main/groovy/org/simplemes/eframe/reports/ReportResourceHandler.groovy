package org.simplemes.eframe.reports

import net.sf.jasperreports.engine.export.HtmlResourceHandler


/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Handles the report engine's resource (image) processing suitable for use in a controller.
 * This provides the correct resource path/names using a convention for use the
 * {@link org.simplemes.eframe.reports.controller.ReportController#image} method.
 * The external URI for the resources uses resource ID and the reportPath to make the reference unique.
 */
class ReportResourceHandler implements HtmlResourceHandler {

  /**
   * The partial URI to find the images.
   */
  String imageURI = "/report/image?image="

  /**
   * The location of the of the report.
   */
  private final String reportPath

  /**
   * Build the handle for the given report path.
   * @param reportPath The path (e.g. '/reports/Metrics.jrxml').
   */
  ReportResourceHandler(String reportPath) {
    this.reportPath = reportPath
  }

  /**
   * Provides the resource path to use for the given ID in the report.
   * This points to ReportController to serve up the image.
   * @param id The image ID.
   * @return The path to use for the image in the HTML.
   */
  @Override
  String getResourcePath(String id) {
    def uniqueID = buildUniqueID(id)
    return "$imageURI$uniqueID"
  }

  /**
   * Stores the given resource (image) in a temporary cache so the resource can be server up by the
   * ReportController.image method.
   * @param id The resource ID within the page.
   * @param data The resource data.
   */
  @Override
  void handleResource(String id, byte[] data) {
    def path = buildUniqueID(id)
    ReportResourceCache.instance.putResource(path, data)
  }

  /**
   * Builds a semi-unique resource ID from the report path and the given resource ID from the report engine.
   * @param id The resource ID.
   * @return The unique ID.
   */
  String buildUniqueID(String id) {
    def reportString = reportPath - '.jrxml'
    return "${reportString}_${id}"
  }


}
