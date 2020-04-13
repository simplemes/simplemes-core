package org.simplemes.mes.demand.service

import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.OrderReleaseResponse

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the API for the Order release extensions supported.
 */
@SuppressWarnings("unused")
interface OrderReleasePoint {
  /**
   * Pre-release extension method for the order release process.
   * See the core method {@link OrderService#release(org.simplemes.mes.demand.OrderReleaseRequest)} for details.
   * <p>
   * <b>Note</b>: This method is part of the Stable API.
   * @param orderReleaseRequest The details of the release request (order and qty).
   */
  void preRelease(OrderReleaseRequest orderReleaseRequest)

  /**
   * Post-release extension method for the order release process.
   * See the core method {@link OrderService#release(org.simplemes.mes.demand.OrderReleaseRequest)} for details.
   * <p>
   * <b>Note</b>: This method is part of the Stable API.
   * @param coreResponse The response from the core method (or from other extensions that provide a response).
   * @param orderReleaseRequest The details of the release request (order and qty).
   * @return Return a new response if your extension needs to return a different value. Return null to use core response.
   *         You can also modify the coreResponse if it is aPOGO that allows modification.
   */
  OrderReleaseResponse postRelease(OrderReleaseResponse coreResponse, OrderReleaseRequest orderReleaseRequest)


}