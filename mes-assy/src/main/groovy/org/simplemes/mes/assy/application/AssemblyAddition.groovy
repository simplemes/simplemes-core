package org.simplemes.mes.assy.application

import groovy.transform.ToString
import org.simplemes.eframe.custom.Addition
import org.simplemes.eframe.custom.AdditionConfiguration
import org.simplemes.eframe.custom.AdditionInterface
import org.simplemes.eframe.custom.BaseAddition

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
