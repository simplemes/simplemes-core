/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.assy.product.page

import org.simplemes.eframe.test.page.AbstractShowPage
import org.simplemes.eframe.test.page.ReadOnlyFieldModule

/**
 * The page definition for the User show page.
 */
@SuppressWarnings("unused")
class ProductShowPage extends AbstractShowPage {

  static url = '/product/show'

  static at = {
    checkTitle('product.label')
  }

  static content = {
    product { module(new ReadOnlyFieldModule(field: 'product')) }
  }

}
