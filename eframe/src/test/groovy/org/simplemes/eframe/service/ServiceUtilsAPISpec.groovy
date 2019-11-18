package org.simplemes.eframe.service

import org.simplemes.eframe.preference.service.UserPreferenceService
import org.simplemes.eframe.security.PasswordEncoderService
import org.simplemes.eframe.test.BaseAPISpecification
import sample.controller.SampleParentController

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests ServiceUtils that need an embedded server.
 */
class ServiceUtilsAPISpec extends BaseAPISpecification {

  def "verify that getAllControllers works"() {
    when: 'the services a found'
    def services = ServiceUtils.instance.allServices

    then: 'it contains the services'
    services.contains(UserPreferenceService)
    services.contains(PasswordEncoderService)

    and: 'not the controllers that do not end in Service'
    !services.contains(SampleParentController)

  }

}
