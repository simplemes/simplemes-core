package sample.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.page.AbstractShowPage
import org.simplemes.eframe.test.page.ReadOnlyFieldModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the All fields Domain List (index) page.
 */
class AllFieldsDomainShowPage extends AbstractShowPage {

  static url = '/allFieldsDomain/show'

  static at = {
    if (domainObject) {
      title == GlobalUtils.lookup('show.title', null,
                                  TypeUtils.toShortString(domainObject),
                                  lookup('allFieldsDomain.label'),
                                  Holders.configuration.appName)
    } else {
      title.contains(lookup('allFieldsDomain.label'))
    }
  }

  static content = {
    // Main panel fields
    name { module(new ReadOnlyFieldModule(field: 'name')) }
    titleField { module(new ReadOnlyFieldModule(field: 'title')) }
    qty { module(new ReadOnlyFieldModule(field: 'qty')) }
    count { module(new ReadOnlyFieldModule(field: 'count')) }
    enabled { module(new ReadOnlyFieldModule(field: 'enabled')) }
    dueDate { module(new ReadOnlyFieldModule(field: 'dueDate')) }
    dateTime { module(new ReadOnlyFieldModule(field: 'dateTime')) }

    // Details panel fields
    notes { module(new ReadOnlyFieldModule(field: 'notes')) }
    transientField(required: false) { module(new ReadOnlyFieldModule(field: 'transientField')) }
  }

}
