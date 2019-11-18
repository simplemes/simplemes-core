package org.simplemes.eframe.test

import org.simplemes.eframe.controller.ControllerUtils
import sample.controller.AllFieldsDomainController
import sample.controller.SampleParentController

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for the MockControllerUtils methods.
 */
class MockControllerUtilsSpec extends BaseSpecification {

  def "verify that the original method can be called to allow testing of the method"() {
    given: 'some mocked controllers, using the original getControllerByName method for testing'
    new MockControllerUtils(this, [AllFieldsDomainController, SampleParentController], ['getControllerByName']).install()

    expect: 'the controller is found'
    ControllerUtils.instance.getControllerByName('SampleParentController') == SampleParentController

  }
}
