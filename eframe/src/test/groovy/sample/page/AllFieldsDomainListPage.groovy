/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.page

import org.simplemes.eframe.test.page.AbstractPage
import org.simplemes.eframe.test.page.ButtonModule
import org.simplemes.eframe.test.page.DefinitionListModule
import org.simplemes.eframe.test.page.TextFieldModule

/**
 * The page definition for the All fields Domain List (index) page.
 */
@SuppressWarnings('unused')
class AllFieldsDomainListPage extends AbstractPage {

  static url = '/allFieldsDomain'

  static at = { title.contains(lookup('allFieldsDomain.label')) && title.contains(lookup('list.label')) }

  static content = {
    allFieldsDomainGrid { module(new DefinitionListModule(field: 'allFieldsDomain')) }
    searchField { module(new TextFieldModule(field: 'allFieldsDomainDefinitionListSearch')) }
    createButton { module(new ButtonModule(id: 'allFieldsDomainDefinitionListCreate')) }
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
