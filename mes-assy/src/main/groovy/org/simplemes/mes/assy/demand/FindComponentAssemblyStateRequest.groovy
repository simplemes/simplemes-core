package org.simplemes.mes.assy.demand

import groovy.transform.ToString
import org.simplemes.mes.demand.DemandObject
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2016. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This class is the request to find the component state of an order/LSN.
 */
@ToString(includeNames = true, includePackage = false)
class FindComponentAssemblyStateRequest {

  /**
   * The order/LSN to find the components for.  (<b>Required</b>).
   */
  DemandObject demand

  /**
   * If true, then all components that are fully assembled or over assembled will be filtered out (<b>default</b>: false).
   */
  Boolean hideAssembled = false

  /**
   * Default constructor.
   */
  FindComponentAssemblyStateRequest() {
  }

  /**
   * Convenience constructor.
   * @param order The demand object.
   */
  FindComponentAssemblyStateRequest(Order order) {
    this.demand = order
  }

  /**
   * Convenience constructor.
   * @param lsn The demand object.
   */
  FindComponentAssemblyStateRequest(LSN lsn) {
    this.demand = lsn
  }
}
