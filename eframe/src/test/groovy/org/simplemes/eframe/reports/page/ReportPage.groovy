package org.simplemes.eframe.reports.page


import org.simplemes.eframe.test.page.AbstractPage

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Defines the GEB page elements for the Report page used with the report engine reports.
 *
 */
@SuppressWarnings("unused")
class ReportPage extends AbstractPage {
  static url = "/report"
  static at = { $("div#ReportFooter") }

  static content = {
    reportTable { $("table.jrPage") }
    reportHeader { $("#ReportHeader") }
    filterValues { $("#filterValues") }
    footerDiv { $("div#footer") }
    nextPageLink { $("a.nextLink") }

    filterLink { $("#FilterLink") }
    pdfLink { $("#PDFLink") }
  }

}

