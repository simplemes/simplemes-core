package sample.page
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the Edit page.
 */
@SuppressWarnings("unused")
class OrderEditPage extends OrderCreatePage {

  static url = '/order/edit'

  static at = {
    checkEditTitle('order.label')
  }

  // Content is the same as the create page.
}
