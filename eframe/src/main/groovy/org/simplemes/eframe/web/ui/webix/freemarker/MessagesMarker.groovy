/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.exception.MessageHolder

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
    def params = getModelValue('params') as Map
    sb << buildMessageFromParams('info', params)
    sb << buildMessageFromParams('error', params)
    sb << buildMessageFromParams('warning', params)

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

  /**
   * Builds a single message HTML text from a URI parameter of the given type.
   * @param params The URI parameters.
   * @return The HTML for the div to display the message correctly.
   */
  String buildMessageFromParams(String type, Map params) {
    if (params) {
      def paramName = "_$type"
      def text = params[paramName] as String
      if (text) {
        return """<div class="message $type-message">${escape(text)}</div>\n"""

      }
    }
    return ''
  }

}
