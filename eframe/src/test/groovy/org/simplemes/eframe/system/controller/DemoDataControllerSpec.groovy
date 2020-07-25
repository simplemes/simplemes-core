/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.system.controller


import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.security.Roles
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockPrincipal
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/**
 * Tests.
 */
class DemoDataControllerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, AllFieldsDomain]

  DemoDataController controller

  def setup() {
    controller = Holders.getBean(DemoDataController)
  }

  def "verify that the controller passes the standard controller test - security, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller DemoDataController
      role Roles.ADMIN
    }
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that index loads the demo records"() {
    when: 'the index is triggered'
    def res = controller.index(new MockPrincipal())

    then: 'the records are loaded'
    SampleParent.findByName('SAMPLE1')

    and: 'the model is correct'
    def model = res.model.get()
    def list = model.list
    list.size() == 1
    def map1 = list[0]
    map1.name == SampleParent.simpleName
    map1.uri == '/sampleParent'
    map1.count == 1
    map1.possible == 1

  }
}
