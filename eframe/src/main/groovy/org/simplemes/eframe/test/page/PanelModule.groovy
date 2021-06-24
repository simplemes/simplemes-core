/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

import geb.Module
import geb.navigator.Navigator

/**
 * Defines the GEB page elements for a standard tabbed panel with the given index.
 * <p>
 * <h4>Example Page Definition:</h4>
 * <pre>
 *   static content = &#123;
 *     mainPanel &#123; module(new PanelModule(index: 0)) &#125;
 *   &#125;
 * </pre>
 *
 * <p>
 * <h4>Example Test Spec Usage:</h4>
 * <pre>
 *   mainPanel.click()
 * </pre>
 *
 */
class PanelModule extends Module {
  int index = 0

  @SuppressWarnings('unused')
  static content = {
    tab { $('ul.p-tabview-nav').find('li', index) }
  }

  /**
   * Clicks the tab.
   * @return Always null.
   */
  Navigator click() {
    tab.click()
    return null
  }

}
