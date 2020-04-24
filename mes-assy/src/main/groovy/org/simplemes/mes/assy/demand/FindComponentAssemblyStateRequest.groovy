package org.simplemes.mes.assy.demand

import groovy.transform.ToString
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
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

  /**
   * Main constructor for use with a controller's HTTP parameters.  Validates the inputs, but does not fail if no order/lsn is
   * given.
   * @param params The HTTP parameters (order, lsn and hideAssembled)
   */
  FindComponentAssemblyStateRequest(Map params) {
    if (params.order) {
      demand = Order.findByOrder(params.order as String)
      if (!demand) {
        //error.110.message=Could not find {0} {1}
        throw new BusinessException(110, [GlobalUtils.lookup('order.label'), params.order])
      }
      if (params.lsn) {
        // If both given, then find the LSN in the order
        def order = demand
        demand = (DemandObject) order.lsns.find { it.lsn == params.lsn }
        if (!demand) {
          //error.3009.message=LSN {0} is not part of Order {1}
          throw new BusinessException(3009, [params.order, params.lsn])
        }
      }
    } else if (params.lsn) {
      def list = LSN.findAllByLsn((String) params.lsn)
      if (list.size() == 1) {
        demand = list[0]
      } else if (list.size() > 1) {
        //error.3011.message=More than one LSN matches "{0}". {1} LSNs exist with the same ID.
        throw new BusinessException(3011, [params.lsn, list.size()])
      } else {
        //error.110.message=Could not find {0} {1}
        throw new BusinessException(110, [GlobalUtils.lookup('lsn.label'), params.lsn])
      }
    }

    hideAssembled = ArgumentUtils.convertToBoolean(params.hideAssembled)

  }

}
