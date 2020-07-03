package org.simplemes.mes.system


import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ButtonPressActionSpec extends BaseSpecification {

  static specNeeds = SERVER

  def "verify that JSON formatting of action works"() {
    given: 'an action'
    def action = new ButtonPressAction(button: 'TEST')

    when: 'the object is formatted'
    def s = Holders.objectMapper.writeValueAsString(action)

    then: 'the JSON is correct'
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = new JsonSlurper().parseText(s)
    json.type == ButtonPressAction.TYPE_BUTTON_PRESS
    json.button == 'TEST'
  }

  def "verify that the type is correct"() {
    given: 'an action'
    def action = new ButtonPressAction(button: 'TEST')

    expect: 'right type'
    action.type == ButtonPressAction.TYPE_BUTTON_PRESS
  }
}
