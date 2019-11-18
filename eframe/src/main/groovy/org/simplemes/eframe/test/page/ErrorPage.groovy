package org.simplemes.eframe.test.page

import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB Page content for the general purpose error page content.
 * This generally does not work with the 'to' action.  Just the 'at' test
 * will work.
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class ErrorPage extends AbstractPage {

  static url = "/?"
  static at = { title.contains(GlobalUtils.lookup('error.title')) }

  static content = {
    messages { $("div#errors") }
  }
}
