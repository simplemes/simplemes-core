package org.simplemes.mes.demand

import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class StartRequestSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  @Rollback
  def "verify that the request can be serialized/deserialized as JSON"() {
    given: 'an order with LSNs and work centers'
    setCurrentUser()
    def order = MESUnitTestUtils.releaseOrder(qty: 90.0, lsnTrackingOption: LSNTrackingOption.LSN_ONLY, lotSize: 100.0)
    def lsn = order.lsns[0]
    def workCenter = new WorkCenter(workCenter: 'ABC').save()

    and: 'a request to test JSON'
    def startRequest = new StartRequest(order: order, lsn: lsn, workCenter: workCenter)


    when: 'the request is serialized and deserialized'
    def s = Holders.objectMapper.writeValueAsString(startRequest)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the request matches'
    def json = new JsonSlurper().parseText(s)
    json.order == order.order
    json.lsn == lsn.lsn

    and: 'the JSON does not contain the order fields, just the ID'
    !s.contains('overallStatus')
  }
}
