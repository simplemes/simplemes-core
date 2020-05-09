package org.simplemes.mes.system

import groovy.transform.ToString
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The request argument for scan method.  This scan resolution is typically handled by the ScanService.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.system.service.ScanService#scan} method.
 */
@ToString(includeNames = true, includePackage = false)
class ScanRequest implements ScanRequestInterface {

  /**
   * A generic user or barcode input string.  This is the ID that will be resolved to a specific object.
   */
  String barcode

  /**
   * The current order from the client (e.g. scan dashboard).
   */
  Order order

  /**
   * The current LSN from the client (e.g. scan dashboard).
   */
  LSN lsn

  /**
   * The current operation sequence from the client (e.g. scan dashboard).  This is the current operation being
   * processed on the client.
   * Only needed when the order/LSN uses a routing.
   *
   */
  int operationSequence

  /**
   * Constructor for use with JSON body request.  This is a lenient form of the normal constructor.  It ignores
   * values from the options that are not part of this POGO.  This makes the controller more reliable when
   * the client sends in incorrect values.
   * @param options The options from the JSON Body.
   */
  ScanRequest(Map options) {
    options.each { k, v ->
      String key = k
      if (v) {
        if (key == 'order') {
          if (v instanceof String) {
            order = Order.findByOrder(v)
            if (!order) {
              //error.110.message=Could not find {0} {1}
              throw new BusinessException(110, ['order', v])
            }
          } else {
            order = v as Order
          }
        } else if (key == 'lsn') {
          if (v instanceof String) {
            lsn = LSN.findByLsn(v)
            if (!lsn) {
              //error.110.message=Could not find {0} {1}
              throw new BusinessException(110, ['lsn', v])
            }
          } else {
            lsn = v as LSN
          }
          if (!order) {
            order = lsn?.order
          }
        } else if (this.hasProperty(key)) {
          this[key] = v
        }
      }
    }
  }
}
