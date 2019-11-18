package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.system.controller.LoggingController
import org.simplemes.eframe.web.ui.webix.DomainToolkitUtils

import java.text.SimpleDateFormat

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the efGUISetup freemarker implementation.
 * This extracts the current request's locale and includes the correct language files.
 */
class GUISetupMarker extends BaseMarker {

  @Override
  void execute() {
    def locale = GlobalUtils.getRequestLocale() ?: Locale.default
    def sb = new StringBuilder()
    def lang = locale?.language ?: 'en'
    def langHTML = locale?.toLanguageTag() ?: 'en-US'
    def assetPath = AssetMarker.getAssetPath("/assets/i18n/${lang}.js", this)
    sb << """<script src="$assetPath" type="text/javascript" charset="utf-8"></script>\n"""
    sb << """<script type="text/javascript">webix.i18n.setLocale("$langHTML");\n"""
    sb << """  webix.i18n.fullDateFormat = "${getFullDateFormat(locale)}";\n"""
    sb << """  webix.i18n.dateFormat = "${getDateFormat(locale)}";\n"""
    sb << """  webix.i18n.parseFormat = "%Y-%m-%d %H:%i:%s";\n"""
    sb << """  webix.i18n.setLocale();\n"""
    sb << buildLoggerSetup()
    sb << """</script>\n"""

    write(sb.toString())
    body?.render(environment.getOut())
  }

  /**
   * Gets the full date/time format for the given locale.
   * @param locale The locale.
   * @return The toolkit full date format.
   */
  String getFullDateFormat(Locale locale) {
    return DomainToolkitUtils.instance.convertDateFormatToToolkit((SimpleDateFormat) DateUtils.getDateFormat(locale))
  }

  /**
   * Gets the date only format for the given locale.
   * @param locale The locale.
   * @return The toolkit date format.
   */
  String getDateFormat(Locale locale) {
    return DomainToolkitUtils.instance.convertDateFormatToToolkit((SimpleDateFormat) DateUtils.getDateOnlyFormat(locale))
  }

  /**
   * Builds the javascript logger setup for this page.
   * @returns The javascript to initialize the logger.
   */
  String buildLoggerSetup() {
    // Find the effective logging level for the page.
    def page = markerContext?.uri
    if (!page) {
      return ''
    }
    def loggerName = ControllerUtils.instance.determineBaseURI(LogUtils.convertPageToClientLoggerName(page))
    def level = LogUtils.getLogger(loggerName).effectiveLevel
    def clientLevel = LogUtils.convertLevelToClientLogSetting(level)
    def url = "/logging/client?logger=${loggerName}"

    def toServerClientLevel = 4000
    def toServerLevel = LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).level
    if (toServerLevel) {
      toServerClientLevel = LogUtils.convertLevelToClientLogSetting(toServerLevel)
    }

    return """
        JL().setOptions({"level": ${clientLevel},
                         "appenders": [JL.createAjaxAppender('ajaxAppender').setOptions({"url": "${
      url
    }", "level": $toServerClientLevel}),
                          JL.createConsoleAppender('consoleAppender')]});
    """

  }

  // TODO: Support theme.
}
