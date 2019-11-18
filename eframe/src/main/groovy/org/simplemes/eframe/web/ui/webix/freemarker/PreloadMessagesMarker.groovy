package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.JavascriptUtils
import org.simplemes.eframe.misc.NumberUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the efPreloadMessages Freemarker marker implementation.
 * Pre-loads the messages.properties entry for the javascript library to use.
 */
@SuppressWarnings("unused")
class PreloadMessagesMarker extends BaseMarker {

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    def locale = GlobalUtils.getRequestLocale() ?: Locale.default

    def out = new StringBuilder()

    out << '<script>\n'

    out << "    eframe._addPreloadedMessages([\n"
    def codes = parameters.codes?.tokenize(', \t\n\r\f')

    StringBuilder sb = new StringBuilder()
    for (code in codes) {
      if (sb.length() > 0) {
        sb << "\n,"
      }
      def value = JavascriptUtils.escapeForJavascript(GlobalUtils.lookup((String) code, locale))
      sb << """      {"$code": "$value"}"""
    }

    // Now, figure out the decimal separator for the current locale.
    def decimal = NumberUtils.determineDecimalSeparator(locale)
    if (sb.length() > 0) {
      sb << "\n,"
    }
    sb << """      {"_decimal_": "$decimal"}"""

    out << sb.toString()

    out << "    ]);\n"

    out << '</script>\n'

    write(out.toString())
  }

}
