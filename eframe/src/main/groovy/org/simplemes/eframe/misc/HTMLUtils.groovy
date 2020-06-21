/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.web.ui.webix.widget.PagerWidget


/**
 * Some general-purpose HTML generation utilities.
 *
 */
class HTMLUtils {
  /**
   * The maximum number of pages to display in the pager for the report.  This is the max number in the center section
   * of the pager.  Up to two more pages are shown for the first/last pages.
   */
  static final int MAX_PAGES_IN_PAGER_CENTER = 10

  /**
   * Builds the 'target' option for an HTML &gt;a&lt; link that will open in a new window/tab.
   * Has logic to prevent the opening in a new tab for GUI tests to simplify the test cleanup (not need to
   * close windows).
   * @return The target="_blank" if not in a GUI test.
   */
  static String buildTargetForLink() {
    return Holders.environmentTest ? '' : 'target="_blank"'
  }

  /**
   * Builds the pager, if needed.
   * @param currentPage The current page being displayed.
   * @param nPages The total number of pages available.
   * @param baseURI The base URI for the report page links.  Will add &page=N to the link.
   * @return The footer string.
   */
  static String buildPager(int currentPage, int nPages, String baseURI) {
    def sb = new StringBuilder()
    if (nPages > 1) {
      sb << '<div id="pagination">\n'

      def pagesToDisplay = PagerWidget.determinePagerLinksNeeded(currentPage, nPages)
      def baseHref = "$baseURI&amp;"

      for (page in pagesToDisplay) {
        switch (page) {
          case '-':
            def prevPage = currentPage - 1
            sb << """<a href="${baseHref}page=$prevPage" class="prevLink">&nbsp;&nbsp;&nbsp;&nbsp;</a>\n"""
            break
          case '+':
            def nextPage = currentPage + 1
            sb << """<a href="${baseHref}page=$nextPage" class="nextLink">&nbsp;&nbsp;&nbsp;&nbsp;</a>\n"""
            break
          case '.':
            sb << """<span class="step gap">..</span>\n"""
            break
          default:
            if (page > 0) {
              sb << """<a href="${baseHref}page=$page" class="step">$page</a>\n"""
            } else {
              sb << """<span class="currentStep">${-page}</span>\n"""
            }
        }
      }

      sb << '</div>'
    }

    return sb.toString()
  }


  /**
   * Formats an exception with stack trace for clear display on an HTML error page.
   * Should only be used in development/test mode.
   * @param exception The exception.
   * @return The formatted exception with stack trace.
   */
  static String formatExceptionForHTML(Throwable exception) {
    def sb = new StringBuilder()

    sb << "<h4>Stack Trace</h4>"
    sb << "${exception.toString()}<br>"

    for (line in exception.stackTrace) {
      sb << "&nbsp; at $line<br>\n"
    }


    return sb.toString()
  }


}
