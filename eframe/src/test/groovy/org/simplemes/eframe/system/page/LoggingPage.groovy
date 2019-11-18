package org.simplemes.eframe.system.page

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.page.AbstractPage

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the logging state/update (index) page.
 */
class LoggingPage extends AbstractPage {

  static url = '/logging'

  static at = { title.contains GlobalUtils.lookup('logging.title') }

  static content = {
    addLoggerButton { $('span#addLoggerButton') }
    otherLoggerTextField { $('div', view_id: 'dialogTextField').find('input') }
  }

  /**
   * If true, then the page will wait on load until the Ajax queries are completed.
   * Override in your sub-class if you have Ajax loading mechanism.
   */
  @Override
  boolean getWaitForAjaxOnLoad() {
    return true
  }
}
