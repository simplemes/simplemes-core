package sample.page

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
 * The page definition for the All fields Domain Create page.
 */
class SampleParentCreatePage extends AbstractCreateOrEditPage {

  static url = '/sampleParent/create'

  static at = {
    title == GlobalUtils.lookup('create.title', null, lookup('sampleParent.label'), Holders.configuration.appName)
  }

  static content = {
    name { module(new TextFieldModule(field: 'name')) }
    titleField { module(new TextFieldModule(field: 'title')) }
    notes { module(new TextFieldModule(field: 'notes')) }
    moreNotes { module(new TextFieldModule(field: 'moreNotes')) }
    sampleChildren { module(new GridModule(field: 'sampleChildren')) }
    allFieldsDomain { module(new ComboboxModule(field: 'allFieldsDomain')) }
    allFieldsDomains { module(new ComboboxModule(field: 'allFieldsDomains')) }
  }

}
