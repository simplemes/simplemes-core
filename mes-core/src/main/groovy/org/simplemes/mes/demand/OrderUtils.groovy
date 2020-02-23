package org.simplemes.mes.demand

import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.UUIDUtils
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
   * This resolves an UUID or name for an Order/LSN.  This is typically passed as a param in the HTTP request to a controller.
   * <p>
   * The UUID is evaluated with this precedence order:
   * <ul>
   *   <li>LSN - UUID (uuid string)</li>
   *   <li>Order - UUID (uuid string)</li>
   *   <li>LSN - name</li>
   *   <li>Order - name</li>
   * </ul>
   *
   * @param id The ID to find the Order/LSN for.
   * @return The Order or LSN.  Can be null.
   */
  static Object resolveUuidOrName(String id) {
    ArgumentUtils.checkMissing(id, 'id')

    def uuid = UUIDUtils.convertToUUIDIfPossible(id)

    if (uuid instanceof UUID) {
      // Try as UUID first, it if fits the format correctly.
      def lsn = LSN.findByUuid(uuid)
      if (lsn) {
        return lsn
      }

      def order = Order.findByUuid(uuid)
      if (order) {
        return order
      }
    }

    // Not found as UUID, so try as name next
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
