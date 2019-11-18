package org.simplemes.eframe.test.page
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tge Page definition for tests using the JavascriptTesterController.
 */
class JavascriptTesterPage extends AbstractPage {

  static url = '/javascriptTester'
  public static final String TITLE = 'Javascript Tester'

  static at = {
    title.contains TITLE
  }

  static content = {
    dialog0 { module(new DialogModule(index: '0')) }

    result { $('div#result') }
  }

  /**
   * If true, then the page will wait on load until the Ajax queries are completed.
   * This sub-class will wait, just in case there is some ajax needed.
   */
  @Override
  boolean getWaitForAjaxOnLoad() {
    return true
  }
}
