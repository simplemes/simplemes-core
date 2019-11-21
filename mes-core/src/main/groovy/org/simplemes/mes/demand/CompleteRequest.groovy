package org.simplemes.mes.demand

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.json.JSONByKey
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The request argument for Complete.  Defines the input values for the complete() and validateComplete service methods.
 * <p/>
 * This request must resolve to a specific workable object.  This is usually an order or LSN at a specific
 * operation.  The fields actually needed depend on the configuration of data in your system.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.demand.service.WorkService#complete} method.
 */
@EqualsAndHashCode
@ToString(includeNames = true, includePackage = false)
class CompleteRequest {

  /**
   * A generic user or barcode input string.  This is typically an order or an LSN.  SimpleMES will attempt
   * to resolve this first as an LSN then an order.
   */
  String barcode

  /**
   * The order to complete (<b>Required:</b> order or lsn).<p/>
   * If not provided, then the LSN is used to find the appropriate order.
   */
  @JSONByKey
  Order order

  /**
   * The Lot/Serial Number (LSN) to complete (<b>Required:</b> order or lsn).
   */
  @JSONByKey
  LSN lsn

  /**
   * The operation sequence to complete. If not provided, then the ResolveService may find the correct operation.
   * Only needed when the order/LSN uses a routing.
   *
   */
  int operationSequence

  /**
   * The work center this work is being performed at. <b>Optional.</b>
   */
  @JSONByKey
  WorkCenter workCenter

  /**
   * The quantity to be completed.  If not provided, then the remaining quantity for the order is used. <b>Optional.</b>
   */
  BigDecimal qty

  /**
   * The date/time this action took place (<b>Default:</b> now).
   */
  Date dateTime

}
