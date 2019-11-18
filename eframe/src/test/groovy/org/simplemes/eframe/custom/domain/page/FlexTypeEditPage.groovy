package org.simplemes.eframe.custom.domain.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils


/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The Flex Type's edit page.
 *
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class FlexTypeEditPage extends FlexTypeCreatePage {
  static url = '/flexType/edit'

  static at = {
    title == GlobalUtils.lookup('edit.title', null,
                                TypeUtils.toShortString(domainObject),
                                lookup('flexType.label'),
                                Holders.configuration.appName)
  }

  // Content is the same as the Create Page.
}
