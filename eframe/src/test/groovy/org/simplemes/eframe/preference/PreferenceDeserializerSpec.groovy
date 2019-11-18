package org.simplemes.eframe.preference

import com.fasterxml.jackson.databind.ObjectMapper
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class PreferenceDeserializerSpec extends BaseSpecification {

  def "verify that deserialize works for preferences"() {
    given: 'the JSON to deserialize'
    def src = """
    {
        "element": "grid1",
        "name": "abc",
        "settings": [
            "org.simplemes.eframe.preference.ColumnPreference",
            {
                "column": "col1a",
                "sequence": null,
                "width": 237,
                "sortLevel": null,
                "sortAscending": null
            },
            "org.simplemes.eframe.preference.DialogPreference",
            {
                "dialogID": "dialog2a",
                "width": 247,
                "height": null,
                "left": null,
                "top": null
            }
        ]
    }"""

    when: 'the value is serialized'
    def pref = new ObjectMapper().readValue(src, Preference)
    //println "pref = $pref"

    then: 'the preference is correct'
    pref.element == 'grid1'
    pref.name == 'abc'
    pref.settings.size() == 2

    and: 'the first setting is correct'
    pref.settings[0] instanceof ColumnPreference
    pref.settings[0].column == 'col1a'
    pref.settings[0].width == 237
    pref.settings[0].sequence == null
    pref.settings[0].sortLevel == null
    pref.settings[0].sortAscending == null

    and: 'the second setting is correct'
    pref.settings[1] instanceof DialogPreference
    pref.settings[1].dialogID == 'dialog2a'
    pref.settings[1].width == 247
    pref.settings[1].height == null
  }

  def "verify that deserialize fails when the setting is not a valid TypeableJSONInterface"() {
    given: 'the JSON to deserialize'
    def src = """
    {
        "element": "grid1",
        "name": "abc",
        "settings": [
            "java.util.Date",
            {
                "name": "col1a"
            }
        ]
    }"""

    when: 'the value is serialized'
    new ObjectMapper().readValue(src, Preference)
    //println "pref = $pref"

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['Date', 'TypeableJSONInterface', 'Preference'])
  }

  def "verify that deserialize fails when the setting list has odd values"() {
    given: 'the JSON to deserialize'
    def src = """
    {
        "element": "grid1",
        "name": "abc",
        "settings": [
            "java.util.Date"
        ]
    }"""

    when: 'the value is serialized'
    new ObjectMapper().readValue(src, Preference)
    //println "pref = $pref"

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['Date', 'Preference'])
  }

  def "verify that deserialize fails when the setting value does not match the setting fields"() {
    given: 'the JSON to deserialize'
    def src = """
    {
        "element": "grid1",
        "name": "abc",
        "settings": [
            "org.simplemes.eframe.preference.ColumnPreference",
            {
                "dialogName": "col1a"
            }
        ]
    }"""


    when: 'the value is serialized'
    new ObjectMapper().readValue(src, Preference)
    //println "pref = $pref"

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['dialogName'])
  }

}
