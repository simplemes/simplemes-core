package org.simplemes.eframe.security.page

import org.simplemes.eframe.test.page.AbstractPage
import org.simplemes.eframe.test.page.DefinitionListModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the user List (index) page.
 */
class UserListPage extends AbstractPage {

  static url = '/user'

  static at = { title.contains 'User List' }

  static content = {
    userGrid { module(new DefinitionListModule(field: 'user')) }
  }

  /**
   * If true, then the page will wait on load until the Ajax queries are completed.
   * Override in your sub-class if you have Ajax loading mechanism.
   */
  @Override
  boolean getWaitForAjaxOnLoad() {
    return true
  }
}
