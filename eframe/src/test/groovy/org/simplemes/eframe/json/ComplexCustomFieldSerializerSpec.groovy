/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.CustomOrderComponent
import sample.domain.Order

/**
 * Tests the JSON complex custom field serializer.
 */
class ComplexCustomFieldSerializerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "verify that serialize handles custom child list from addition"() {
    given: 'a domain object with custom fields'
    def order = new Order(order: 'M1001')
    def customComponents = []
    customComponents << new CustomOrderComponent(sequence: 1, product: 'PROD1')
    customComponents << new CustomOrderComponent(sequence: 2, product: 'PROD2')
    customComponents << new CustomOrderComponent(sequence: 3, product: 'PROD3')
    order.setFieldValue('customComponents', customComponents)
    order.save()

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(order)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is correct'
    def json = new JsonSlurper().parseText(s)
    List customComponents2 = json.customComponents
    customComponents2.size() == 3
    customComponents2[0].sequence == 1
    customComponents2[0].product == 'PROD1'
    customComponents2[1].sequence == 2
    customComponents2[1].product == 'PROD2'
    customComponents2[2].sequence == 3
    customComponents2[2].product == 'PROD3'
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
    json.customComponents.size() == 0
  }

  // test no complex custom field value


}
