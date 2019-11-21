package org.simplemes.mes.demand.controller

import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.mes.demand.FindWorkRequest
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class WorkListControllerAPISpec extends BaseAPISpecification {

  def dirtyDomains = [ActionLog, ProductionLog, Order, Product, WorkCenter]

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that findWork handles the simple case "() {
    given: 'a work center and a released order'
    def order = null
    WorkCenter.withTransaction {
      new WorkCenter(workCenter: 'ABC').save()
      setCurrentUser()
      order = MESUnitTestUtils.releaseOrder([:])
    }

    and: 'the find work request'
    def s = Holders.objectMapper.writeValueAsString(new FindWorkRequest(findInWork: false))

    when: ''
    login()
    def res = sendRequest(uri: '/workList/findWork?workCenter=ABC', content: s)

    then: 'the released order is in the list'
    def json = new JsonSlurper().parseText((String) res)
    json.totalAvailable == 1
    json.list.size() == 1
    json.list[0].order == order.order

  }
}
