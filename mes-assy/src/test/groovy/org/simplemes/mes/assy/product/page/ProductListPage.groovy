package org.simplemes.mes.assy.product.page

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
class ProductListPage extends AbstractPage {

  static url = '/product'

  static at = { title.contains 'Product List' }

  static content = {
    productGrid { module(new DefinitionListModule(field: 'product')) }
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
