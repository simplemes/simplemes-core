package org.simplemes.eframe.test.page

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB Page for the framework's home page.
 * <p/>
 * This page defines these content sections:
 * <ul>
 *   <li><b>logoutButton</b> - The standard login button (not always present).</li>
 * </ul>
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class HomePage extends AbstractPage {
  /**
   * Build the page.
   */
  HomePage() {
    // No need to wait for Ajax finish on load.
  }

  static url = "/"
  static at = { title }
  //static at = { true }
}
