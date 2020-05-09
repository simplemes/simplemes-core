package org.simplemes.mes.system

import groovy.transform.ToString

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The suggested client dashboard action is the change the current order/LSN.
 * This action is recommended based on the barcode scanned.
 */
@ToString(includePackage = false, includeNames = true)
class OrderLSNChangeAction extends ScanAction {
  /**
   * The action.type for this action.
   */
  static final String TYPE_ORDER_LSN_CHANGED = 'ORDER_LSN_CHANGED'

  /**
   * The recommended client action.
   */
  String type = TYPE_ORDER_LSN_CHANGED

  /**
   * The list of new Order/LSN value(s).
   */
  List<OrderLSNChangeDetail> list

}

class OrderLSNChangeDetail {
  /**
   * The new order.
   */
  String order

  /**
   * The new LSN.
   */
  String lsn

  /**
   * The qty in queue after processing.
   */
  BigDecimal qtyInQueue

  /**
   * The qty in work after processing.
   */
  BigDecimal qtyInWork

  /**
   * The qty done (completed) after processing.
   */
  BigDecimal qtyDone

}
