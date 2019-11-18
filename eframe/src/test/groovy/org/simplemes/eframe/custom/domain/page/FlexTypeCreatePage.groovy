package org.simplemes.eframe.custom.domain.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.page.AbstractCreateOrEditPage
import org.simplemes.eframe.test.page.BooleanFieldModule
import org.simplemes.eframe.test.page.GridModule
import org.simplemes.eframe.test.page.TextFieldModule

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The Flex Type's create page.
 *
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class FlexTypeCreatePage extends AbstractCreateOrEditPage {

  static url = '/flexType/create'

  static at = {
    title == GlobalUtils.lookup('create.title', null, lookup('flexType.label'), Holders.configuration.appName)
  }

  static content = {
    flexType { module(new TextFieldModule(field: 'flexType')) }
    category { module(new TextFieldModule(field: 'category')) }
    titleField { module(new TextFieldModule(field: 'title')) }
    defaultFlexType { module(new BooleanFieldModule(field: 'defaultFlexType')) }

    // Field list with inlineGrid version
    fields { module(new GridModule(field: 'fields')) }
  }

}
