/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.service


import org.simplemes.eframe.preference.service.UserPreferenceService
import org.simplemes.eframe.security.PasswordEncoderService
import org.simplemes.eframe.test.BaseAPISpecification
import sample.controller.SampleParentController

/**
 * Tests ServiceUtils that need an embedded server.
 */
class ServiceUtilsSpec extends BaseAPISpecification {

  /**
   * Determines if the given beanClass is the target class or a sub-class of it.
   * @param beanClass
   * @param targetClass
   * @return
   */
  boolean isClass(Class beanClass, Class targetClass) {
    return beanClass == targetClass || beanClass.getSuperclass() == targetClass
  }

  def "verify that getAllControllers works"() {
    when: 'the services are found'
    def services = ServiceUtils.instance.allServices

    then: 'it contains the services'
    services.find { isClass(it, UserPreferenceService) }
    services.find { isClass(it, PasswordEncoderService) }

    and: 'not the controllers that do not end in Service'
    !services.find { isClass(it, SampleParentController) }
  }

}
