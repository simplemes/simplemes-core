package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.exception.MessageHolder

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the efMessages Freemarker marker implementation.
 * This creates the standard <div> for the message display and populates the div
 * if any messages exist.
 */
@SuppressWarnings("unused")
class MessagesMarker extends BaseMarker {
  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    // Build any messages
    def sb = new StringBuilder()

    def messageHolder = (MessageHolder) getModelValue(StandardModelAndView.MESSAGES)
    if (messageHolder) {
      sb << buildMessage(messageHolder)
      // Add any other messages
      for (message in messageHolder.otherMessages) {
        sb << buildMessage(message)
      }
    }

    def s = """<div id="messages">${sb}</div>\n"""

    write(s)
  }

  /**
   * Builds a single message HTML text.
   * @param messageHolder The message to build.
   * @return The HTML for the div to display the message correctly.
   */
  String buildMessage(MessageHolder messageHolder) {
    def level = messageHolder.levelText
    return """<div class="message $level-message">${escape(messageHolder.text)}</div>\n"""
  }

}
