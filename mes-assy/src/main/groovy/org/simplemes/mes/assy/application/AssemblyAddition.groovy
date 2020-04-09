package org.simplemes.mes.assy.application

import groovy.transform.ToString
import org.simplemes.eframe.custom.Addition
import org.simplemes.eframe.custom.AdditionConfiguration
import org.simplemes.eframe.custom.AdditionInterface
import org.simplemes.eframe.custom.BaseAddition
import org.simplemes.eframe.data.format.CustomChildListFieldFormat
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.product.domain.Product

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The assembly module definitions for run-time additions to the application. Provides built-in types
 * and related details. <p>
 * This addition provides the components and assembly-related features to the MES Core module.
 */
@Singleton
@SuppressWarnings("unused")
@ToString(includeNames = true, includePackage = false)
class AssemblyAddition extends BaseAddition implements AdditionInterface {

  /**
   * Defines the elements needed/provided by this addition.
   */
  AdditionConfiguration addition = Addition.configure {
    field {
      domain Product
      name 'components'
      label 'components.label'
      format CustomChildListFieldFormat
      valueClass ProductComponent
      fieldOrder { name 'group:components' }
      fieldOrder { name 'components'; after 'group:components' }
      guiHints 'sequence@default="tk.findMaxGridValue(gridName, \'sequence\')+10"'
    }


/*
    initialDataLoader InitialData
    encodedType LSNStatus
    encodedType OrderStatus
    encodedType WorkCenterStatus
    asset {
      page "dashboard/index"
      script "/assets/mes_dashboard.js"
    }
    asset {
      page "dashboard/index"
      css "/assets/mes_dashboard.css"
    }
*/
  }

}
