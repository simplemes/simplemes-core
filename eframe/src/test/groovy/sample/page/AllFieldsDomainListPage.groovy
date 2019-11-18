package sample.page

import org.simplemes.eframe.test.page.AbstractPage
import org.simplemes.eframe.test.page.DefinitionListModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the All fields Domain List (index) page.
 */
class AllFieldsDomainListPage extends AbstractPage {

  static url = '/allFieldsDomain'

  static at = { title.contains(lookup('allFieldsDomain.label')) && title.contains(lookup('list.label')) }

  static content = {
    allFieldsDomainGrid { module(new DefinitionListModule(field: 'allFieldsDomain')) }
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
