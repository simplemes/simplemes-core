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
class OrderLSNStatusChangedActionSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static specNeeds = SERVER

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that JSON formatting of action works"() {
    given: 'an action'
    def list = [new OrderLSNStatusChangeDetail(order: 'TEST', lsn: 'SN001')]
    def action = new OrderLSNStatusChangedAction(list: list)

    when: 'the object is formatted'
    def s = Holders.objectMapper.writeValueAsString(action)

    then: 'the JSON is correct'
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = new JsonSlurper().parseText(s)
    json.type == OrderLSNStatusChangedAction.TYPE_ORDER_LSN_STATUS_CHANGED
    json.list[0].order == 'TEST'
    json.list[0].lsn == 'SN001'
  }
}
