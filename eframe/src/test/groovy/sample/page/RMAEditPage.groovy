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
class RMAEditPage extends RMACreatePage {

  static url = '/rma/edit'

  static at = {
    checkEditTitle('rma.label')
  }

  // Content is the same as the create page.
}
