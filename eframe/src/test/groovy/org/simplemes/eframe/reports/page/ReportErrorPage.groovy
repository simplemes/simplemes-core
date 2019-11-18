package org.simplemes.eframe.reports.page

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.page.AbstractPage

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Defines the GEB page elements for the Report Error page used with the report engine reports.
 *
 */
@SuppressWarnings("unused")
class ReportErrorPage extends AbstractPage {
  static url = "/report"
  static at = { title.contains(GlobalUtils.lookup('error.title')) }

  static content = {
    messages { $("div#errors") }
  }

}

