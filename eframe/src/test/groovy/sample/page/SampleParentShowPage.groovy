package sample.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.page.AbstractShowPage
import org.simplemes.eframe.test.page.GridModule
import org.simplemes.eframe.test.page.ReadOnlyFieldModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the All fields Domain List (index) page.
 */
@SuppressWarnings("unused")
class SampleParentShowPage extends AbstractShowPage {

  static url = '/sampleParent/show'

  static at = {
    if (domainObject) {
      title == GlobalUtils.lookup('show.title', null,
                                  TypeUtils.toShortString(domainObject),
                                  lookup('sampleParent.label'),
                                  Holders.configuration.appName)
    } else {
      title.contains(lookup('sampleParent.label'))
    }
  }

  static content = {
    name { module(new ReadOnlyFieldModule(field: 'name')) }
    titleField { module(new ReadOnlyFieldModule(field: 'title')) }
    notes { module(new ReadOnlyFieldModule(field: 'notes')) }
    sampleChildren { module(new GridModule(field: 'sampleChildren')) }
  }

  /**
   * If true, then the page will wait on load until the Ajax queries are completed.
   * Override in your sub-class if you have Ajax loading mechanism.
   */
  @Override
  boolean getWaitForAjaxOnLoad() {
    return false
  }
}
