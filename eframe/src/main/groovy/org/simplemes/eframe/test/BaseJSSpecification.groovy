package org.simplemes.eframe.test

import groovy.util.logging.Slf4j
import org.simplemes.eframe.test.page.JavascriptTesterPage

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This is the common Spock specification base class for GUI/GEB tests with support for easy testing fo
 * javascript libraries in a real browser page.
 */
@Slf4j
class BaseJSSpecification extends BaseGUISpecification {

  /**
   * Displays the tester page, if not already displayed.
   */
  def displayTesterPage() {

  }

  /**
   * Executes the given javascript on the client.
   * @param script The javascript.
   * @return the result.
   */
  def execute(String script) {
    to JavascriptTesterPage
    return js.exec(script)
  }

}
