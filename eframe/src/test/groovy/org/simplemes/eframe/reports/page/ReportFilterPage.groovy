package org.simplemes.eframe.reports.page

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.page.AbstractPage
import org.simplemes.eframe.test.page.ComboboxModule
import org.simplemes.eframe.test.page.DateFieldModule
import org.simplemes.eframe.test.page.TextFieldModule

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Defines the GEB page elements for the Report change filter page used with the report engine reports.
 *
 */
@SuppressWarnings("unused")
class ReportFilterPage extends AbstractPage {
  static url = "/report/filter"
  static at = { title.contains(GlobalUtils.lookup('reportFilter.title')) }

  static content = {
    updateButton { $('div.webix_el_button', view_id: "updateFilter").find('button') }

    // Fields dependent on the report being display: SampleReport fields
    name { module(new TextFieldModule(field: 'name')) }
    reportTimeInterval { module(new ComboboxModule(field: 'reportTimeInterval')) }
    startDateTime { module(new DateFieldModule(field: 'startDateTime')) }
    endDateTime { module(new DateFieldModule(field: 'endDateTime')) }
  }

}

