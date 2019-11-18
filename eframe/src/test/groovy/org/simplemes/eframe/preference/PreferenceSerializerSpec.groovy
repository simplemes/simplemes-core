package org.simplemes.eframe.preference

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class PreferenceSerializerSpec extends BaseSpecification {

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that the serializer can handle a simple preference"() {
    given: 'a preference'
    def p1 = [new ColumnPreference(column: 'col1a', width: 237), new DialogPreference(dialogID: 'dialog2a', width: 247)]
    def preference = new Preference(element: 'grid1', name: 'abc', settings: p1)

    when: 'the value is serialized'
    String s = new ObjectMapper().writeValueAsString(preference)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is valid'
    def json = new JsonSlurper().parseText(s)
    json.element == 'grid1'
    json.name == 'abc'
    json.settings.size() == 4

    and: 'the first setting pair is correct'
    json.settings[0] == ColumnPreference.name
    json.settings[1].column == 'col1a'
    json.settings[1].width == 237
    json.settings[1].sequence == null
    json.settings[1].sortLevel == null
    json.settings[1].sortAscending == null

    and: 'the second setting pair is correct'
    json.settings[2] == DialogPreference.name
    json.settings[3].dialogID == 'dialog2a'
    json.settings[3].width == 247
    json.settings[3].height == null
  }
}
