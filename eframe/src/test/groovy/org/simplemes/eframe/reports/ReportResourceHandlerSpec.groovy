package org.simplemes.eframe.reports

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Tests.
 */
class ReportResourceHandlerSpec extends BaseSpecification {

  def "verify that getResourcePath generates the correct path"() {
    given: 'a resource handler'
    def handler = new ReportResourceHandler('/path/reports')

    when: 'the path is generated'
    def path = handler.getResourcePath('image_0.png')

    then: 'the path is correct'
    path == '/report/image?image=/path/reports_image_0.png'
  }

  def "verify that handleResource generates the correct path"() {
    given: 'a resource handler'
    def handler = new ReportResourceHandler('/path/reports')

    and: 'simulated image data'
    def data = "ABC-DEF".bytes

    when: 'the resource is handled'
    handler.handleResource('image_0.png', data)

    then: 'image data is in the cache under the correct path'
    ReportResourceCache.instance.getResource('/path/reports_image_0.png') == data
  }
}
