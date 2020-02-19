package org.simplemes.mes.system.controller

import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockPrincipal

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
class SelectionControllerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, SERVER]

  def "verify that controller has secured methods and meets standard requirements"() {
    expect: 'the controller follows the standard requirements'
    ControllerTester.test {
      controller SelectionController
      role 'OPERATOR'
    }
  }

  def "verify that workCenterChanged persists the preference"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    when: 'the request is made'
    def s = """ {
      "page": "/dashboard",
      "workCenter": "WC137"
    }
    """
    new SelectionController().workCenterChanged(s, new MockPrincipal('jane', 'OPERATOR'))

    then: 'the new value is in the preferences'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/dashboard'
        user SecurityUtils.TEST_USER
        element SelectionController.WORK_CENTER_ELEMENT
      }
      SimpleStringPreference stringPreference = (SimpleStringPreference) preference[SelectionController.WORK_CENTER_ELEMENT]
      assert stringPreference.value == 'WC137'
      true
    }
  }

  def "verify that workCenterSelection use the latest workCenter from the user preferences"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    and: 'a value is set in the user preferences'
    UserPreference.withTransaction {
      PreferenceHolder holder = PreferenceHolder.find {
        page '/dashboard'
        user SecurityUtils.TEST_USER
        element SelectionController.WORK_CENTER_ELEMENT
      }
      holder.settings[0] = new SimpleStringPreference(SelectionController.WORK_CENTER_ELEMENT, 'WC237')
      holder.save()
    }

    when: 'the request is made'
    def res = new SelectionController().workCenterSelection(mockRequest(), new MockPrincipal('jane', 'OPERATOR'))

    then: 'the preference value is in the model'
    res.model.get().params.workCenter == 'WC237'
  }

  def "verify that workCenterSelection passes work center request argument to model"() {
    when: 'the page is rendered'
    def modelAndView = new SelectionController().workCenterSelection(mockRequest([workCenter: 'WC137']),
                                                                     new MockPrincipal('jane', 'OPERATOR'))

    then: 'the work center is part of the model'
    modelAndView.model.get().params.workCenter == 'WC137'
  }

  def "verify that a work center on the URL supercedes the work center from user preferences"() {
    given: 'a simulated current user is set'
    setCurrentUser()

    and: 'a value is set in the user preferences'
    UserPreference.withTransaction {
      PreferenceHolder holder = PreferenceHolder.find {
        page '/dashboard'
        user SecurityUtils.TEST_USER
        element SelectionController.WORK_CENTER_ELEMENT
      }
      holder.settings[0] = new SimpleStringPreference(SelectionController.WORK_CENTER_ELEMENT, 'WC237')
      holder.save()
    }

    when: 'the page is rendered with a workcenter in the URL params'
    def modelAndView = new SelectionController().workCenterSelection(mockRequest([workCenter: 'WC137']),
                                                                     new MockPrincipal('jane', 'OPERATOR'))

    then: 'the work center is part of the model'
    modelAndView.model.get().params.workCenter == 'WC137'
  }

  def "verify that changeWorkCenterDialog works with a work center passed in"() {
    when: 'the page is rendered'
    def modelAndView = new SelectionController().changeWorkCenterDialog(mockRequest([workCenter: 'WC137']),
                                                                        new MockPrincipal('jane', 'OPERATOR'))

    then: 'the work center is part of the model'
    modelAndView.model.get().params.workCenter == 'WC137'
  }

}
