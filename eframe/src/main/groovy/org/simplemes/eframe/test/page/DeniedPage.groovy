package org.simplemes.eframe.test.page

import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB Page content for the denied page content.
 * This generally does not work with the 'to' action.  Just the 'at' test
 * will work.
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class DeniedPage extends AbstractPage {

  static url = "/?"
  static at = { title.contains(GlobalUtils.lookup('denied.title')) }
  //static at = { true }
}
