package org.simplemes.eframe.test

import geb.navigator.Navigator
import groovy.util.logging.Slf4j
import org.openqa.selenium.Point
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.test.page.HomePage
import org.simplemes.eframe.test.page.LoginPage
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This is the common Spock specification base class for GUI/GEB tests.
 * This includes login/out and debug functions for GEB elements.
 */
@Slf4j
class BaseGUISpecification extends BaseSpecification {

  /**
   * Makes sure the GUI test has GEB GUI, embedded server and hibernate.
   */
  static specNeeds = [GUI, HIBERNATE]

  /**
   * Tracks the current user logged in.
   */
  static String loggedInUser

  /**
   * The current local being tested.  Comes from the -Dgeb.lang option (e.g. -Dgeb.lang=de-DE).  (<b>Default</b>: en-US).
   */
  Locale currentLocale = Locale.US

  /**
   * Initializes the GUI, if needed.
   */
  def setup() {
    browser.config.autoClearCookies = false
    //println "headless = $headless"
    if (!headless) {
      // Now, shift the window horizontally if desired.
      def w = driver.manage().window()
      if (w.position.x > -20) {
        w.position = new Point(w.position.x - 1500, 10)
        w.maximize()
      }
    }
    browser.baseUrl = "http://localhost:${embeddedServer.port}"

    // Grab the specific language, if given.
    if (System.getProperty('geb.lang')) {
      currentLocale = Locale.forLanguageTag(System.getProperty('geb.lang'))
      log.debug('Using locale {} from -Dgeb.lang option {} ', currentLocale, System.getProperty('geb.lang'))
    }

    // Check to make sure this test has the IgnoreIf annotation to prevent execution on non-GUI servers.
    if (!this.getClass().isAnnotationPresent(IgnoreIf)) {
      log.warn("Test Spec (${this.getClass().name}) does not specify the @IgnoreIf annotation for geb tests.")
    }
  }

  /**
   * Logs the user in as the given user, if not already logged in.
   * If the user is different from the previous login, then the user will be logged out before the new user is logged in.
   * @param _userName The user to login as <b>Required</b>.
   * @param passwordInput The password to login with (<b>Default</b>: userName).
   */
  void login(String _userName = null, String passwordInput = null) {
    _userName = _userName ?: 'admin'
    log.debug("Checking userName = {} loggedInUser = {}", _userName, loggedInUser)
    //println "userName = $userName, loggedInUser = $loggedInUser"
    if (loggedInUser) {
      // Already logged in, so make sure it is the right user
      if (_userName == loggedInUser) {
        // No change in user, so ignore the login() request.
        return
      }
      // Force a logout, then we can log in as the new user.
      log.debug("Logging out {} to switch to user {}", loggedInUser, _userName)
      logout()
    }
    if (passwordInput == null) {
      passwordInput = _userName
    }

    to LoginPage
    assert at(LoginPage)
    userName << _userName
    password << passwordInput
    loginButton.click()
    def url = new URL(currentUrl)
    if (url.path != '/') {
      log.error('Not at home page after login.  At {} ', currentUrl)
      browser.page(HomePage)
      to HomePage
    }
    loggedInUser = _userName
  }

  /**
   * Logs the user out.  Will navigate to the home page if no logout button is found.
   */
  void logout() {
    if (loggedInUser) {
      log.debug("Logging out user {} ", loggedInUser)
      if (findLogoutButton().displayed && !$("#_dialog0").displayed) {
        findLogoutButton().click()
      } else {
        to HomePage
        findLogoutButton().click()
      }
      loggedInUser = null
    }
  }


  /**
   * Finds the logout button on the current page.  This is needed since in most pages, the page context is not always know for a given test.
   * A simple 'logoutButton.click()' frequently fails when a new test starts, so we use the explicit $() below instead
   * of the simple page-based approach.
   * @return The logout button.
   */
  protected findLogoutButton() {
    return $('div', view_id: 'logoutButton')
  }


  static final List<String> dumpAttributes = ['view_id', 'view_id', 'column', 'aria-rowindex', 'disabled']
  /**
   * Creates a human readable debug representation of the given element and its children (recursive).
   * @param elementList The element (can be a list).
   * @param level The nesting level (optional).
   * @return The element and its children in a debug format.
   */
  String dumpElement(elementList, int level = 0) {
    def sb = new StringBuilder()
    def spacing = '  ' * level
    for (element in elementList) {
      def idString = element.@id ? " id='${element.@id}'" : ''
      def roleString = element.@role ? " role='${element.@role}'" : ''
      def classString = element.@class ? " class='${element.@class}'" : ''
      def styleString = element.@style ? " style='${element.@style}'" : ''
      def attrs = new StringBuilder()
      for (attr in dumpAttributes) {
        def s = element.getAttribute(attr)
        if (s) {
          attrs << " $attr='$s'"
        }
      }
      def textString = element.text() ? "${element.text()}" : '(no text)'
      sb.append "${spacing}<${element.tag()}$idString$roleString$classString$attrs$styleString>${textString}\n"
      for (child in element.children()) {
        sb << dumpElement(child, level + 1)
      }
    }
    return sb.toString()
  }

  /**
   * Determines the column index for the given column (field) name for the given domain.  Uses the
   * default fieldOrder.
   * @param domainClass The domain class.
   * @param columnName The column name.
   * @return The column Index.  -1 if not found.
   */
  int getColumnIndex(Class domainClass, String columnName) {
    def fieldOrder = DomainUtils.instance.getStaticFieldOrder(domainClass)
    if (fieldOrder) {
      return fieldOrder.findIndexOf { it == columnName }
    } else {
      return -1
    }
  }

  /**
   * Send the key(s) without wait to the browser.
   * @keys The key(s) to send.
   */
  void sendKey(Object keys) {
    interact {
      sendKeys(keys)
    }
  }

  /**
   * Returns true if the given domain has non-zero record count.
   * @param domainClass The domain class.
   * @return True if non-zero.
   */
  boolean nonZeroRecordCount(Class domainClass) {
    def count = 0
    domainClass.withTransaction {
      count = (int) domainClass.count()
    }
    return count > 0
  }

  /**
   * Wait for a record to to be created in the database.
   * @param domainClass The domain class.
   */
  void waitForNonZeroRecordCount(Class domainClass) {
    waitFor {
      // Need to use a temp variable (res) to avoid confusing waitFor with a closure inside of a closure.
      def res = false
      domainClass.withTransaction {
        def count = domainClass.count()
        res = (count > 0)
        res
      }
      res
    }
  }

  /**
   * Wait for a record to change in the database.  Checks the version until is changes.
   * @param record The record to wait for a change.
   */
  void waitForRecordChange(Object record) {
    def originalVersion = record.version
    def domainClass = record.getClass()
    waitFor {
      // Need to use a temp variable (res) to avoid confusing waitFor with a closure inside of a closure.
      def res = false
      domainClass.withTransaction {
        def record2 = domainClass.load(record.id)
        res = (record2.version != originalVersion)
        res
      }
      res
    }
  }

  /**
   * Convenience method to wait a standard delay time (approx 100ms) for the GUI to react.
   * <p>
   * <b>Note:</b> This should only be used when waitFor is not suitable (rare).
   * <p>
   * <b>Note:</b> If TRACE logging is enabled, then this method will print an abbreviated stack trace to help
   *                 you track down where it was called.  This will help you decide when a waitFor might be better than
   *                 a sleep.
   * @param multiplier The number of standard sleeps to wait for. (<b>Default:</b> 1)
   */
  void standardGUISleep(int multiplier = 1) {
    // This stack trace log can be slow (8ms), but we are waiting anyway, so a little extra won't hurt.
    log.trace("standardGUISleep: {}\n{}", multiplier * 100, LogUtils.extractImportantMethodsFromStackTrace())
    sleep(100 * multiplier)
  }

  /**
   * Finds the given input text field element on the GUI.
   * <p>
   * <b>Note:</b> This is rarely needed outside of closures.  Most Page elements use the TextFieldModule to
   *        find the input field.
   * @param fieldName The field name.
   * @return The input field element.
   */
  Navigator getInputField(String fieldName) {
    return $('div.webix_el_text', view_id: fieldName).find('input')
  }

  /**
   * Finds the combobox popup list item with the given ID.
   * @param id The ID.
   * @return The item (supports the GEB click() and text() methods).
   */
  Navigator getComboListItem(String id) {
    return $('div.webix_popup').find('div.webix_list_item', webix_l_id: id)
  }


  /**
   * Clicks a toolkit button with the given view ID.
   * @param id The ID of the button.
   */
  void clickButton(String id) {
    $('div.webix_el_button', view_id: id).find('button').click()
  }

  /**
   * Sets a combobox to the given ID setting.  Does not use the drop-down.
   * @param combobox The combobox module to set the value to.
   * @param id The ID of the combobox element to select.
   */
  void setCombobox(Object combobox, String id) {
    setCombobox((String) combobox.field, id)
  }

  /**
   * Sets a combobox to the given ID setting. Does not use the drop-down.
   * @param view The combobox view.
   * @param id The ID of the combobox element to select.
   */
  void setCombobox(String view, String id) {
    js.exec("\$\$('$view').setValue('$id')")
  }

  /**
   * Sets a multi-select combobox to the given list of IDs. Does not use the drop-down.
   * @param combobox The combobox module to set the value to.
   * @param values The list of values to set (uses the .id element).
   */
  void setMultiCombobox(Object combobox, List values) {
    setMultiCombobox((String) combobox.field, values)
  }

  /**
   * Sets a multi-select combobox to the given list of IDs. Does not use the drop-down.
   * @param view The combobox view.
   * @param values The list of values to set (uses the .id element).
   */
  void setMultiCombobox(String view, List values) {
    def valueList = values*.id.join(',')
    js.exec("\$\$('$view').\$setValue('$valueList')")
  }


  /**
   * Finds the field label (text) for the given field name.
   * <p>
   * <b>Note:</b> This is rarely needed.  Most Page elements use the TextFieldModule or ReadOnlyFieldModule to
   *        find the label.
   * @param fieldName The field name.
   * @return The field label text.
   */
  String getFieldLabel(String fieldName) {
    //        label { $('div.webix_el_label', view_id: "${field}Label").text() }
    return $('div.webix_el_label', view_id: "${fieldName}Label").text()
  }
  /**
   * Finds the field value (text) for the given field name.
   * <p>
   * <b>Note:</b> This is rarely needed.  Most Page elements use the TextFieldModule or ReadOnlyFieldModule to
   *        find the field text.
   * @param fieldName The field name.
   * @return The field text.
   */
  String getReadonlyFieldValue(String fieldName) {
    // value { $('div.webix_el_label', view_id: "${field}").text() }
    return $('div.webix_el_label', view_id: "${fieldName}").text()
  }

  /**
   * Calculates the display offsets between a given pair of GEB (HTML) elements.
   * This is the distance to move from element1 to element2.
   *
   * @param element1 The first GEB element.
   * @param element2 The second GEB element.
   * @return A Tuple with the x and y offset.
   */
  Tuple2 calculateOffset(element1, element2) {
    return [element2?.x - element1.x, element2?.y - element1.y]
  }

  /**
   * Clicks a menu (and maybe sub menu).
   * @param menuID The menu to click.
   * @param subMenuID
   */
  void clickMenu(String menuID, String subMenuID = null) {
    menu(menuID).click()
    if (subMenuID) {
      waitFor { menu(subMenuID).displayed }
      menu(subMenuID).click()
    }
  }


}