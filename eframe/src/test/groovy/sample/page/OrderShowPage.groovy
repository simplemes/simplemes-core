package sample.page


import org.simplemes.eframe.test.page.AbstractShowPage
import org.simplemes.eframe.test.page.GridModule
import org.simplemes.eframe.test.page.ReadOnlyFieldModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the sample domain Order show page.
 */
@SuppressWarnings("unused")
class OrderShowPage extends AbstractShowPage {

  static url = '/order/show'

  static at = {
    checkTitle('order.label')
  }

  static content = {
    order { module(new ReadOnlyFieldModule(field: 'order')) }
    product { module(new ReadOnlyFieldModule(field: 'product')) }
    qtyToBuild { module(new ReadOnlyFieldModule(field: 'qtyToBuild')) }
    status { module(new ReadOnlyFieldModule(field: 'status')) }
    dueDate { module(new ReadOnlyFieldModule(field: 'dueDate')) }

    // Field(s) added by SampleAddition
    priority { module(new ReadOnlyFieldModule(field: 'priority')) }
    orderLines { module(new GridModule(field: 'orderLines')) }
  }

}
