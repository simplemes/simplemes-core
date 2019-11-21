package org.simplemes.mes.demand

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The response from the OrderService.release() service method.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.demand.service.OrderService#release(org.simplemes.mes.demand.OrderReleaseRequest)} method.
 */
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
class OrderReleaseResponse {

  /**
   * The order released.
   */
  Order order

  /**
   * The number of pieces released.
   */
  BigDecimal qtyReleased

}
