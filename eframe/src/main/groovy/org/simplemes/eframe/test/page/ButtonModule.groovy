/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

import geb.Module
import geb.navigator.Navigator

/**
 * Defines the GEB page elements for a standard button with the given id.  This contains these elements:
 * <p>
 * <h4>Example Page Definition:</h4>
 * <pre>
 *   static content = &#123;
 *     releaseButton &#123; module(new ButtonModule(id: 'release')) &#125;
 *   &#125;
 * </pre>
 *
 * <p>
 * <h4>Example Test Spec Usage:</h4>
 * <pre>
 *   releaseButton.click()
 *   releaseButton.button.text() == 'Release'
 * </pre>
 *
 * <h4>This contains these elements:</h4>
 * <ul>
 *   <li><b>button</b> - The button itself.</li>
 * </ul>
 *
 */
@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyUnusedDeclaration"])
class ButtonModule extends Module {
  String id

  static content = {
    button { $('div.webix_el_button', view_id: id).find('button') }
  }

  /**
   * Clicks the button.
   * @return Always null.
   */
  Navigator click() {
    button.click()
    return null
  }

}
