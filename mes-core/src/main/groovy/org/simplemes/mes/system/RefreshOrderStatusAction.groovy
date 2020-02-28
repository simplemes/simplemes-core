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
class RefreshOrderStatusAction extends ScanAction {
  /**
   * The action.type for this action.
   */
  static final String TYPE_REFRESH_ORDER_STATUS = 'REFRESH_ORDER_STATUS'

  /**
   * The order which changed the status.
   */
  String order

  /**
   * The recommended client action.
   */
  String type = TYPE_REFRESH_ORDER_STATUS
}
