/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.test.BaseWidgetSpecification
import org.simplemes.eframe.test.HTMLTestUtils

/**
 * Tests.
 */
class PagerWidgetSpec extends BaseWidgetSpecification {


  def "verify that the pager works for the basic case"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [uri: '/path?name=ABC', from: 10, total: 50])
    def page = new PagerWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the page is valid'
    HTMLTestUtils.checkHTML(page)

    and: 'the pager is in the correct div'
    page.contains('<div id="pagination"')

    and: 'the current page is flagged correctly'
    page.contains('<span class="webix_pager_item_selected pager-button">2</span>')

    and: 'the other pages are correct'
    def baseHref = "/path?name=ABC&amp;"
    !page.contains('from=10"')
    page.contains("""<a href="${baseHref}from=0&amp;size=10" class="webix_pager_item pager-button">1</a>""")
    page.contains("""<a href="${baseHref}from=20&amp;size=10" class="webix_pager_item pager-button">3</a>""")
    page.contains("""<a href="${baseHref}from=30&amp;size=10" class="webix_pager_item pager-button">4</a>""")
    page.contains("""<a href="${baseHref}from=40&amp;size=10" class="webix_pager_item pager-button">5</a>""")
    !page.contains('from=50"')

    and: 'the first and last page links are correct'
    page.contains("""<a href="${baseHref}from=0&amp;size=10" class="webix_pager_item pager-button">&lt;&lt;</a>""")
    page.contains("""<a href="${baseHref}from=40&amp;size=10" class="webix_pager_item pager-button">&gt;&gt;</a>""")

    and: 'the right number of total pager buttons is shown'
    page.count('<a href=') == 6
  }

  def "verify that the pager is supressed with no and zero total"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [uri: '/path?name=ABC', from: 10, total: total])
    def page = new PagerWidget(widgetContext).build().toString()

    then: 'the page is valid'
    HTMLTestUtils.checkHTML(page)

    and: 'the pager is in the correct div'
    !page.contains('<div id="pagination"')

    where:
    total | _
    null  | _
    0     | _
  }

  def "verify that determinePagerLinksNeeded works with various page counts and current page"() {
    expect: 'the pager links are determined'
    linksNeeded == PagerWidget.determinePagerLinksNeeded(currentPage, nPages)

    where:
    currentPage | nPages | linksNeeded
    1           | 1      | [-1]
    1           | 7      | [-1, 2, 3, 4, 5, 6, 7, '+']
    2           | 7      | ['-', 1, -2, 3, 4, 5, 6, 7, '+']
    7           | 7      | ['-', 1, 2, 3, 4, 5, 6, -7]
    1           | 20     | [-1, 2, 3, 4, 5, 6, 7, 8, 9, 10, '.', 20, '+']
    3           | 20     | ['-', 1, 2, -3, 4, 5, 6, 7, 8, 9, 10, '.', 20, '+']
    8           | 20     | ['-', 1, '.', 3, 4, 5, 6, 7, -8, 9, 10, 11, 12, '.', 20, '+']
    12          | 20     | ['-', 1, '.', 7, 8, 9, 10, 11, -12, 13, 14, 15, 16, '.', 20, '+']
    15          | 20     | ['-', 1, '.', 10, 11, 12, 13, 14, -15, 16, 17, 18, 19, 20, '+']
    18          | 20     | ['-', 1, '.', 10, 11, 12, 13, 14, 15, 16, 17, -18, 19, 20, '+']
  }

  def "verify that the pager works for the fractional page case"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [uri: '/path?name=ABC', from: 10, total: 55])
    def page = new PagerWidget(widgetContext).build().toString()
    //println "page = $page"

    then: 'the page is valid'
    HTMLTestUtils.checkHTML(page)

    and: 'the last page is correct'
    def baseHref = "/path?name=ABC&amp;"
    page.contains("""<a href="${baseHref}from=0&amp;size=10" class="webix_pager_item pager-button">1</a>""")
    page.contains("""<a href="${baseHref}from=20&amp;size=10" class="webix_pager_item pager-button">3</a>""")
    page.contains("""<a href="${baseHref}from=30&amp;size=10" class="webix_pager_item pager-button">4</a>""")
    page.contains("""<a href="${baseHref}from=40&amp;size=10" class="webix_pager_item pager-button">5</a>""")
    page.contains("""<a href="${baseHref}from=50&amp;size=10" class="webix_pager_item pager-button">6</a>""")
    !page.contains('from=50"')

    and: 'the last page link is correct'
    page.contains("""<a href="${baseHref}from=50&amp;size=10" class="webix_pager_item pager-button">&gt;&gt;</a>""")

    and: 'the right number of total pager buttons is shown'
    page.count('<a href=') == 7
  }

  def "verify that the pager works for the fractional page case - from starts on a non-boundary row"() {
    when: 'the UI element is built'
    def widgetContext = buildWidgetContext(parameters: [uri: '/path?name=ABC', from: 15, total: 55])
    def page = new PagerWidget(widgetContext).build().toString()

    then: 'the page is valid'
    HTMLTestUtils.checkHTML(page)

    and: 'the pages are still based on proper boundaries'
    def baseHref = "/path?name=ABC&amp;"
    page.contains("""<a href="${baseHref}from=0&amp;size=10" class="webix_pager_item pager-button">1</a>""")
    page.contains("""<a href="${baseHref}from=20&amp;size=10" class="webix_pager_item pager-button">3</a>""")
    page.contains("""<a href="${baseHref}from=30&amp;size=10" class="webix_pager_item pager-button">4</a>""")
    page.contains("""<a href="${baseHref}from=40&amp;size=10" class="webix_pager_item pager-button">5</a>""")
    page.contains("""<a href="${baseHref}from=50&amp;size=10" class="webix_pager_item pager-button">6</a>""")
    !page.contains('from=50"')

    and: 'the last page link is correct'
    page.contains("""<a href="${baseHref}from=50&amp;size=10" class="webix_pager_item pager-button">&gt;&gt;</a>""")

    and: 'the right number of total pager buttons is shown'
    page.count('<a href=') == 7
  }


}
