package org.simplemes.eframe.system

import ch.qos.logback.classic.Level
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.preference.TreeStatePreference
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.system.controller.LoggingController
import org.simplemes.eframe.system.page.LoggingPage
import org.simplemes.eframe.test.BaseGUISpecification
import sample.controller.SampleParentController
import sample.domain.SampleChild
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class LoggingGUISpec extends BaseGUISpecification {

  /**
   * The trace levels to check.
   */
  def levels = ['error', 'warn', 'info', 'debug', 'trace']

  /**
   * Creates a preference for an added logger and optionally with the given tree state open.
   * @param loggerName The logger to create as and added logger (can be null).
   * @param treeOpenedKey The lookup key for the tree level to be opened (e.g. 'others.label').  (<b>Optional</b>)
   * @return The UserPreference record ID.
   */
  private Object createPreference(String loggerName, String treeOpenedKey = null) {
    def res = null
    UserPreference.withTransaction {
      def preference = PreferenceHolder.find {
        page '/logging'
        user SecurityUtils.GUI_TEST_USER
        //element LoggingController.OTHERS_ELEMENT
      }
      if (loggerName) {
        preference.element = LoggingController.OTHERS_ELEMENT
        SimpleStringPreference stringPreference = new SimpleStringPreference(LoggingController.OTHERS_KEY)
        stringPreference.value = loggerName
        preference.setPreference(stringPreference)
      }

      if (treeOpenedKey) {
        preference.element = LoggingController.TREE_STATE_ELEMENT
        TreeStatePreference treeStatePreference = new TreeStatePreference()
        treeStatePreference.expandedKeys = lookup(treeOpenedKey)
        preference.setPreference(treeStatePreference)
      }

      preference.save()
      res = preference.userPreference
    }
    return res
  }

  /**
   * Utility method to return the HTML ID for the given logger name (title).
   * @param title The title (class name_ of the logger).
   * @return The internal ID used in the HTML for that row in the table.
   */
  @SuppressWarnings("GroovyAccessibility")
  String getIdForTitle(String title) {
    def treeData = new LoggingController().buildTreeData(SecurityUtils.GUI_TEST_USER)

    for (topLevelRow in treeData) {
      for (classRow in topLevelRow.data) {
        if (classRow.title == title) {
          return classRow.id
        }
      }
    }

    return null
  }

  /**
   * Verifies that the GUI shows the level status for the given logger ID.
   * @param id The internal ID for the level to check.
   * @param expectedLevel The level that should be checked.
   */
  boolean assertLevelState(String id, Level expectedLevel) {
    def expectedLevelString = expectedLevel.toString()
    for (level in levels) {
      def containsOn = false
      def assMessage = "Level $level should not be flagged as on for ID=$id.  Expected level on = $expectedLevelString "
      if (level == expectedLevelString) {
        containsOn = true
        assMessage = "Level $level should be flagged as on for ID=$id.  Expected level on = $expectedLevelString "
      }
      assert $("span#$level-$id").classes().contains("$level-on") == containsOn, assMessage
    }

    return true
  }

  def "verify that GUI displays the current logging state"() {
    given: 'a specific logging state'
    LogUtils.getLogger(SampleChild.name).level = Level.TRACE

    and: 'the id of the desired class is found'
    def sampleChildID = getIdForTitle(SampleChild.name)

    when: 'the GUI is displayed'
    login()
    to LoggingPage

    and: 'the domains element is opened'
    $('div', 'aria-rowindex': '1').find('div.webix_tree_close').click()

    then: 'the current level is shown'
    assertLevelState(sampleChildID, Level.TRACE)

    cleanup:
    LogUtils.getLogger(SampleChild.name).level = null
  }

  def "verify that the user can set a level"() {
    given: 'the id of the desired class is found'
    def sampleChildID = getIdForTitle(SampleChild.name)

    when: 'the GUI is displayed'
    login()
    to LoggingPage

    and: 'the domains element is opened'
    $('div', 'aria-rowindex': '1').find('div.webix_tree_close').click()

    and: 'the user clicks the record TRACE button'
    $("span#trace-$sampleChildID").click()
    // Wait for the level to change on the server-side
    waitFor() {
      LogUtils.getLogger(SampleChild.name).level == Level.TRACE
    }

    then: 'the client-side states is correct'
    assertLevelState(sampleChildID, Level.TRACE)

    cleanup:
    LogUtils.getLogger(SampleChild.name).level = null
  }

  def "verify that the user can un-set a level"() {
    given: 'a specific logging state'
    LogUtils.getLogger(SampleChild.name).level = Level.DEBUG

    and: 'the id of the desired class is found'
    def sampleChildID = getIdForTitle(SampleChild.name)

    when: 'the GUI is displayed'
    login()
    to LoggingPage

    and: 'the domains element is opened'
    $('div', 'aria-rowindex': '1').find('div.webix_tree_close').click()

    and: 'the user clicks the record DEBUG button'
    $("span#debug-$sampleChildID").click()
    // Wait for the level to change on the server-side
    waitFor() {
      LogUtils.getLogger(SampleChild.name).level == null
    }

    then: 'the client-side states is correct'
    assertLevelState(sampleChildID, null)

    cleanup:
    LogUtils.getLogger(SampleChild.name).level = null
  }

  def "verify that the expanded tree elements are preserved on next display"() {
    given: 'the ids of the a domain and a controller'
    def sampleChildID = getIdForTitle(SampleChild.name)
    def sampleParentControllerID = getIdForTitle(SampleParentController.name)

    when: 'the GUI is displayed'
    login()
    to LoggingPage

    and: 'the controllers element is opened'
    $('div', 'aria-rowindex': '2').find('div.webix_tree_close').click()
    waitFor {
      $("span#debug-$sampleParentControllerID").displayed
    }

    and: 'the domain element is opened'
    $('div', 'aria-rowindex': '1').find('div.webix_tree_close').click()
    waitFor {
      $("span#debug-$sampleChildID").displayed
    }

    then: 'the user preferences are created'
    waitFor() {
      def count = 0
      UserPreference.withTransaction {
        count = UserPreference.count()
      }
      count > 0
    }

    when: 'the page is re-displayed'
    to LoggingPage

    then: 'the two elements are opened'
    waitFor {
      $("span#debug-$sampleChildID").displayed
    }
    waitFor {
      $("span#debug-$sampleChildID").displayed
    }
  }

  def "verify that the user can add other level"() {
    given: 'some non-default setting for the added logger'
    def loggerName = 'another.test.logger'
    LogUtils.getLogger(loggerName).level = Level.DEBUG

    and: 'a user preference with the others entry opened'
    createPreference(null, 'others.label')

    when: 'the GUI is displayed'
    login()
    to LoggingPage

    then: 'the others entry has an add button with the correct tooltip'
    addLoggerButton.@title == lookup('addLogger.tooltip')

    when: 'the add other icon is clicked'
    addLoggerButton.click()
    waitFor { dialog0.exists }

    and: 'the custom logger is entered'
    otherLoggerTextField.value(loggerName)

    and: 'the user clicks the Ok button'
    dialog0.okButton.click()
    waitFor { !dialog0.exists }

    and: 'the preferences are written to the DB'
    waitFor() { nonZeroRecordCount(UserPreference) }

    then: 'the new element has the right data displayed - first new logger always starts at 9999'
    def newEntryID = '9999'
    assertLevelState(newEntryID, Level.DEBUG)

    and: 'the title is shown correctly'
    $("span#title-$newEntryID").text() == loggerName

    and: 'the remove button is shown correctly with the right tooltip'
    $("span#remove-$newEntryID").@title == lookup('removeLogger.tooltip')

    and: 'the level is stored in the user preferences correctly'
    def preference = PreferenceHolder.find {
      page '/logging'
      user SecurityUtils.GUI_TEST_USER
      element LoggingController.OTHERS_ELEMENT
    }
    SimpleStringPreference sPref = (SimpleStringPreference) preference[LoggingController.OTHERS_KEY]
    sPref.value.contains('another.test.logger')
  }

  def "verify that the user can set the level on the added logger"() {
    given: 'a user preference with the added logger and the Others entry is opened'
    def loggerName = 'another.test.logger'
    createPreference(loggerName, 'others.label')

    when: 'the GUI is displayed'
    login()
    to LoggingPage

    and: 'the id of the desired class is found'
    def otherLoggerID = getIdForTitle(loggerName)

    and: 'the user clicks the record TRACE button'
    $("span#trace-$otherLoggerID").click()
    // Wait for the level to change on the server-side
    waitFor() {
      LogUtils.getLogger(loggerName).level == Level.TRACE
    }

    then: 'the client-side states is correct'
    assertLevelState(otherLoggerID, Level.TRACE)

    cleanup:
    LogUtils.getLogger(otherLoggerID).level = null
  }

  def "verify that the user can remove the added logger"() {
    given: 'a user preference with the added logger and the Others entry is opened'
    def loggerName = 'another.test.logger'
    def userPreference = createPreference(loggerName, 'others.label')

    when: 'the GUI is displayed'
    login()
    to LoggingPage

    and: 'the id of the desired class is found'
    def otherLoggerID = getIdForTitle(loggerName)

    and: 'the user clicks the remove icon'
    $("span#remove-$otherLoggerID").click()

    and: 'the user preference record has been updated with the remove logger'
    waitForRecordChange(userPreference)

    then: 'the logger is no longer in the user preference'
    def preference = PreferenceHolder.find {
      page '/logging'
      user SecurityUtils.GUI_TEST_USER
      element LoggingController.OTHERS_ELEMENT
    }
    SimpleStringPreference stringPreference = (SimpleStringPreference) preference[LoggingController.OTHERS_KEY]
    !stringPreference.value.contains(loggerName)
  }

}
