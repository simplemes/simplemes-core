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
class RefreshOrderStatusActionSpec extends BaseSpecification {

  static specNeeds = SERVER

  def "verify that JSON formatting of action works"() {
    given: 'an action'
    def action = new RefreshOrderStatusAction(order: 'TEST')

    when: 'the object is formatted'
    def s = Holders.objectMapper.writeValueAsString(action)

    then: 'the JSON is correct'
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = new JsonSlurper().parseText(s)
    json.type == RefreshOrderStatusAction.TYPE_REFRESH_ORDER_STATUS
    json.order == 'TEST'
  }

  def "verify that the type is correct"() {
    given: 'an action'
    def action = new RefreshOrderStatusAction(order: 'TEST')

    expect: 'right type'
    action.type == RefreshOrderStatusAction.TYPE_REFRESH_ORDER_STATUS
  }
}
