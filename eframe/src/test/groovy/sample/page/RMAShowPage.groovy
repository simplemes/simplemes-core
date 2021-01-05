package sample.page


import org.simplemes.eframe.test.page.AbstractShowPage
import org.simplemes.eframe.test.page.ReadOnlyFieldModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the sample domain RMA show page.
 */
@SuppressWarnings("unused")
class RMAShowPage extends AbstractShowPage {

  static url = '/rma/show'

  static at = {
    checkTitle('rma.label')
  }

  static content = {
    rma { module(new ReadOnlyFieldModule(field: 'rma')) }
    status { module(new ReadOnlyFieldModule(field: 'status')) }
    product { module(new ReadOnlyFieldModule(field: 'product')) }
    qty { module(new ReadOnlyFieldModule(field: 'qty')) }
    rmaType { module(new ReadOnlyFieldModule(field: 'rmaType')) }
    returnDate { module(new ReadOnlyFieldModule(field: 'returnDate')) }

    // Field(s) added by Flex Type
    FIELD1 { module(new ReadOnlyFieldModule(field: 'FIELD1')) }
    FIELD2 { module(new ReadOnlyFieldModule(field: 'FIELD2')) }
    FIELD3 { module(new ReadOnlyFieldModule(field: 'FIELD3')) }
  }

}
