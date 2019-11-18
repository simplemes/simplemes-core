package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.HTMLTestUtils

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Tests.
 */
class HTMLUtilsSpec extends BaseSpecification {

  def "verify that buildTargetForLink works for unit tests"() {
    expect: 'the target is set to not open in new tab'
    assert HTMLUtils.buildTargetForLink() == ''
  }

  def "verify that buildPager handles less than 10 pages"() {
    when: 'the pager is built'
    def page = HTMLUtils.buildPager(2, 7, "/path?name=ABC")

    then: 'the HTML is valid'
    def cleanedUpPage = page.replaceAll('&nbsp;', ' ')
    HTMLTestUtils.checkHTML(cleanedUpPage)

    and: 'the pager is in the correct div'
    page.contains('<div id="pagination">')

    and: 'the current page is displayed correctly'
    page.contains('<span class="currentStep">2</span>')

    and: 'the other pages are correct'
    def baseHref = "/path?name=ABC&amp;"
    !page.contains('page=2"')
    page.contains("""<a href="${baseHref}page=1" class="step">1</a>""")
    page.contains("""<a href="${baseHref}page=3" class="step">3</a>""")
    page.contains("""<a href="${baseHref}page=4" class="step">4</a>""")
    page.contains("""<a href="${baseHref}page=5" class="step">5</a>""")
    page.contains("""<a href="${baseHref}page=6" class="step">6</a>""")
    page.contains("""<a href="${baseHref}page=7" class="step">7</a>""")
    !page.contains('page=8"')

    and: 'the prev page and next page links are correct'
    page.contains("""<a href="${baseHref}page=1" class="prevLink">""")
    page.contains("""<a href="${baseHref}page=3" class="nextLink">""")
  }

  def "verify that determinePagerLinksNeeded works with various page counts and current page"() {
    expect: 'the pager links are determined'
    linksNeeded == HTMLUtils.determinePagerLinksNeeded(currentPage, nPages)

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

  def "verify that buildPager does not build a pager if page count is 1"() {
    when: 'the footer is built'
    def footer = HTMLUtils.buildPager(1, 1, "/report...")

    then: 'the footer is in the correct div'
    !footer.contains('<div id="pagination">')
  }

  def "verify that formatExceptionForHTML works for basic HTML formatting"() {
    when: 'the footer is built'
    def page = HTMLUtils.formatExceptionForHTML(new IllegalArgumentException('Some text message'))

    then: 'the exception and stack trace are formatted'
    page.contains("<h4>Stack Trace</h4>")
    page.contains("Some text message<br>")
    page.contains("&nbsp; at ${this.class.name}")
  }


}
