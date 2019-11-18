package org.simplemes.eframe.test.page

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the standard login GEB page elements for testing.
 * <p/>
 * This page defines these content sections:
 * <ul>
 *   <li><b>username</b> - The username input field.</li>
 *   <li><b>password</b> - The password input field.</li>
 *   <li><b>loginButton</b> - The login button.  Supports <code>click()</code>.</li>
 *   <li><b>message</b> - The message text for any login error messages.  </li>
 * </ul>
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class LoginPage extends AbstractPage {

  static url = "/login/auth"
  static at = { title == "Login" }
  static content = {
    userName { $('input', id: 'username') }
    password { $('input', id: 'password') }
    loginButton { $('input#submit') }
    message { $('div.login_message').text() }
    //searchButton(to: GoogleResultsPage) { $("input[value='Google Search']") }
  }

}
