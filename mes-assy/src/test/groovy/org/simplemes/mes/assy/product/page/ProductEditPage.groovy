package org.simplemes.mes.assy.product.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the Product page.
 */
class ProductEditPage extends ProductCreatePage {

  static url = '/product/edit'

  static at = {
    title == GlobalUtils.lookup('edit.title', null,
                                TypeUtils.toShortString(domainObject),
                                lookup('product.label'),
                                Holders.configuration.appName)
  }

  // Content is the same as the Create Page.
}
