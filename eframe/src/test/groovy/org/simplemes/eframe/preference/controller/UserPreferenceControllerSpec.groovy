/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.preference.controller

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.preference.BasePreferenceSetting
import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.preference.DialogPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.preference.service.UserPreferenceService
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.UnitTestUtils

/**
 *
 */
class UserPreferenceControllerSpec extends BaseAPISpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  UserPreferenceController controller = new UserPreferenceController()

  def setup() {
    controller.userPreferenceService = Holders.applicationContext.getBean(UserPreferenceService)
    // Always use a mock user for unit tests.
    setCurrentUser()
  }

  def "verify that controller follows standards - security etc"() {
    expect: 'the tester is run'
    ControllerTester.test {
      controller UserPreferenceController
      role SecurityRule.IS_AUTHENTICATED
    }
  }

  def "basic guiStateChanged test - good result"() {
    given: 'a state change request'
    def params = [element: 'SampleDomainList', event: 'ColumnResized', pageURI: '/eframe/sampleDomain/list',
                  column : 'description', newSize: '86']
    def json = new ObjectMapper().writeValueAsString(params)

    when: 'the request is made'
    def res = controller.guiStateChanged(json)

    then: 'the response is valid'
    res.status == HttpStatus.OK

    and: 'the new gui preference is stored'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/eframe/sampleDomain/list'
        user SecurityUtils.TEST_USER
        element 'SampleDomainList'
      }
      ColumnPreference columnPreference = (ColumnPreference) preference['description']
      assert columnPreference.width == 86
      true
    }
  }

  def "test findPreferences with no type passed in"() {
    given: 'a dialog state for two dialogs'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page '/app/test'
        user SecurityUtils.TEST_USER
        element '_dialogX'
      }
      def dialogPreference = new DialogPreference(width: 23.4, height: 24.5, left: 25.6, top: 26.7)
      preference.setPreference(dialogPreference).save()

      preference = PreferenceHolder.find {
        page '/app/test'
        user SecurityUtils.TEST_USER
        element '_dialogY'
      }
      dialogPreference = new DialogPreference(width: 99, height: 98, left: 97, top: 96)
      preference.setPreference(dialogPreference).save()
    }

    when: 'the request is made'
    def res = controller.findPreferences(mockRequest([pageURI: '/app/test', element: '_dialogX']))

    then: 'the response is valid'
    res._dialogX
    res._dialogX.width == 23.4
    res._dialogX.height == 24.5
    res._dialogX.left == 25.6
    res._dialogX.top == 26.7
  }

  def "test findPreferences with a preference type passed in"() {
    given: 'a dialog state'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page '/app/test'
        user SecurityUtils.TEST_USER
        element '_dialogX'
      }
      def dialogPreference = new DialogPreference(width: 23.4, height: 249.5, left: 25.6, top: 26.7)
      preference.setPreference(dialogPreference).save()
    }
    // Need to sleep to make sure transaction commits for some reason.
    // Without this, the second _dialogY record is lost.
    sleep(100)

    and: 'a second dialog state'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page '/app/test'
        user SecurityUtils.TEST_USER
        element '_dialogY'
      }
      def dialogPreference = new DialogPreference(width: 99, height: 987, left: 97, top: 96)
      preference.setPreference(dialogPreference).save()
      preference.userPreference.save()
    }
    // Need to sleep to make sure transaction commits for some reason.
    // Without this, the second _dialogY record is lost.
    sleep(100)

    and: 'a column preference'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page '/app/test'
        user SecurityUtils.TEST_USER
        element 'OrderList'
      }
      def columnPreference = new ColumnPreference(column: 'colA', width: 99)
      preference.setPreference(columnPreference).save()
    }
    // Need to sleep to make sure transaction commits for some reason.
    // Without this, the second _dialogY record is lost.
    sleep(100)

    when: 'the request is made'
    def res = controller.findPreferences(mockRequest([pageURI: '/app/test', preferenceType: 'DialogPreference']))

    then: 'the response is valid'
    res._dialogX
    res._dialogX.width == 23.4

    res._dialogY
    res._dialogY.width == 99

    and: 'the other preferenceTypes are not found'
    !res.OrderList
  }

  def "test findPreferences with empty result returned"() {
    when: 'the request is made'
    def res = controller.findPreferences(mockRequest([pageURI: '/app/test', preferenceType: 'DialogPreference']))

    then: 'the response is empty'
    res.keySet().size() == 0
  }

  def "test saveSimplePreference"() {
    when: 'the request is made'
    controller.saveSimplePreference([pageURI: '/test/app1', element: 'ABC', value: 'wc237'])

    then: 'the preference was saved'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/test/app1'
        user SecurityUtils.TEST_USER
        element 'ABC'
      }
      SimpleStringPreference stringPreference = (SimpleStringPreference) preference[BasePreferenceSetting.DEFAULT_KEY]
      assert stringPreference.value == 'wc237'
      true
    }
  }

  def "test saveSimplePreference with missing JSON input"() {
    when: 'the request is made'
    controller.saveSimplePreference([:])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['pageURI', 'not', 'allowed'])
  }

  def "test saveSimplePreference with missing element input"() {
    when: 'the request is made'
    controller.saveSimplePreference([pageURI: '/dummy'])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['element', 'not', 'allowed'])
  }

  def "test saveSimplePreference with empty value"() {
    when: 'the request is made'
    controller.saveSimplePreference([pageURI: '/test/app1', element: 'ABC', value: ''])

    then: 'the preference was saved'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/test/app1'
        user SecurityUtils.TEST_USER
        element 'ABC'
      }
      SimpleStringPreference stringPreference = (SimpleStringPreference) preference[BasePreferenceSetting.DEFAULT_KEY]
      assert stringPreference.value == ''
      true
    }
  }

  def "test findSimplePreference basic scenario"() {
    given: 'a string preference is saved'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page '/app/test'
        user SecurityUtils.TEST_USER
        element 'workCenter'
      }
      def stringPreference = new SimpleStringPreference(value: 'wc237')
      preference.setPreference(stringPreference).save()
    }

    when: 'the find request is made'
    def res = controller.findSimplePreference(mockRequest([pageURI: '/app/test', element: 'workCenter']))

    then: 'the preference is returned'
    res.value == 'wc237'
  }

  def "test findSimplePreference with empty value in the database"() {
    given: 'a string preference is saved'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page '/app/test'
        user SecurityUtils.TEST_USER
        element 'workCenter'
      }
      def stringPreference = new SimpleStringPreference(value: '')
      preference.setPreference(stringPreference).save()
    }

    when: 'the find request is made'
    def res = controller.findSimplePreference(mockRequest([pageURI: '/app/test', element: 'workCenter']))

    then: 'the preference is returned'
    res.value == ''
  }

  def "test findSimplePreference with missing pageURI input"() {
    when: 'the request is made'
    controller.findSimplePreference(mockRequest([element: '/dummy']))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['pageURI', 'not', 'allowed'])
  }


  def "test findSimplePreference with missing element input"() {
    when: 'the request is made'
    controller.findSimplePreference(mockRequest([pageURI: '/dummy']))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['element', 'not', 'allowed'])
  }

  def "test findSimplePreference with no preference in the database"() {
    when: 'the find request is made'
    def res = controller.findSimplePreference(mockRequest([pageURI: '/app/test', element: 'workCenter']))

    then: 'the preference is returned'
    res.value == null
  }

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

    and: 'use the admin user for this test'
    setCurrentUser(SecurityUtils.API_TEST_USER)

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
    disableSQLTrace()
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
    and: 'use the admin user for this test'
    setCurrentUser(SecurityUtils.API_TEST_USER)


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

    and: 'use the admin user for this test'
    setCurrentUser(SecurityUtils.API_TEST_USER)

    when: 'the request is made'
    login()
    def res = sendRequest(uri: '/userPreference/findSimplePreference?pageURI=/test&element=workCenter')
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"
    def json = new JsonSlurper().parse(res.bytes)

    then: 'the JSON contains the right data'
    json.value == 'wc237'
  }

}
