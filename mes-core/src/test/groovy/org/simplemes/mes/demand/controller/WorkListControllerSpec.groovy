package org.simplemes.mes.demand.controller

import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.FindWorkRequest
import org.simplemes.mes.demand.FindWorkResponse
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.WorkListService
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog

//import org.simplemes.eframe.misc.Limits
//import org.simplemes.eframe.test.ControllerTstHelper

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class WorkListControllerSpec extends BaseAPISpecification {

  @SuppressWarnings("unused")
  def dirtyDomains = [ActionLog, ProductionLog, Order, Product, WorkCenter]

  def "verify that the controller passes the standard controller test - security, task menu, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller WorkListController
      role 'OPERATOR'
    }
  }

  def "verify that findWork handles parameters"() {
    given: 'a controller with a mocked service'
    def controller = new WorkListController()
    def mockService = Mock(WorkListService)
    def expectedFindWorkRequest = new FindWorkRequest(max: 13, from: 2, findInWork: false, findInQueue: false)
    def expectedFindWorkResponse = new FindWorkResponse(totalAvailable: 231)
    1 * mockService.findWork(expectedFindWorkRequest) >> expectedFindWorkResponse
    0 * mockService._
    controller.workListService = mockService

    when: 'the request is sent to the controller'
    def res = controller.findWork(mockRequest([count: '13', start: '26', findInWork: 'false', findInQueue: 'false']), new MockPrincipal('jane', 'OPERATOR'))

    then: 'the response is correct'
    FindWorkResponse findWorkResponse = res.body.get() as FindWorkResponse
    findWorkResponse.totalAvailable == 231
  }

  @Rollback
  def "verify that findWork handles workCenter parameter"() {
    given: 'a work center'
    def workCenter = null
    WorkCenter.withTransaction {
      workCenter = new WorkCenter(workCenter: 'ABC').save()
    }

    and: 'a controller with a mocked service'
    def controller = new WorkListController()
    def mockService = Mock(WorkListService)
    def expectedFindWorkRequest = new FindWorkRequest(workCenter: workCenter)
    def expectedFindWorkResponse = new FindWorkResponse(totalAvailable: 237)
    1 * mockService.findWork(expectedFindWorkRequest) >> expectedFindWorkResponse
    0 * mockService._
    controller.workListService = mockService

    when: 'the request is sent to the controller'
    def res = controller.findWork(mockRequest([workCenter: 'ABC']), new MockPrincipal('jane', 'OPERATOR'))

    then: 'the response is correct'
    FindWorkResponse findWorkResponse = res.body.get() as FindWorkResponse
    findWorkResponse.totalAvailable == 237
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that findWork handles the simple case - via HTTP API "() {
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
