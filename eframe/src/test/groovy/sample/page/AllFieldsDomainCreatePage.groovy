package sample.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.page.AbstractCreateOrEditPage
import org.simplemes.eframe.test.page.BooleanFieldModule
import org.simplemes.eframe.test.page.ComboboxModule
import org.simplemes.eframe.test.page.DateFieldModule
import org.simplemes.eframe.test.page.TextFieldModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the All fields Domain page.
 */
class AllFieldsDomainCreatePage extends AbstractCreateOrEditPage {

  static url = '/allFieldsDomain/create'

  static at = {
    title == GlobalUtils.lookup('create.title', null, lookup('allFieldsDomain.label'), Holders.configuration.appName)
  }

  static content = {
    // Main panel fields
    name { module(new TextFieldModule(field: 'name')) }
    titleField { module(new TextFieldModule(field: 'title')) }
    qty { module(new TextFieldModule(field: 'qty')) }
    count { module(new TextFieldModule(field: 'count')) }
    enabled { module(new BooleanFieldModule(field: 'enabled')) }
    dueDate { module(new DateFieldModule(field: 'dueDate')) }
    dateTime { module(new DateFieldModule(field: 'dateTime')) }

    // Details panel fields
    notes { module(new TextFieldModule(field: 'notes')) }
    transientField(required: false) { module(new TextFieldModule(field: 'transientField')) }
    reportTimeInterval { module(new ComboboxModule(field: 'reportTimeInterval')) }
    order { module(new ComboboxModule(field: 'order')) }
    status { module(new ComboboxModule(field: 'status')) }
  }

}
