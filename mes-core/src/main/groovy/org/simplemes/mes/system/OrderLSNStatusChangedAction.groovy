package org.simplemes.mes.system

import groovy.transform.ToString

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The suggested client dashboard action is to refresh the order/LSN status in the display.
 * This action is recommended based on the barcode scanned.
 */
@ToString(includePackage = false, includeNames = true)
class OrderLSNStatusChangedAction extends ScanAction {
  /**
   * The action.type for this action.
   */
  static final String TYPE_ORDER_LSN_STATUS_CHANGED = 'ORDER_LSN_STATUS_CHANGED'

  /**
   * The list of new Order/LSN value(s).
   */
  List<OrderLSNStatusChangeDetail> list

  /**
   * The recommended client action.
   */
  String type = TYPE_ORDER_LSN_STATUS_CHANGED
}

class OrderLSNStatusChangeDetail {
  /**
   * The order that was changed.
   */
  String order

  /**
   * The LSN that was changed.
   */
  String lsn

}

