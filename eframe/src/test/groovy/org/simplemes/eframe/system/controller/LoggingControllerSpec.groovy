package org.simplemes.eframe.system.controller

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.preference.TreeStatePreference
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.preference.service.UserPreferenceService
import org.simplemes.eframe.security.PasswordEncoderService
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.service.ServiceUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockObjectMapper
import org.simplemes.eframe.test.MockPrincipal
import sample.controller.AllFieldsDomainController
import sample.controller.SampleParentController
import sample.domain.SampleChild
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class LoggingControllerSpec extends BaseSpecification {

  static specNeeds = [SERVER]

  /**
   * A dummy logger for testing.
   */
  public static final String LOGGER = 'org.gibberish.LoggingService'

  def setup() {
    // Reset the test logger to no level
    LogUtils.getLogger(LOGGER).setLevel(null)
    LogUtils.getLogger('org.gibberish').setLevel(null)

    // index() uses the mapper
    new MockObjectMapper(this).install()
  }

  void cleanup() {
    // Reset the test logger to no level
    LogUtils.getLogger(LOGGER).setLevel(null)
    LogUtils.getLogger('org.gibberish').setLevel(null)
    DomainUtils.instance = new DomainUtils()
    ControllerUtils.instance = new ControllerUtils()
    ServiceUtils.instance = new ServiceUtils()
  }

  /**
   * Verifies that the given map has the correct values for the given logger.
   * @param map The level state as a map for the client.
   * @param logger The logger name.
   * @param level The expected level.  Can be null.
   * @param effective The effective (string) level.  No check is made if null.
   */
  void assertLevelMapIsCorrect(Map map, String logger, Level level, String effective = null) {
    assert map.title == logger
    assert map.error == (level == Level.ERROR ? 'on' : 'off')
    assert map.warn == (level == Level.WARN ? 'on' : 'off')
    assert map.info == (level == Level.INFO ? 'on' : 'off')
    assert map.debug == (level == Level.DEBUG ? 'on' : 'off')
    assert map.trace == (level == Level.TRACE ? 'on' : 'off')

    if (effective) {
      // Check the effective flag
      assert map.effective == effective
    }
  }

  def "verify that the controller passes the standard controller test - security, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller LoggingController
      secured 'client', SecurityRule.IS_AUTHENTICATED
    }
  }

  def "verify that getLoggingLevels works for simple level"() {
    given: 'the logger level is set'
    LogUtils.getLogger(LOGGER).setLevel(Level.INFO)

    when: 'the level is found'
    def res = new LoggingController().getLoggingLevels(LOGGER)

    then: 'the level is correct'
    assertLevelMapIsCorrect(res, LOGGER, Level.INFO)
  }

  def "verify that getLoggingLevels flags the effective level correctly simple case"() {
    given: 'the logger level is set'
    LogUtils.getLogger('org.gibberish').setLevel(Level.DEBUG)

    when: 'the level is found'
    def res = new LoggingController().getLoggingLevels(LOGGER)

    then: 'the level is correct'
    assertLevelMapIsCorrect(res, LOGGER, null, 'debug')
  }

  def "verify that getLoggingLevels flags the effective level correctly when a specific level is set"() {
    given: 'the logger level is set'
    LogUtils.getLogger('org.gibberish').setLevel(Level.DEBUG)
    LogUtils.getLogger(LOGGER).setLevel(Level.TRACE)

    when: 'the level is found'
    def res = new LoggingController().getLoggingLevels(LOGGER)

    then: 'the level is correct'
    assertLevelMapIsCorrect(res, LOGGER, Level.TRACE, 'debug')
  }

  def "verify that setLoggingLevel works for simple level"() {
    given: 'the body of the POST'
    def params = [logger: LOGGER, level: 'info']
    def json = new ObjectMapper().writeValueAsString(params)

    when: 'the level is set'
    def res = new LoggingController().setLoggingLevel(json)

    then: 'the level is correct'
    LogUtils.getLogger(LOGGER).level == Level.INFO

    and: 'the return value is correct'
    assertLevelMapIsCorrect(res, LOGGER, Level.INFO)
  }

  def "verify that setLoggingLevel works for simple level - change level"() {
    given: 'the body of the POST'
    def params = [logger: LOGGER, level: 'debug']
    def json = new ObjectMapper().writeValueAsString(params)

    and: 'the level is already set'
    LogUtils.getLogger(LOGGER).level = Level.INFO

    when: 'the level is set'
    def res = new LoggingController().setLoggingLevel(json)

    then: 'the level is correct'
    LogUtils.getLogger(LOGGER).level == Level.DEBUG

    and: 'the return value is correct'
    assertLevelMapIsCorrect(res, LOGGER, Level.DEBUG)
  }

  def "verify that setLoggingLevel works for simple level - clear level"() {
    given: 'the body of the POST'
    def params = [logger: LOGGER, level: 'clear']
    def json = new ObjectMapper().writeValueAsString(params)

    and: 'the level is already set'
    LogUtils.getLogger(LOGGER).level = Level.INFO

    when: 'the level is set'
    def res = new LoggingController().setLoggingLevel(json)

    then: 'the level is correct'
    LogUtils.getLogger(LOGGER).level == null

    and: 'the return value is correct'
    assertLevelMapIsCorrect(res, LOGGER, null)
  }

  def "verify that index finds the right domains and generates the JSON for the tree data"() {
    given: 'some mocked domains'
    DomainUtils.instance = Mock(DomainUtils)
    DomainUtils.instance.allDomains >> [SampleParent, SampleChild]

    when: 'the index page is generated'
    def res = new LoggingController().index(null)

    then: 'the map contains the tree data for the domains'
    def model = res.model.get()
    def treeData = model.treeData
    treeData

    and: 'domain data list has the right entries'
    def domainEntry = treeData.find { it.title == GlobalUtils.lookup('domains.label') }
    domainEntry
    List domainList = domainEntry.data
    domainList.size() == 2

    and: 'the domains are in the right order'
    domainList[0].title == SampleChild.name
    domainList[1].title == SampleParent.name

    and: 'the JSON data is valid'
    def s = model.treeJSON
    def json = new JsonSlurper().parseText((String) s)
    json.size() >= 5
  }

  def "verify that index finds the right controllers and generates the JSON for the tree data"() {
    given: 'some mocked controllers'
    ControllerUtils.instance = Mock(ControllerUtils)
    ControllerUtils.instance.allControllers >> [SampleParentController, AllFieldsDomainController]

    when: 'the index page is generated'
    def res = new LoggingController().index(null)

    then: 'the map contains the tree data for the controllers'
    def model = res.model.get()
    def treeData = model.treeData
    treeData

    and: 'controller data list has the right entries'
    def controllerEntry = treeData.find { it.title == GlobalUtils.lookup('controllers.label') }
    controllerEntry
    List controllerList = controllerEntry.data
    controllerList.size() == 2

    and: 'the controllers are in the right order'
    controllerList[0].title == AllFieldsDomainController.name
    controllerList[1].title == SampleParentController.name

    and: 'the JSON data is valid'
    def s = model.treeJSON
    def json = new JsonSlurper().parseText((String) s)
    json.size() >= 5
  }

  def "verify that index finds the right services and generates the JSON for the tree data"() {
    given: 'some mocked services'
    ServiceUtils.instance = Mock(ServiceUtils)
    ServiceUtils.instance.allServices >> [PasswordEncoderService, UserPreferenceService]

    when: 'the index page is generated'
    def res = new LoggingController().index(null)

    then: 'the map contains the tree data for the services'
    def model = res.model.get()
    def treeData = model.treeData
    treeData

    and: 'service data list has the right entries'
    def serviceEntry = treeData.find { it.title == GlobalUtils.lookup('services.label') }
    serviceEntry
    List serviceList = serviceEntry.data
    serviceList.size() == 2

    and: 'the services are in the right order'
    serviceList[0].title == UserPreferenceService.name
    serviceList[1].title == PasswordEncoderService.name

    and: 'the JSON data is valid'
    def s = model.treeJSON
    def json = new JsonSlurper().parseText((String) s)
    json.size() >= 5
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that index finds the right client pages and generates the JSON for the tree data"() {
    given: 'some mocked controllers'
    ControllerUtils.instance = Mock(ControllerUtils)
    ControllerUtils.instance.allBrowserPaths >> ['/home', '/home/aPage', '/begin', '/last?with=params']
    ControllerUtils.instance.determineBaseURI(_) >> { args -> return new ControllerUtils().determineBaseURI(args[0]) }

    when: 'the index page is generated'
    def res = new LoggingController().index(null)

    then: 'the map contains the tree data for the views'
    def model = res.model.get()
    def treeData = model.treeData
    treeData

    and: 'client view data list has the right entries'
    def clientEntry = treeData.find { it.title == GlobalUtils.lookup('jsClient.label') }
    clientEntry
    List clientList = clientEntry.data
    clientList.size() == 6

    and: 'the special client loggers are in the right place'
    clientList[0].title == LoggingController.CLIENT_LOGGER
    clientList[1].title == LoggingController.CLIENT_TO_SERVER_LOGGER

    and: 'the views are in the right order too.'
    clientList[2].title == "${LoggingController.CLIENT_PREFIX}.begin"
    clientList[3].title == "${LoggingController.CLIENT_PREFIX}.home"
    clientList[4].title == "${LoggingController.CLIENT_PREFIX}.home.aPage"
    clientList[5].title == "${LoggingController.CLIENT_PREFIX}.last"

    and: 'the JSON data is valid'
    def s = model.treeJSON
    def json = new JsonSlurper().parseText((String) s)
    json.size() >= 5
  }

  def "verify that the correct others are in the tree data"() {
    when: 'the index page is generated'
    def res = new LoggingController().index(null)

    then: 'the map contains the tree data for the services'
    def model = res.model.get()
    def treeData = model.treeData
    treeData

    and: 'other entries list has the right entries'
    def othersEntry = treeData.find { it.title == GlobalUtils.lookup('others.label') }
    othersEntry
    List otherList = othersEntry.data
    otherList.size() == 2

    and: 'the other entry is marked to allow add button'
    othersEntry.add

    and: 'the other entries are in the right order'
    otherList[0].title == LoggingController.DEFAULT_OTHERS[0]
    otherList[1].title == LoggingController.DEFAULT_OTHERS[1]

    and: 'the JSON data is valid'
    def s = model.treeJSON
    def json = new JsonSlurper().parseText((String) s)
    json.size() >= 5
  }

  def "verify that index uses a list of other loggers stored in the user preferences"() {
    given: 'a user preference with several loggers'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/logging'
        user SecurityUtils.TEST_USER
        element LoggingController.OTHERS_ELEMENT
      }
      SimpleStringPreference stringPreference = new SimpleStringPreference(LoggingController.OTHERS_KEY)
      stringPreference.value = 'org.xyz.AClass,org.abc.ZClass'
      preference.setPreference(stringPreference).save()
    }

    when: 'the index page is generated'
    def res = new LoggingController().index(new MockPrincipal())

    then: 'the map contains the tree data for the others'
    def model = res.model.get()
    def treeData = model.treeData
    treeData

    and: 'other data list has the right entries'
    def othersEntry = treeData.find { it.title == GlobalUtils.lookup('others.label') }
    othersEntry
    List othersList = othersEntry.data
    othersList.size() == 4

    and: 'the others are in the right order'
    othersList[0].title == 'org.abc.ZClass'
    othersList[1].title == 'org.hibernate.SQL'
    othersList[2].title == 'org.hibernate.type'
    othersList[3].title == 'org.xyz.AClass'

    and: 'all are marked as allowing remove'
    for (other in othersList) {
      assert other.remove
    }
  }

  def "verify that index restores the tree state from the user preferences"() {
    given: 'a user preference with several loggers'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/logging'
        user SecurityUtils.TEST_USER
        element LoggingController.TREE_STATE_ELEMENT
      }
      TreeStatePreference treeStatePreference = new TreeStatePreference()
      treeStatePreference.expandedKeys = "${lookup('controllers.label')},${lookup('domains.label')}"
      preference.setPreference(treeStatePreference).save()
    }

    when: 'the index page is generated'
    def res = new LoggingController().index(new MockPrincipal())

    then: 'the map contains the tree data'
    def model = res.model.get()
    def treeData = model.treeData
    treeData

    and: 'the open flag is set correctly'
    def domainsEntry = treeData.find { it.title == GlobalUtils.lookup('domains.label') }
    def controllerEntry = treeData.find { it.title == GlobalUtils.lookup('controllers.label') }
    def servicesEntry = treeData.find { it.title == GlobalUtils.lookup('services.label') }
    def othersEntry = treeData.find { it.title == GlobalUtils.lookup('others.label') }

    domainsEntry.open
    controllerEntry.open
    !servicesEntry.open
    !othersEntry.open
  }

  def "verify that addOtherLogger works for for first custom logger"() {
    given: 'the logger level is set'
    LogUtils.getLogger(LOGGER).setLevel(Level.DEBUG)

    and: 'the body of the POST'
    def params = [logger: LOGGER]
    def json = new ObjectMapper().writeValueAsString(params)

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the logger is added'
    def res = new LoggingController().addOtherLogger(json)

    then: 'the logger is in the preferences'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/logging'
        user SecurityUtils.TEST_USER
        element LoggingController.OTHERS_ELEMENT
      }
      SimpleStringPreference stringPreference = (SimpleStringPreference) preference[LoggingController.OTHERS_KEY]
      assert stringPreference.value.contains(LOGGER)
      true
    }

    and: 'the response is good'
    res.status == HttpStatus.OK

    and: 'the response contains the new logger status'
    assertLevelMapIsCorrect((Map) res.body(), LOGGER, Level.DEBUG)
    res.body().remove == true
  }

  def "verify that addOtherLogger works for for second and later custom logger"() {
    given: 'a user preference with several loggers'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/logging'
        user SecurityUtils.TEST_USER
        element LoggingController.OTHERS_ELEMENT
      }
      SimpleStringPreference stringPreference = new SimpleStringPreference()
      stringPreference.value = 'org.xyz.AClass,org.abc.ZClass'
      preference.setPreference(stringPreference).save()
    }
    // Need to sleep to make sure transaction commits for some reason.
    // Without this, the second _dialogY record is lost.
    sleep(100)

    and: 'the body of the POST'
    def params = [logger: LOGGER]
    def json = new ObjectMapper().writeValueAsString(params)

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the logger is added'
    def res = null
    UserPreference.withTransaction {
      res = new LoggingController().addOtherLogger(json)
    }
    // Need to sleep to make sure transaction commits for some reason.
    // Without this, the second _dialogY record is lost.
    sleep(100)

    then: 'the logger is in the preferences'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/logging'
        user SecurityUtils.TEST_USER
        element LoggingController.OTHERS_ELEMENT
      }
      SimpleStringPreference stringPreference = (SimpleStringPreference) preference[LoggingController.OTHERS_KEY]
      assert stringPreference.value.contains(LOGGER)
      true
    }

    and: 'the response is good'
    res.status == HttpStatus.OK
  }

  def "verify that addOtherLogger works prevents duplicate entry in the list"() {
    given: 'the body of the POST'
    def params = [logger: LOGGER]
    def json = new ObjectMapper().writeValueAsString(params)

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the logger is added twice'
    new LoggingController().addOtherLogger(json)
    def res = new LoggingController().addOtherLogger(json)

    then: 'the logger is in the preferences only once'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/logging'
        user SecurityUtils.TEST_USER
        element LoggingController.OTHERS_ELEMENT
      }
      SimpleStringPreference stringPreference = (SimpleStringPreference) preference[LoggingController.OTHERS_KEY]
      assert stringPreference.value.contains(LOGGER)
      def list = stringPreference.value.tokenize(',').findAll { it == LOGGER }
      assert list.size() == 1
      true
    }

    and: 'the response is good'
    res.status == HttpStatus.OK
  }

  def "verify that addOtherLogger gracefully handles missing logger"() {
    when: 'the logger is added twice'
    def res = new LoggingController().addOtherLogger('{}')

    then: 'a bad request is returned'
    res.status == HttpStatus.BAD_REQUEST
  }

  def "verify that removeOtherLogger removes it from the list"() {
    given: 'a user preference with several loggers'
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/logging'
        user SecurityUtils.TEST_USER
        element LoggingController.OTHERS_ELEMENT
      }
      SimpleStringPreference stringPreference = new SimpleStringPreference(LoggingController.OTHERS_KEY)
      stringPreference.value = 'org.xyz.AClass,org.abc.YClass,org.abc.ZClass'
      preference.setPreference(stringPreference).save()
    }

    and: 'a simulated current user is set'
    setCurrentUser()

    when: 'the logger is removed'
    def json = new ObjectMapper().writeValueAsString([logger: 'org.abc.YClass'])
    new LoggingController().removeOtherLogger(json)

    then: 'the logger is removed from the preferences'
    String s = null
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/logging'
        user SecurityUtils.TEST_USER
        element LoggingController.OTHERS_ELEMENT
      }
      SimpleStringPreference stringPreference = (SimpleStringPreference) preference[LoggingController.OTHERS_KEY]
      s = stringPreference.value
    }
    !s.contains('org.abc.YClass')

    and: 'the remaining list is correct'
    def list = s.tokenize(',')
    list.size() == 2
    list[0] == 'org.xyz.AClass'
    list[1] == 'org.abc.ZClass'

    and: 'the leading comma is removed'
    !s.contains(',,')
  }

  def "verify that removeOtherLogger gracefully handles missing logger"() {
    when: 'the logger is added twice'
    def res = new LoggingController().removeOtherLogger('{}')

    then: 'a bad request is returned'
    res.status == HttpStatus.BAD_REQUEST
  }


}
