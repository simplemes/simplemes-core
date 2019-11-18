package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the efLanguage Freemarker marker implementation.
 * This extracts the current request's locale and converts it to HTML language format.
 */
@SuppressWarnings("unused")
class LanguageMarker extends BaseMarker {

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    def locale = GlobalUtils.getRequestLocale() ?: Locale.default
    write(locale.toString())
  }

}
