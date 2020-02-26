package org.simplemes.mes.demand

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.UUIDUtils
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.ResolveService

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
   * This will fallback to the ResolveService if the id is not a UUID.
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

    // Not found as UUID, so try as name next using the resolve service
    ResolveService resolveService = Holders.getBean(ResolveService)
    def resolveResponse = resolveService.resolveID(new ResolveIDRequest(barcode: id))
    if (resolveResponse.lsn) {
      return resolveResponse.lsn
    }
    if (resolveResponse.order) {
      return resolveResponse.order
    }

    return null
  }

}
