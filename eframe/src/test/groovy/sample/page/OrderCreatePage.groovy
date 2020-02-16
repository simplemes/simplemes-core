/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.page


import org.simplemes.eframe.test.page.AbstractCreateOrEditPage
import org.simplemes.eframe.test.page.DateFieldModule
import org.simplemes.eframe.test.page.GridModule
import org.simplemes.eframe.test.page.TextFieldModule

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
    orderLines { module(new GridModule(field: 'orderLines')) }

    // Field(s) added by SampleAddition 
    priority { module(new TextFieldModule(field: 'priority')) }
    customComponents { module(new GridModule(field: 'customComponents')) }
  }

}
