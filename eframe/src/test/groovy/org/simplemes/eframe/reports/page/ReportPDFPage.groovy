package org.simplemes.eframe.reports.page


import org.simplemes.eframe.test.page.AbstractPage

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Defines the GEB page elements for the Report page used with the report engine reports (PDF format).
 *
 */
@SuppressWarnings("unused")
class ReportPDFPage extends AbstractPage {
  static url = "/report"
  static at = { title.contains('report') }
  // The PDF page has no easily testable elements.

}

