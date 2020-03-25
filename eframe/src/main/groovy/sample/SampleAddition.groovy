/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample

import groovy.transform.ToString
import org.simplemes.eframe.custom.Addition
import org.simplemes.eframe.custom.AdditionConfiguration
import org.simplemes.eframe.custom.AdditionInterface
import org.simplemes.eframe.custom.BaseAddition
import org.simplemes.eframe.data.format.CustomChildListFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import sample.domain.CustomOrderComponent
import sample.domain.Order

import javax.inject.Singleton

/**
 * Defines an internal addition for the framework that is used to specify some features
 * for the framework (e.g. BasicStatus codes, etc).
 * <p>
 * <b>Note:</b> This is used in E2E tests.  
 */
@Singleton
@ToString(includeNames = true, includePackage = false)
class SampleAddition extends BaseAddition implements AdditionInterface {

  /**
   * Defines the elements needed/provided by this addition.
   */
  AdditionConfiguration addition = Addition.configure {

    // Sample addition field for the sample Order
    field {
      domain Order
      name 'priority'
      label 'Delivery Priority'
      format IntegerFieldFormat
      fieldOrder {
        name 'priority'
        after 'status'
      }
      guiHints 'required="true"'
    }

    // Sample custom child list for the sample Order
    field {
      domain Order
      name 'customComponents'
      label 'Components'
      format CustomChildListFieldFormat
      valueClass CustomOrderComponent
      fieldOrder {
        name 'customComponents'
        after 'dueDate'
      }
      guiHints 'sequence@default="tk.findMaxGridValue(gridName, \'sequence\')+10"' +
                 'product@default="\'P\'+tk.findMaxGridValue(gridName, \'sequence\')+10"'
    }

    // Sample assets added to all pages
    asset {
      page "home/index"
      script "/assets/sample.js"
    }
    asset {
      page "home/index"
      css "/assets/sample.css"
    }
  }

}
