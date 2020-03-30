package org.simplemes.mes.system.page

import org.simplemes.eframe.test.page.DashboardPage

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for a dashboard with the standard scan dashboard main activity page.
 */
@SuppressWarnings("unused")
class ScanDashboardPage extends DashboardPage {

  static content = {
    orderDiv { $('#order') }
    orderStatusDiv { $('#orderStatus') }
  }

}

