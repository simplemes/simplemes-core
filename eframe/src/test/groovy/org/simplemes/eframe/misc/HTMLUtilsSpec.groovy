/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.HTMLTestUtils

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
