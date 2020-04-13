package sample

import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.OrderReleaseResponse
import org.simplemes.mes.demand.service.OrderReleasePoint

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A sample extension to the order release extension point.
 * Not shipped with product.
 */
@Singleton
class SampleOrderExtension implements OrderReleasePoint {

  /**
   * Used for testing or pre method.
   */
  static preOrderRequest


  /**
   * Pre-release extension method for the order release process.
   * @param orderReleaseRequest The details of the release request (order and qty).
   */
  @Override
  void preRelease(OrderReleaseRequest orderReleaseRequest) {
    preOrderRequest = orderReleaseRequest
  }

  /**
   * Post-release extension method for the order release process.
   * <p>
   * @param coreResponse The response from the core method (or from other extensions that provide a response).
   * @param orderReleaseRequest The details of the release request (order and qty).
   * @return A replacement response value is used only for order EXT_TEST_1001.
   */
  @Override
  OrderReleaseResponse postRelease(OrderReleaseResponse coreResponse, OrderReleaseRequest orderReleaseRequest) {
    if (orderReleaseRequest.order.order == 'EXT_TEST_1001') {
      return new OrderReleaseResponse(order: coreResponse.order, qtyReleased: 237.2)
    }
    return null
  }
}
