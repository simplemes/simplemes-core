/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.UnitTestUtils

/**
 * Tests.
 */
class PagerMarkerSpec extends BaseMarkerSpecification {

  def "verify that marker works for the simple case"() {
    when: 'the content is created'
    def page = execute(source: '<@efPager uri="/some/url?name=ABC" total="35" size="10" from="10"/>')

    then: 'the pager is correct'
    def baseHref = "/some/url?name=ABC&amp;"
    !page.contains('from=10"')
    page.contains("""<a href="${baseHref}from=20&amp;size=10" class="webix_pager_item pager-button">3</a>""")
  }

  def "verify that the marker detects missing attributes - uri, from, total"() {
    when: 'the marker is built'
    execute(source: src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efPager', missing])

    where:
    src                                            | missing
    '<@efPager             total="35" from="10"/>' | 'uri'
    '<@efPager uri="/some" total="35"          />' | 'from'
    '<@efPager uri="/some"            from="10"/>' | 'total'
  }


}
