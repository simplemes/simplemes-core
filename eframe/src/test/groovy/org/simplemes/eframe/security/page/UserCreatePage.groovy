package org.simplemes.eframe.security.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.page.AbstractCreateOrEditPage
import org.simplemes.eframe.test.page.BooleanFieldModule
import org.simplemes.eframe.test.page.TextFieldModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the User page.
 */
@SuppressWarnings("unused")
class UserCreatePage extends AbstractCreateOrEditPage {

  static url = '/user/create'

  static at = {
    title == GlobalUtils.lookup('create.title', null, lookup('user.label'), Holders.configuration.appName)
  }

  static content = {
    userName { module(new TextFieldModule(field: 'userName')) }
    titleField { module(new TextFieldModule(field: 'title')) }
    enabled { module(new BooleanFieldModule(field: 'enabled')) }
    accountExpired { module(new BooleanFieldModule(field: 'accountExpired')) }
    accountLocked { module(new BooleanFieldModule(field: 'accountLocked')) }
    passwordExpired { module(new BooleanFieldModule(field: 'passwordExpired')) }
    _pwNew { module(new TextFieldModule(field: '_pwNew')) }
    _pwConfirm { module(new TextFieldModule(field: '_pwConfirm')) }
    email { module(new TextFieldModule(field: 'email')) }
  }

}
