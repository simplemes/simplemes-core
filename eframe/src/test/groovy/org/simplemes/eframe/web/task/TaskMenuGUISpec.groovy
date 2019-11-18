package org.simplemes.eframe.web.task

import org.openqa.selenium.Keys
import org.simplemes.eframe.security.page.UserListPage
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.page.HomePage
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the client-side of the Task Menu.
 */
@IgnoreIf({ !sys['geb.env'] })
class TaskMenuGUISpec extends BaseGUISpecification {

  @Override
  void cleanup() {
    js.exec('localStorage.clear()')
  }

  def "verify that the task menu works with mouse clicks"() {
    when: 'the menu is opened'
    login()
    to HomePage
    taskMenuButton.click()

    and: 'the admin branch is opened'
    $('div.webix_tree_item', webix_tm_id: 'admin').find('.webix_tree_close').click()

    and: 'the user page is displayed'
    $('a#_user').click()
    waitForCompletion()

    then: 'the user index page is displayed'
    at UserListPage
  }

  def "verify that the task menu remembers the last selection and tree state"() {
    when: 'the menu is opened'
    login()
    to HomePage
    taskMenuButton.click()

    and: 'the admin branch is opened'
    $('div.webix_tree_item', webix_tm_id: 'admin').find('.webix_tree_close').click()

    and: 'the user page is displayed'
    $('a#_user').click()
    waitForCompletion()

    then: 'the user index page is displayed'
    at UserListPage

    when: 'the menu is re-displayed'
    taskMenuButton.click()

    then: 'the admin branch is open'
    $('a#_user').displayed

    and: 'the user entry is selected'
    $('a#_user').parent('.webix_tree_item').classes().contains('webix_selected')
  }

  def "verify that the task menu works with keyboard navigation"() {
    when: 'the menu is opened'
    login()
    to HomePage
    taskMenuButton.click()

    and: 'the first branch is opened'
    sendKey(Keys.ARROW_DOWN)
    sendKey(Keys.SPACE)

    then: 'the first entry is displayed'
    sendKey(Keys.ARROW_DOWN)
    sendKey(Keys.ENTER)
    def taskMenus = TaskMenuHelper.instance.taskMenu
    def taskMenuItem = taskMenus.root.folders[0].menuItems[0]
    waitFor {
      currentUrl.endsWith(taskMenuItem.uri)
    }
  }

}
