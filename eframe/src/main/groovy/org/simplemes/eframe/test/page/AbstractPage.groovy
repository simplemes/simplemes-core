/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

import geb.Page
import org.simplemes.eframe.i18n.GlobalUtils

/**
 * The general base class for all pages tested in the framework.
 * Provides basic methods to wait for Ajax completion on load and when needed.
 * <p>
 * If the variable 'waitForAjaxOnLoad=true', then the page will wait until the jQuery Ajax methods are done.
 * On your constructor, you would set this using the code:
 * <pre>
 * public LoginPage() &#123;
 *   // No need to wait for Ajax finish on load.
 *   boolean getWaitForAjaxOnLoad() &#123;
 *     return true
 *   &#125;
 * &#125;
 * </pre>
 * <p/>
 * This page defines these content sections:
 * <ul>
 *   <li><b>messages</b> - The error/info/warning message section.  See {@link MessagesModule} for details.</li>
 *   <li><b>button</b> - A parameterized generic toolkit button (e.g. button('BUTTON').click()).  Supports clicked() and text() methods.</li>
 *   <li><b>toggleConfigButtons</b> - The main button to toggle configuration buttons on a page.</li>
 *   <li><b>logoutButton</b> - The logout button.</li>
 *   <li><b>dialog0</b> - The first dialog.</li>
 * </ul>
 */
class AbstractPage extends Page {

  /**
   * The page content available for this page.  See above.
   */
  static content = {
    messages { module(new MessagesModule(divID: 'messages')) }
    button { id -> $('div.webix_el_button', view_id: id) }
    textField { id -> module(new TextFieldModule(field: id)) }
    configButton(required: false) { $('div.webix_el_button', view_id: 'configButton') }
    logoutButton(required: false) { $('a', id: 'LogoutLink') }
    taskMenuButton { $('div.webix_el_button', view_id: "_taskMenuButton").find('button') }

    //$('a.webix_list_item', webix_l_id: "showMoreMenu")
    menu { id -> $('a.webix_list_item', webix_l_id: id) }

    dialog0 { module(new DialogModule(index: 0)) }
    dialog1 { module(new DialogModule(index: 1)) }
    dialog2 { module(new DialogModule(index: 2)) }
    dialog0Messages { $("div#dialog0Messages") }
    dialog1Messages { $("div#dialog1Messages") }
    dialog2Messages { $("div#dialog2Messages") }
  }

  /**
   * Triggered on load.  Will wait for the Ajax queries to finish.
   * @param previousPage The previous page (unused).
   */
  @SuppressWarnings("UnusedMethodParameter")
  void onLoad(Page previousPage) {
    waitForCompletion()
  }

  /**
   * Waits for any outstanding jQuery Ajax or page loads to finish (+ a small extra delay).
   */
  void waitForCompletion() {
    if (waitForAjaxOnLoad) {
      waitFor() {
        def res = driver.executeScript("return window._ajaxPending()")
        return res == false
      }
    } else {
      // No ajax check needed, so just wait for the page to load
      waitFor() { return driver.executeScript("return (document.readyState=='complete')") }
    }
    // Wait just a little longer to be sure the client has settled down.
    Thread.sleep(20)
  }

  /**
   * If true, then the page will wait on load until the Ajax queries are completed.
   * Override in your sub-class if you have Ajax loading mechanism.
   * This parent class sets it to false.
   * <p>
   * <b>Note:</b> This wait for Ajax completion requires the eframe_toolkit.js be loaded.
   */
  boolean getWaitForAjaxOnLoad() {
    return false
  }

  /**
   * Looks up the given key in the bundle.
   * @param key The key.
   * @return The looked up value.
   */
  String lookup(String key) {
    return GlobalUtils.lookup(key)
  }

}
