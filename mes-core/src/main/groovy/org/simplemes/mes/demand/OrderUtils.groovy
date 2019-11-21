package org.simplemes.mes.demand

import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order


/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Order/LSN Utility methods.
 */
class OrderUtils {

  /**
   * This resolves an ID or name for an Order/LSN.  This is typically passed as a param in the HTTP request to a controller.
   * <p>
   * The ID is evaluated with this precedence order:
   * <ul>
   *   <li>LSN - ID (numeric)</li>
   *   <li>Order - ID (numeric)</li>
   *   <li>LSN - name</li>
   *   <li>Order - name</li>
   * </ul>
   *
   * @param id The ID to find the Order/LSN for.
   * @return The Order or LSN.  Can be null.
   */
  static Object resolveIdOrName(String id) {
    ArgumentUtils.checkMissing(id, 'id')

    if (id.isNumber()) {
      // Try as ID first.
      def idNumber = Long.parseLong(id)

      def lsn = LSN.findById(idNumber)
      if (lsn) {
        return lsn
      }

      def order = Order.findById(idNumber)
      if (order) {
        return order
      }
    }

    // Not found, so try as name next
    def lsn = LSN.findByLsn(id)
    if (lsn) {
      return lsn
    }

    def order = Order.findByOrder(id)
    if (order) {
      return order
    }

    return null
  }

}
