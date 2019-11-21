package org.simplemes.mes.demand.controller

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.mes.demand.FindWorkRequest
import org.simplemes.mes.demand.FindWorkResponse
import org.simplemes.mes.demand.service.WorkListService
import org.simplemes.mes.floor.domain.WorkCenter

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
class WorkListControllerSpec extends BaseSpecification {

  static specNeeds = [HIBERNATE]

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
    def expectedFindWorkRequest = new FindWorkRequest(max: 13, offset: 17, findInWork: false, findInQueue: false)
    def expectedFindWorkResponse = new FindWorkResponse(totalAvailable: 231)
    1 * mockService.findWork(expectedFindWorkRequest) >> expectedFindWorkResponse
    0 * mockService._
    controller.workListService = mockService

    when: 'the request is sent to the controller'
    def res = controller.findWork(mockRequest([max: '13', offset: '17', findInWork: 'false', findInQueue: 'false']), new MockPrincipal('jane', 'OPERATOR'))

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

}
