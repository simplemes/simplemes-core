package org.simplemes.mes.system

import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderLSNChangeActionSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that JSON formatting of action works"() {
    given: 'an action'
    def list = [new OrderLSNChangeDetail(order: 'TEST', lsn: 'L1', qtyInQueue: 1.2, qtyInWork: 2.2)]
    def action = new OrderLSNChangeAction(list: list)

    when: 'the object is formatted'
    def s = Holders.objectMapper.writeValueAsString(action)

    then: 'the JSON is correct'
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = new JsonSlurper().parseText(s)
    json.type == OrderLSNChangeAction.TYPE_ORDER_LSN_CHANGED
    List jsonList = json.list
    jsonList[0].order == 'TEST'
    jsonList[0].lsn == 'L1'
    jsonList[0].qtyInQueue == 1.2
    jsonList[0].qtyInWork == 2.2
  }

  def "verify that the type is correct"() {
    given: 'an action'
    def action = new OrderLSNChangeAction()

    expect: 'right type'
    action.type == OrderLSNChangeAction.TYPE_ORDER_LSN_CHANGED
  }
}
