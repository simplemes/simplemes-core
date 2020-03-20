package org.simplemes.mes.demand.page

import org.simplemes.eframe.test.page.AbstractShowPage
import org.simplemes.eframe.test.page.TextFieldModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for a dashboard with the standard work center selection page.
 */
@SuppressWarnings("unused")
class WorkCenterSelectionDashboardPage extends AbstractShowPage {

  static url = "/dashboard"
  static at = { title.contains(lookup('dashboard.label')) }

  static content = {
    orderLSNField { module(new TextFieldModule(field: 'order')) }
  }

}
