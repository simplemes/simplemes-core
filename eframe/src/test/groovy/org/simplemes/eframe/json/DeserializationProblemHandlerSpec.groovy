/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.CustomOrderComponent
import sample.domain.Order

/**
 * Tests.
 */
class DeserializationProblemHandlerSpec extends BaseSpecification {

  static specNeeds = SERVER

  @Rollback
  def "verify that deserialize of custom child lists supports child records with domain references"() {
    given: 'foreign reference'
    def flexType = DataGenerator.buildFlexType()

    and: 'a domain with a custom field'
    def order = new Order(order: 'M1001').save()
    order.customComponents << new CustomOrderComponent(sequence: 237, assyDataType: flexType)
    order.save()

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(order)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the object is deleted'
    order.delete()
    assert CustomOrderComponent.list().size() == 0

    and: 'the object is re-created'
    def order2 = Holders.objectMapper.readValue(s, Order)

    then: 'the object is correct'
    order2.customComponents.size() == 1
    //noinspection GroovyAssignabilityCheck
    order2.customComponents[0].assyDataType == flexType

  }
}
