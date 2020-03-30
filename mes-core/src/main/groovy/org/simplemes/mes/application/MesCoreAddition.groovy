package org.simplemes.mes.application

import groovy.transform.ToString
import org.simplemes.eframe.custom.Addition
import org.simplemes.eframe.custom.AdditionConfiguration
import org.simplemes.eframe.custom.AdditionInterface
import org.simplemes.eframe.custom.BaseAddition
import org.simplemes.mes.demand.LSNStatus
import org.simplemes.mes.demand.OrderStatus
import org.simplemes.mes.floor.WorkCenterStatus
import org.simplemes.mes.system.InitialData

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The module definition for run-time additions to the application. Provides built-in types
 * and related details.
 */
@Singleton
@SuppressWarnings("unused")
@ToString(includeNames = true, includePackage = false)
class MesCoreAddition extends BaseAddition implements AdditionInterface {

  /**
   * Defines the elements needed/provided by this addition.
   */
  AdditionConfiguration addition = Addition.configure {
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
  }

}
