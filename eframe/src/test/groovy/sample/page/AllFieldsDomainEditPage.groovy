package sample.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the All fields Domain page.
 */
class AllFieldsDomainEditPage extends AllFieldsDomainCreatePage {

  static url = '/allFieldsDomain/edit'

  static at = {
    title == GlobalUtils.lookup('edit.title', null,
                                TypeUtils.toShortString(domainObject),
                                lookup('allFieldsDomain.label'),
                                Holders.configuration.appName)
  }

  // Content is the same as the Create Page.
}
