/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.test.BaseMarkerSpecification

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

}
