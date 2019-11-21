package org.simplemes.mes.system
/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The response for client actions recommended by the
 * {@link org.simplemes.mes.system.service.ScanService#scan} method.
 * This action is recommended based on the barcode scanned.
 */
interface ScanActionInterface {

  /**
   * The recommended client action type.
   */
  String getType()

}
