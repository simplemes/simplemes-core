package org.simplemes.eframe.controller

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockPrincipal
import sample.controller.SampleParentController

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class StandardModelAndViewSpec extends BaseSpecification {

  def "verify that the constructor stores the right values in the right places - logged in scenario"() {
    given: 'a controller'
    def controller = new SampleParentController()

    and: 'a request with parameters is set as the current request'
    def parameters = [column: 'a', row: 'b', uri: '/theURI']
    def request = mockRequest(parameters)
    Holders.mockRequest = request

    when: 'the constructor is called'
    def modelAndView = new StandardModelAndView('theView', new MockPrincipal(), controller)

    then: 'the properties are in the model correctly - using the optional get method'
    def model = modelAndView.model.get()
    modelAndView.view.get() == 'theView'
    model[StandardModelAndView.MARKER_CONTEXT].controller == controller
    model[StandardModelAndView.MARKER_CONTEXT].uri == '/theURI'
    model[StandardModelAndView.LOGGED_IN] == true
    model[StandardModelAndView.USER_NAME] == SecurityUtils.TEST_USER
    model[StandardModelAndView.REQUEST] == request
    model[StandardModelAndView.PARAMS] == parameters
  }

  def "verify that the constructor stores the right values in hte right places - not logged in scenario"() {
    given: 'a controller'
    def controller = new SampleParentController()

    when: 'the constructor is called'
    def modelAndView = new StandardModelAndView('theView', null, controller)

    then: 'the properties are in the model correctly'
    def model = modelAndView.model.get()
    model[StandardModelAndView.LOGGED_IN] == false
    model[StandardModelAndView.USER_NAME] == null
  }

  def "verify that the get and put methods work on the model"() {
    given: 'a controller'
    and: 'a request with parameters is set as the current request'
    def parameters = [column: 'a', row: 'b', uri: '/theURI']
    def request = mockRequest(parameters)
    Holders.mockRequest = request

    when: 'the constructor is called'
    def modelAndView = new StandardModelAndView('theView', new MockPrincipal(), new SampleParentController())

    then: 'the properties are in the model correctly'
    modelAndView.view.get() == 'theView'
    modelAndView[StandardModelAndView.MARKER_CONTEXT].uri == '/theURI'
    modelAndView[StandardModelAndView.LOGGED_IN] == true
    modelAndView[StandardModelAndView.USER_NAME] == SecurityUtils.TEST_USER
    modelAndView[StandardModelAndView.REQUEST] == request
    modelAndView[StandardModelAndView.PARAMS] == parameters

    when: 'the model is updated'
    modelAndView[StandardModelAndView.USER_NAME] = 'sam'

    then: 'the value can be read'
    modelAndView[StandardModelAndView.USER_NAME] == 'sam'
  }


}
