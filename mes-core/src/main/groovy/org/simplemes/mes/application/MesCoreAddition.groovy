package org.simplemes.mes.application

import groovy.transform.ToString
import org.simplemes.eframe.custom.Addition
import org.simplemes.eframe.custom.AdditionConfiguration
import org.simplemes.eframe.custom.AdditionInterface
import org.simplemes.eframe.custom.BaseAddition
import org.simplemes.mes.MESCorePackage
import org.simplemes.mes.demand.LSNStatus
import org.simplemes.mes.demand.OrderStatus
import org.simplemes.mes.floor.WorkCenterStatus
import org.simplemes.mes.system.InitialData

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The module definition for run-time additions to the application. Provides built-in types
 * and related details.
 */
@SuppressWarnings("unused")
@ToString(includeNames = true, includePackage = false)
class MesCoreAddition extends BaseAddition implements AdditionInterface {

  /**
   * Defines the elements needed/provided by this addition.
   */
  AdditionConfiguration addition = Addition.configure {
    domainPackage MESCorePackage
  }

  /**
   * Returns the list of encoded field types that Hibernate will support.
   * @return The list of classes.
   */
  @Override
  List<Class> getEncodedTypes() {
    return [LSNStatus, OrderStatus, WorkCenterStatus]
  }

  /**
   * Returns a list of classes that define non-domain classes that will perform initial data loading.
   * These classes need a static initialDataLoad() method.
   * @return The list of other classes
   */
  @Override
  List<Class> getInitialDataLoaders() {
    return [InitialData]
  }
}
