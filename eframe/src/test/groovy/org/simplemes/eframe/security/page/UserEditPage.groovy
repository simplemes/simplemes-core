package org.simplemes.eframe.security.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the User page.
 */
class UserEditPage extends UserCreatePage {

  static url = '/user/edit'

  static at = {
    title == GlobalUtils.lookup('edit.title', null,
                                TypeUtils.toShortString(domainObject),
                                lookup('user.label'),
                                Holders.configuration.appName)
  }

  // Content is the same as the Create Page.
}
