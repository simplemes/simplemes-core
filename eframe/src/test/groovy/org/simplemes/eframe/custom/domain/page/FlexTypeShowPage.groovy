package org.simplemes.eframe.custom.domain.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.page.AbstractShowPage
import org.simplemes.eframe.test.page.BooleanFieldModule
import org.simplemes.eframe.test.page.GridModule
import org.simplemes.eframe.test.page.ReadOnlyFieldModule

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The Flex Type's show page.
 *
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class FlexTypeShowPage extends AbstractShowPage {

  static url = '/order/show'

  static at = {
    if (domainObject) {
      title == GlobalUtils.lookup('show.title', null,
                                  TypeUtils.toShortString(domainObject),
                                  lookup('flexType.label'),
                                  Holders.configuration.appName)
    } else {
      title.contains(lookup('flexType.label'))
    }
  }

  static content = {
    flexType { module(new ReadOnlyFieldModule(field: 'flexType')) }
    category { module(new ReadOnlyFieldModule(field: 'category')) }
    titleField { module(new ReadOnlyFieldModule(field: 'title')) }
    defaultFlexType { module(new BooleanFieldModule(field: 'defaultFlexType')) }

    // Field list with inlineGrid version
    fields { module(new GridModule(field: 'fields')) }
  }

}

