package org.simplemes.eframe.preference.controller

import groovy.json.JsonSlurper
import org.simplemes.eframe.preference.BasePreferenceSetting
import org.simplemes.eframe.preference.DialogPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseAPISpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class UserPreferenceControllerAPISpec extends BaseAPISpecification {

  def "verify that findPreference works with real HTTP requests"() {
    given: 'a dialog state is saved'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page '/app/test'
        user SecurityUtils.API_TEST_USER
        element '_dialogX'
      }
      def dialogPreference = new DialogPreference(width: 23.4, height: 24.5, left: 25.6, top: 26.7)
      preference.setPreference(dialogPreference).save()
    }

    when: 'the request is made'
    login()

    then: 'the response it Ok'
    def res = sendRequest(uri: '/userPreference/findPreferences?pageURI=/app/test&element=_dialogX')
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"
    def json = new JsonSlurper().parse(res.bytes)

    and: 'the JSON contains the right data'
    json._dialogX
    json._dialogX.width == 23.4
    json._dialogX.height == 24.5
    json._dialogX.left == 25.6
    json._dialogX.top == 26.7
  }

  def "verify that saveSimplePreference works with real HTTP requests"() {
    given: 'the JSON body for the request'
    def json = """
      {
        "pageURI": "/test/app1",
        "element": "ABC",
        "value": "wc237"
      }
    """

    when: 'the request is made'
    login()
    sendRequest(uri: '/userPreference/saveSimplePreference', content: json, method: 'post')

    then: 'the preference was saved'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/test/app1'
        user SecurityUtils.API_TEST_USER
        element 'ABC'
      }
      SimpleStringPreference stringPreference = (SimpleStringPreference) preference[BasePreferenceSetting.DEFAULT_KEY]
      assert stringPreference.value == 'wc237'
      true
    }
  }

  def "verify that findSimplePreference works with real HTTP requests"() {
    given: 'a simple preference is saved'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page '/test'
        user SecurityUtils.API_TEST_USER
        element 'workCenter'
      }
      def stringPreference = new SimpleStringPreference(value: 'wc237')
      preference.setPreference(stringPreference).save()
    }

    when: 'the request is made'
    login()
    def res = sendRequest(uri: '/userPreference/findSimplePreference?pageURI=/test&element=workCenter')
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"
    def json = new JsonSlurper().parse(res.bytes)

    then: 'the JSON contains the right data'
    json.value == 'wc237'
  }

}
