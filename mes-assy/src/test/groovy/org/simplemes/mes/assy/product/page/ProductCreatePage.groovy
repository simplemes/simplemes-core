package org.simplemes.mes.assy.product.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.page.AbstractCreateOrEditPage
import org.simplemes.eframe.test.page.ComboboxModule
import org.simplemes.eframe.test.page.GridModule
import org.simplemes.eframe.test.page.TextFieldModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the Product page.
 */
@SuppressWarnings("unused")
class ProductCreatePage extends AbstractCreateOrEditPage {

  static url = '/product/create'

  static at = {
    title == GlobalUtils.lookup('create.title', null, lookup('product.label'), Holders.configuration.appName)
  }

  static content = {
    product { module(new TextFieldModule(field: 'product')) }
    assemblyDataType { module(new ComboboxModule(field: 'assemblyDataType')) }

    componentsPanel { $('div.webix_item_tab', button_id: "componentsBody") }
    components { module(new GridModule(field: 'components')) }

  }

}
