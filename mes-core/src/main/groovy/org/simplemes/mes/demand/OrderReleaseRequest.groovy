package org.simplemes.mes.demand

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.json.JSONByKey
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The request argument for the OrderService.release() service method.  This provides the details
 * for the release of the order.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.demand.service.OrderService#release(org.simplemes.mes.demand.OrderReleaseRequest)} method.
 */
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
class OrderReleaseRequest {

  /**
   * Convenience constructor for a simple order release.
   * @param order The order.
   */
  OrderReleaseRequest(Order order) {
    this.order = order
  }

  /**
   * Map constructor.  Provides the standard Groovy map constructor.  Needed since we have a convenience constructor above.
   * @param options The options map
   */
  OrderReleaseRequest(Map options) {
    options?.each { k, v -> this[k as String] = v }
  }

  /**
   * Empty constructor.  Provides the standard Groovy map constructor.  Needed since we have a convenience constructor above.
   */
  OrderReleaseRequest() {
  }

  /**
   * The order to release (<b>Required</b>).
   */
  @JSONByKey
  Order order

  /**
   * The number of pieces to release (<b>Default:</b> All remaining quantity).
   */
  BigDecimal qty

  /**
   * The date/time this release should be logged as.
   */
  Date dateTime

}
