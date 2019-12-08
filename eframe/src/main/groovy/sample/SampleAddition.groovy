package sample

import groovy.transform.ToString
import org.simplemes.eframe.custom.Addition
import org.simplemes.eframe.custom.AdditionConfiguration
import org.simplemes.eframe.custom.AdditionInterface
import org.simplemes.eframe.custom.BaseAddition
import org.simplemes.eframe.data.format.CustomChildListFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import sample.domain.Order
import sample.domain.OrderLine
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines an internal addition for the framework that is used to specify some features
 * for the framework (e.g. BasicStatus codes, etc).
 * <p>
 * <b>Note:</b> This is used in E2E tests.  
 */
@ToString(includeNames = true, includePackage = false)
class SampleAddition extends BaseAddition implements AdditionInterface {

  /**
   * Defines the elements needed/provided by this addition.
   */
  AdditionConfiguration addition = Addition.configure {
    domainPackage SampleParent

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
      name 'orderLines'
      label 'Line Items'
      format CustomChildListFieldFormat
      valueClass OrderLine
      fieldOrder {
        name 'orderLines'
        after 'dueDate'
      }
      guiHints 'sequence@default="tk.findMaxGridValue(gridName, \'sequence\')+10"' +
                 'product@default="\'P\'+tk.findMaxGridValue(gridName, \'sequence\')+10"'
    }
  }

}
