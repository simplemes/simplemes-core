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

  static specNeeds = JSON

  def "verify that JSON formatting of action works"() {
    given: 'an action'
    def action = new OrderLSNChangeAction(order: 'TEST', lsn: 'L1', qtyInQueue: 1.2, qtyInWork: 2.2)

    when: 'the object is formatted'
    def s = Holders.objectMapper.writeValueAsString(action)

    then: 'the JSON is correct'
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = new JsonSlurper().parseText(s)
    json.type == OrderLSNChangeAction.TYPE_ORDER_LSN_CHANGE
    json.order == 'TEST'
    json.lsn == 'L1'
    json.qtyInQueue == 1.2
    json.qtyInWork == 2.2
  }

  def "verify that the type is correct"() {
    given: 'an action'
    def action = new OrderLSNChangeAction(order: 'TEST')

    expect: 'right type'
    action.type == OrderLSNChangeAction.TYPE_ORDER_LSN_CHANGE
  }
}
