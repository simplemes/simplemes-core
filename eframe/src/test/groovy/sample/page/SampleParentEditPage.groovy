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
 * The page definition for the All fields Domain Create page.
 */
class SampleParentEditPage extends SampleParentCreatePage {

  static url = '/sampleParent/edit'

  static at = {
    title == GlobalUtils.lookup('edit.title', null,
                                TypeUtils.toShortString(domainObject),
                                lookup('sampleParent.label'),
                                Holders.configuration.appName)
  }

  // Content is the same as the create page.
}
