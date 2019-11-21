package org.simplemes.mes.system
/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The suggested client dashboard action is the change the current order/LSN.
 * This action is recommended based on the barcode scanned.
 */
class OrderLSNChangeAction extends ScanAction {
  /**
   * The action.type for this action.
   */
  static final String TYPE_ORDER_LSN_CHANGE = 'ORDER_LSN_CHANGE'

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
   * The recommended client action.
   */
  @Override
  String getType() {
    return TYPE_ORDER_LSN_CHANGE
  }
}
