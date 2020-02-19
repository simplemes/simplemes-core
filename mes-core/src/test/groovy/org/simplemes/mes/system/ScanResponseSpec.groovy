package org.simplemes.mes.system

import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.StartRequest
import org.simplemes.mes.demand.StartResponse
import org.simplemes.mes.demand.StartUndoAction
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ScanResponseSpec extends BaseSpecification {

  static specNeeds = [JSON, SERVER]

  def "tests the copy constructor for a ScanRequest"() {
    given: 'a scan request'
    def scanRequest = new ScanRequest(barcode: 'GIBBERISH')

    when: 'the copy constructor is used'
    def scanResponse = new ScanResponse(scanRequest)

    then: 'the right values are copied to the new object'
    scanResponse.barcode == 'GIBBERISH'
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that JSON form of the response is consistent with the format the javascript client expects"() {
    // This enforces compatibility with the Javascript client (mes_dashboard.js _handleScanResponse() function).
    given: 'a scan request'
    def scanRequest = new ScanRequest(barcode: 'GIBBERISH')
    def scanResponse = new ScanResponse(scanRequest)

    and: 'a message is in the holder'
    scanResponse.messageHolder.addInfo([text: 'info msg'])

    and: 'some scan actions'
    def scanAction0 = new ButtonPressAction(button: 'TEST')
    def scanAction1 = new ButtonPressAction(button: 'REFRESH')
    scanResponse.scanActions << scanAction0
    scanResponse.scanActions << scanAction1

    and: 'some undo actions'
    def startUndoAction = new StartUndoAction(new StartResponse(qty: 1.2, operationSequence: 247))
    scanResponse.undoActions << startUndoAction

    when: 'the response is formatted in JSON'
    def s = Holders.objectMapper.writeValueAsString([scanResponse: scanResponse])

    then: 'the JSON is correct'
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = new JsonSlurper().parseText(s)
    json.scanResponse.barcode == scanRequest.barcode
    json.scanResponse.resolved == scanResponse.resolved

    and: 'the scanActions are correct'
    json.scanResponse.scanActions.size() == 2
    json.scanResponse.scanActions[0].type == scanAction0.type
    json.scanResponse.scanActions[0].button == scanAction0.button
    json.scanResponse.scanActions[1].type == scanAction1.type
    json.scanResponse.scanActions[1].button == scanAction1.button

    and: 'the undoActions are correct and the internal JSON for undo is correct'
    json.scanResponse.undoActions.size() == 1
    json.scanResponse.undoActions[0].uri == startUndoAction.URI
    def startRequest = (StartRequest) Holders.objectMapper.readValue(json.scanResponse.undoActions[0].json, StartRequest)
    startRequest.operationSequence == 247
    startRequest.qty == 1.2

    and: 'the message holder is correct'
    json.scanResponse.messageHolder.message.text == 'info msg'
    json.scanResponse.messageHolder.message.level == 'info'

  }


  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that JSON form of the response has a valid order format"() {
    given: 'a scan request'
    def scanRequest = new ScanRequest(barcode: 'GIBBERISH')
    def scanResponse = new ScanResponse(scanRequest)

    and: 'a message is in the holder'
    scanResponse.order = new Order(order: 'ABC').save()

    when: 'the response is formatted in JSON'
    def s = Holders.objectMapper.writeValueAsString(scanResponse)

    then: 'the JSON is correct'
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = new JsonSlurper().parseText(s)
    json.scanResponse.order == 'ABC'
  }

}
