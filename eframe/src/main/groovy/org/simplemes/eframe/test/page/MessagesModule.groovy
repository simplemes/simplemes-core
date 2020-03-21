/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

import geb.Module

/**
 * Defines the GEB page elements the standard message area.
 * <p/>
 * <b>Usage:</b>
 * <pre>
 * // The page definition.
 * static content = &#123;
 *   messages &#123; module(new MessagesModule()) &#125;
 *   altMessages &#123; module(new MessagesModule(divID: 'altMessages')) &#125;
 * &#125;
 * ...
 * assert messages.info
 * </pre>
 *
 * <b>Note:</b> The divID defaults to 'messages'.  You can pass in a different value if needed.
 *
 * <h3>content</h3>
 * The methods available include (with normal Groovy property syntax):
 * <ul>
 *   <li><b>getText() or text</b> - The displayed text. </li>
 *   <li><b>isInfo() or info</b> - True if any message displayed is an info message.  Only use when a single message is displayed. </li>
 *   <li><b>isWarning or warning</b> - True if any message displayed is a warning message.  Only use when a single message is displayed. </li>
 *   <li><b>isWarn or warn</b> - True if any message displayed is a warning message.  Only use when a single message is displayed. </li>
 *   <li><b>isError or error</b> - True if any message displayed is an error message.  Only use when a single message is displayed. </li>
 * </ul>
 *
 */
@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyUnusedDeclaration"])
class MessagesModule extends Module {
  def divID

  static content = {
    _text { $("div#$divID").text() }
    _info { $("div#$divID").find("div.info-message").displayed }
    _warn { $("div#$divID").find("div.warning-message").displayed }
    _error { $("div#$divID").find("div.error-message").displayed }
  }

  String text() {
    return _text
  }

  String getText() {
    return _text
  }

  boolean isError() {
    return _error
  }

  boolean isInfo() {
    return _info
  }

  boolean isWarning() {
    return _warn
  }

  boolean isWarn() {
    return _warn
  }
}
