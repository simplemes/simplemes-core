package org.simplemes.eframe.test
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This is a base class that provide additional support for testing the Freemarker marker (helper) classes.
 */
class BaseMarkerSpecification extends BaseSpecification {

  /**
   * Executes the marker to generate the page content.
   * Options include: <b>source, controllerClass, uri, domainObject and dataModel.</b>
   *
   * @param options See {@link UnitTestRenderer} for options supported.
   * @return The generated content.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  String execute(Map options) {
    return new UnitTestRenderer(options).render()
  }

  /**
   * Checks the HTML and Javascript for basic format errors.
   * @param page The page content to check.
   * @return True if Ok.  Exception if not.
   */
  boolean checkPage(String page) {
    HTMLTestUtils.checkHTML(page)
    return JavascriptTestUtils.checkScriptsOnPage(page)
  }

}
