package org.simplemes.mes.demand

import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Request values for the resolveWorkable() method(s).  The service {@link org.simplemes.mes.demand.service.ResolveService}
 * is the best place to resolve an order/lsn to a workable unit for processing.
 * <p/>
 * This is a POGO and has no internal logic.  It is used mainly for passing values to the business logic.
 *
 * Original Author: mph
 *
 */
class ResolveWorkableRequest {
  /**
   * A generic user or barcode input string.  This is typically an order or an LSN.  SimpleMES will attempt
   * to resolve this first as an LSN then an order.
   */
  String barcode

  /**
   * The order that was provided by the client to resolve.
   */
  Order order

  /**
   * The LSN that was provided by the client to resolve.
   */
  LSN lsn

  /**
   * The quantity to be processed.  <b>Default:</b> 1.0
   */
  BigDecimal qty

  /**
   * The operation sequence to process. If not provided, then the ResolveService may find the correct operation.
   * Only needed when the order/LSN uses a routing.
   *
   */
  int operationSequence

  /**
   * The work center this work is taking place in.  Used in some cases to help
   */
  WorkCenter workCenter
}
