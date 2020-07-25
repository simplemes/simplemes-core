package org.simplemes.mes.assy.application

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.system.controller.DemoDataController
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.product.domain.Product

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DemoDataLoaderSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ProductComponent, Product, FlexType]

  DemoDataController controller

  @SuppressWarnings('unused')
  def setup() {
    controller = Holders.getBean(DemoDataController)
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that the loader loads the demo records"() {
    when: 'the index is triggered'
    def res = controller.index(new MockPrincipal())

    then: 'the records are loaded'
    def flexType = FlexType.findByFlexType('LOT')
    flexType

    and: 'the components are correct'
    def seat = Product.findByProduct('SEAT')
    seat
    seat.assemblyDataType == flexType

    def wheel = Product.findByProduct('WHEEL-27')
    wheel
    wheel.assemblyDataType == flexType

    and: 'the main product is correct'
    def bike = Product.findByProduct('BIKE-27')
    bike
    bike.components.size() == 2
    bike.components[0].component == seat
    bike.components[0].qty == 1.0
    bike.components[1].component == wheel
    bike.components[1].qty == 2.0

    and: 'the model is correct'
    def model = res.model.get()
    def list = model.list
    list.size() == 2

    def map1 = list[0]
    map1.name == FlexType.simpleName
    map1.uri == '/flexType'
    map1.count == 1
    map1.possible == 1

    def map2 = list[1]
    map2.name == Product.simpleName
    map2.uri == '/product'
    map2.count == 3
    map2.possible == 3

  }

}
