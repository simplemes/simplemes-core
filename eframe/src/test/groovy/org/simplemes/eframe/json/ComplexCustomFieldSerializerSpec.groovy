package org.simplemes.eframe.json

import grails.gorm.transactions.Rollback
import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import sample.domain.Order
import sample.domain.OrderLine

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the JSON complex custom field serializer.
 */
class ComplexCustomFieldSerializerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, HIBERNATE]

  @Rollback
  def "verify that serialize handles custom child list from addition"() {
    given: 'a domain object with custom fields'
    def order = new Order(order: 'M1001')
    def orderLines = []
    orderLines << new OrderLine(sequence: 1, product: 'PROD1')
    orderLines << new OrderLine(sequence: 2, product: 'PROD2')
    orderLines << new OrderLine(sequence: 3, product: 'PROD3')
    order.setFieldValue('orderLines', orderLines)
    order.save()

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(order)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is correct'
    def json = new JsonSlurper().parseText(s)
    List orderLines2 = json.orderLines
    orderLines2.size() == 3
    orderLines2[0].sequence == 1
    orderLines2[0].product == 'PROD1'
    orderLines2[1].sequence == 2
    orderLines2[1].product == 'PROD2'
    orderLines2[2].sequence == 3
    orderLines2[2].product == 'PROD3'
  }

  @Rollback
  def "verify that serialize handles empty custom child list from addition"() {
    given: 'a domain object with custom fields'
    def order = new Order(order: 'M1001')
    order.save()

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(order)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is correct'
    def json = new JsonSlurper().parseText(s)
    json.orderLines == null
  }

  // test no complex custom field value


}
