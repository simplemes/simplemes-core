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
class SampleParentListPage extends AbstractPage {

  static url = '/sampleParent'

  static at = { title.contains(lookup('sampleParent.label')) && title.contains(lookup('list.label')) }

  static content = {
    sampleParentList { module(new DefinitionListModule(field: 'parent', suffix: 'Grid')) }
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
