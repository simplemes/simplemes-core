/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.annotation

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseAPISpecification
import sample.service.OrderService
import sample.service.OrderServiceExtension

/**
 *
 */
class ExtensiblePointHelperAPISpec extends BaseAPISpecification {

  def "verify that the extension point logic works in a running app server"() {
    given: 'the extension bean'
    def extensionBean = Holders.getBean(OrderServiceExtension)
    extensionBean.preOrderValue = null

    when: 'the core service method is called'
    def service = Holders.getBean(OrderService)
    def s = service.release('M1001')

    then: 'the extension was called'
    s.contains('OrderServiceExtension called')
    s.contains('M1001')

    and: 'the pre method was called'
    extensionBean.preOrderValue == 'M1001'

  }
}
