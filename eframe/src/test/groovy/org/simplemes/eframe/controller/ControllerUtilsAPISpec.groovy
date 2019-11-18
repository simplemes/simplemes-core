package org.simplemes.eframe.controller

import org.simplemes.eframe.test.BaseAPISpecification
import sample.controller.SampleParentController

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for ControllerUtils that need an embedded server.
 */
class ControllerUtilsAPISpec extends BaseAPISpecification {

  def "verify that getAllControllers works"() {
    when: 'the controllers a found'
    def controllers = ControllerUtils.instance.allControllers

    then: 'it contains the controllers'
    controllers.contains(SampleParentController)

  }
}
