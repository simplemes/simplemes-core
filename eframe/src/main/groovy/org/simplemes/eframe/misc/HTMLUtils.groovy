package org.simplemes.eframe.misc

import org.simplemes.eframe.application.Holders


/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

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

      def pagesToDisplay = determinePagerLinksNeeded(currentPage, nPages)
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
   * Determines which links are needed in a pager, given the current page and total number of pages.
   * @param currentPage
   * @param nPages
   * @return The list of pages needed.  This is a number for the page, negative for the current page, '.' for '..',
   *         '-' for previous,
   *         and '+' for the next page links.
   */
  static List determinePagerLinksNeeded(int currentPage, int nPages) {
    def res = []

    // The key sections are:
    //  prev first (..) center (..) last  next

    if (currentPage > 1) {
      res << '-'
    }

    // Figure out the size of the center section.
    def start = 1
    def end = nPages

    if ((end - start) > MAX_PAGES_IN_PAGER_CENTER) {
      // make the center section centered on the current page.
      start = currentPage - (int) (MAX_PAGES_IN_PAGER_CENTER / 2)
      if (start < 1) {
        start = 1
      }
      end = start + MAX_PAGES_IN_PAGER_CENTER - 1
      if (end > nPages) {
        // Make sure we don't go past the end of the actual pages
        start = nPages - MAX_PAGES_IN_PAGER_CENTER
        end = nPages
      }
    }

    // See if first element is needed
    if (start > 1) {
      res << 1
    }

    // See if .. is needed at start of list
    if (start > 1) {
      res << '.'
    }

    for (i in start..end) {
      if (i == currentPage) {
        res << (-1) * i
      } else {
        res << i
      }
    }

    // See if .. is needed at end of list
    if (end < (nPages - 1)) {
      res << '.'
    }

    // See if last link is needed
    if (end < nPages) {
      res << nPages
    }

    // See if next link is needed
    if (currentPage < nPages) {
      res << '+'
    }

    return res
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
