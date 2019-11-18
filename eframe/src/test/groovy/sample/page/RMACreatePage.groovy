package sample.page


import org.simplemes.eframe.test.page.AbstractCreateOrEditPage
import org.simplemes.eframe.test.page.ComboboxModule
import org.simplemes.eframe.test.page.DateFieldModule
import org.simplemes.eframe.test.page.TextFieldModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the RMA Create page.
 */
@SuppressWarnings("unused")
class RMACreatePage extends AbstractCreateOrEditPage {

  static url = '/rma/create'

  static at = {
    checkCreateTitle('rma.label')
  }

  static content = {
    rma { module(new TextFieldModule(field: 'rma')) }
    status { module(new TextFieldModule(field: 'status')) }
    product { module(new TextFieldModule(field: 'product')) }
    qty { module(new TextFieldModule(field: 'qty')) }
    rmaType { module(new ComboboxModule(field: 'rmaType')) }
    returnDate { module(new DateFieldModule(field: 'returnDate')) }

    // Field(s) added by Flex Type
    FIELD1 { module(new TextFieldModule(field: 'rmaType_FIELD1')) }
    FIELD2 { module(new TextFieldModule(field: 'rmaType_FIELD2')) }
    FIELD3 { module(new TextFieldModule(field: 'rmaType_FIELD3')) }
  }

}
