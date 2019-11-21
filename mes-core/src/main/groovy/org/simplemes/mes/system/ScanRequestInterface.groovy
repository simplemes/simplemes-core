package org.simplemes.mes.system

import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The request argument for the scan() method.  This scan resolution is typically handled by the ScanService.
 * <p/>
 * This is usually a POGO used by the {@link org.simplemes.mes.system.service.ScanService#scan} method.
 */
@SuppressWarnings("GrFinalVariableAccess")
interface ScanRequestInterface {

  /**
   * A generic user or barcode input string.  This is the ID that will be resolved to a specific object.
   */
  String getBarcode()

  /**
   * A generic user or barcode input string.  This is the ID that will be resolved to a specific object.
   */
  void setBarcode(String barcode)

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


}
