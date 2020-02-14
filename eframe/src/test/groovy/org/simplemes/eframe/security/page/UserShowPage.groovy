/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.page

import org.simplemes.eframe.test.page.AbstractShowPage
import org.simplemes.eframe.test.page.BooleanFieldModule
import org.simplemes.eframe.test.page.ReadOnlyFieldModule

/**
 * The page definition for the User show page.
 */
@SuppressWarnings("unused")
class UserShowPage extends AbstractShowPage {

  static url = '/user/show'

  static at = {
    checkTitle('user.label')
  }

  static content = {
    userName { module(new ReadOnlyFieldModule(field: 'userName')) }
    titleField { module(new ReadOnlyFieldModule(field: 'title')) }
    enabled { module(new BooleanFieldModule(field: 'enabled')) }
    accountExpired { module(new BooleanFieldModule(field: 'accountExpired')) }
    accountLocked { module(new BooleanFieldModule(field: 'accountLocked')) }
    passwordExpired { module(new BooleanFieldModule(field: 'passwordExpired')) }
    email { module(new ReadOnlyFieldModule(field: 'email')) }
    userRoles { module(new ReadOnlyFieldModule(field: 'userRoles')) }
  }

}
