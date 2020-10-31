/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import geb.navigator.Navigator
import groovy.util.logging.Slf4j
import org.apache.http.HttpHost
import org.elasticsearch.client.Request
import org.elasticsearch.client.RestClient
import org.openqa.selenium.Point
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.test.page.HomePage
import org.simplemes.eframe.test.page.LoginPage
import spock.lang.IgnoreIf
import spock.lang.Shared

/**
 * This is the common Spock specification base class for GUI/GEB tests.
 * This includes login/out and debug functions for GEB elements.
 */
@Slf4j
class BaseGUISpecification extends BaseSpecification {

  /**
   * Makes sure the GUI test has GEB GUI, embedded server and database.
   */
  @SuppressWarnings("unused")
  static specNeeds = [GUI]

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
    // Now, shift the window horizontally if desired.
    def w = driver.manage().window()
    if (w.position.x > -20) {
      w.position = new Point(w.position.x - 1500, 10)
      w.maximize()
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
      sendKeys(keys as String)
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
   * Waits for the combobox in a grid to display the input value.  Assumes the focus is in the cell with
   * the combobox.
   * @param value The value in the current combobox editor field.
   */
  void waitForGridComboboxInputValue(String value) {
    waitFor {
      $('div.webix_dt_editor').find('input').value().contains(value)
    }
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
    def valueList = values*.uuid.join(',')
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
   * Checks and sets the given field value.  Supports text field and checkbox fields.
   * @param fieldName The field name.
   * @param expectedValue The expected value.
   * @param newValue The new value.
   */
  void setFieldValue(String fieldName, Object expectedValue, Object newValue) {
    if (newValue instanceof Integer) {
      newValue = newValue.toString()
      expectedValue = expectedValue.toString()
    }
    if (newValue instanceof Boolean) {
      def field = $('div.webix_el_checkbox', view_id: "${fieldName}").find('button')
      def currentValue = field.@'aria-checked' == 'true'
      assert currentValue == expectedValue, "Field Value for '$fieldName' is not correct. Found $currentValue, expected $expectedValue"
      if (currentValue != newValue) {
        // Needs to be changed, so just click it to toggle the value.
        field.click()
      }
    } else {
      // Default to text field.
      def field = $('div.webix_el_text', view_id: "${fieldName}").find('input')
      assert field.value() == expectedValue, "Field Value for '$fieldName' is not correct. Found ${field.value()}, expected $expectedValue"
      field.value(newValue)
    }
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

  /**
   * Check a menu from a toolbar-style menu with some sub menus.
   * @param menuName The top-level menu.
   * @param menuLabel The top-level menu lookup key.
   * @param subMenus The list of menus to check (element 0 = menu ID, element 1 = lookup key for label.
   *
   * @return Always returns true.
   */
  boolean checkMenuLabels(String menuName, String menuLabel, List<List<String>> subMenus) {
    assert page.menu(menuName).text() == lookup(menuLabel)
    page.menu(menuName).click()
    def waitForMenu = subMenus[0][0].replaceAll('\\.', '_')
    //println "menu = ${page.menu(menuName).text()} ${waitForMenu}"
    waitFor { page.menu(waitForMenu).displayed }
    //sleep(1000)

    for (sub in subMenus) {
      //println "sub $sub menu = ${page.menu(sub[0]).text()}"
      assert page.menu(sub[0].replaceAll('\\.', '_')).text() == lookup(sub[1])
    }

    return true
  }

  /**
   * Returns true if the GUI test is running in firefox.
   * <p>
   *  <b>Note:</b> Use this sparingly.
   * @return True if running in firefox.
   */
  boolean isFireFox() {
    return System.getProperty('geb.env')?.contains('firefox')
  }

  /**
   * Convenience method for general message.properties lookup.
   * This sub-class uses the current locale from the GUI test browser if none is given.
   * <p>
   * <b>Note:</b> This lookup will flag missing .properties entries.
   * @param key The key to lookup.
   * @param locale The locale to use for the message. (<b>Default</b>: Current Browser Locale)
   * @param args The replaceable arguments used by the message (if any).
   * @return The looked up message.
   */
  @Override
  String lookup(String key, Locale locale = null, Object... args) {
    locale = locale ?: currentLocale
    return super.lookup(key, locale, args)
  }

  /**
   * Convenience method for general message.properties lookup for fields marked as required.
   * This sub-class uses the current locale from the GUI test browser if none is given.
   * <p>
   * <b>Note:</b> This lookup will flag missing .properties entries.
   * @param key The key to lookup.
   * @param locale The locale to use for the message. (<b>Default</b>: Current browser Locale)
   * @param args The replaceable arguments used by the message (if any).
   * @return The looked up message.
   */
  @Override
  String lookupRequired(String key, Locale locale = null, Object... args) {
    locale = locale ?: currentLocale
    return super.lookupRequired(key, locale, args)
  }

  /**
   * See if the search server is up.
   */
  @Shared
  static Boolean searchServerUp = null

  static boolean isSearchServerUp() {
    if (searchServerUp != null) {
      return searchServerUp
    }
    try {
      // Try to open a connection to the localhost:9200
      def restClient = RestClient.builder([new HttpHost('localhost', 9200)] as HttpHost[]).build()
      restClient.performRequest(new Request("GET", "/_cluster/health"))
      // No exception, so assume the server is live.
      searchServerUp = true
      return true
    } catch (Exception ignored) {
    }
    searchServerUp = false
    return false
  }

}
