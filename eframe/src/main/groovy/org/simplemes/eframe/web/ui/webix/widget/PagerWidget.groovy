/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget

import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.web.ui.UIDefaults

/**
 * The pager widget.  Produces the HTML needed for a generic pager.
 *
 * <h3>Parameters</h3>
 * This widget supports these parameters:
 * <ul>
 *   <li><b>id</b> - The ID for the pager.  This should be unique. </li>
 *   <li><b>from</b> - The current top-row being displayed. </li>
 *   <li><b>size</b> - The page size for each page (<b>Default</b>: 10). </li>
 *   <li><b>total</b> - The page size for each page. </li>
 *   <li><b>baseURI</b> - The base URI for the page buttons.  Adds from/size or page to the URI for each link. </li>
 *   </li>
 * </ul>
 */
class PagerWidget extends BaseWidget {

  /**
   * The maximum number of pages to display in the pager.  This is the max number in the center section
   * of the pager.  Up to two more pages are shown for the first/last pages.
   */
  static final int MAX_PAGES_IN_PAGER_CENTER = 10

  String idS = ''

  int from = 0
  int size = 0
  int total = 0

  String uri = ''

  /**
   * Basic constructor.
   * @param widgetContext The widget context this widget is operating in.  Includes URI, parameters, marker etc.
   */
  PagerWidget(WidgetContext widgetContext) {
    super(widgetContext)

    // Initialize values common to std button and sub-menu buttons.
    if (widgetContext.parameters.id) {
      idS = """id: "$widgetContext.parameters.id", """
    }
    from = ArgumentUtils.convertToInteger(widgetContext.parameters.from) ?: 0
    size = ArgumentUtils.convertToInteger(widgetContext.parameters.size) ?: UIDefaults.PAGE_SIZE
    total = ArgumentUtils.convertToInteger(widgetContext.parameters.total) ?: 0
    uri = widgetContext.parameters.uri
  }


  /**
   * Builds the text for the UI elements.
   * @return The UI page text.
   */
  @Override
  CharSequence build() {
    def sb = new StringBuilder()

    if (total > 1) {
      sb << '<div id="pagination" class="webix_view webix_pager">\n'

      int currentPage = (int) (from / size) + 1
      int nPages = NumberUtils.divideRoundingUp(total, size)
      def pagesToDisplay = determinePagerLinksNeeded(currentPage, nPages)
      def baseHref = "$uri&amp;"
      for (page in pagesToDisplay) {
        switch (page) {
          case '-':
            int firstPage = 1
            String spec = buildArgs(firstPage)
            sb << """<a href="${baseHref}$spec" class="webix_pager_item pager-button">&lt;&lt;</a>\n"""
            break
          case '+':
            def lastPage = NumberUtils.divideRoundingUp(total, size)
            String spec = buildArgs(lastPage)
            sb << """<a href="${baseHref}$spec" class="webix_pager_item pager-button">&gt;&gt;</a>\n"""
            break
          case '.':
            sb << """<span class="step gap">..</span>\n"""
            break
          default:
            if (page > 0) {
              String spec = buildArgs(page)
              sb << """<a href="${baseHref}$spec" class="webix_pager_item pager-button">$page</a>\n"""
            } else {
              sb << """<span class="webix_pager_item_selected pager-button">${-page}</span>\n"""
            }
        }
      }


      sb << '</div>\n'
    }
    return sb.toString()
  }

  /**
   * Builds the page arguments (e.g. 'from=N&size=L') for the link URL.
   * @param page The page for the link URL.  Page is 1-based (e.g. first page is 1).
   * @return The page URI spec.
   */
  String buildArgs(int page) {
    int from = (page - 1) * size
    return "from=$from&amp;size=$size"

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


}
