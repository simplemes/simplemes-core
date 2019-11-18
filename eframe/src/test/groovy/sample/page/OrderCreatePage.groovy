package sample.page


import org.simplemes.eframe.test.page.AbstractCreateOrEditPage
import org.simplemes.eframe.test.page.DateFieldModule
import org.simplemes.eframe.test.page.GridModule
import org.simplemes.eframe.test.page.TextFieldModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for the Domain Create page.
 */
@SuppressWarnings("unused")
class OrderCreatePage extends AbstractCreateOrEditPage {

  static url = '/order/create'

  static at = {
    checkCreateTitle('order.label')
  }

  static content = {
    order { module(new TextFieldModule(field: 'order')) }
    product { module(new TextFieldModule(field: 'product')) }
    qtyToBuild { module(new TextFieldModule(field: 'qtyToBuild')) }
    status { module(new TextFieldModule(field: 'status')) }
    dueDate { module(new DateFieldModule(field: 'dueDate')) }

    // Field(s) added by SampleAddition 
    priority { module(new TextFieldModule(field: 'priority')) }
    orderLines { module(new GridModule(field: 'orderLines')) }
  }

}
