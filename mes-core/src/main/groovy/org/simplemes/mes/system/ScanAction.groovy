package org.simplemes.mes.system

import groovy.transform.EqualsAndHashCode
import org.simplemes.eframe.json.TypeableJSONInterface

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The response for client actions recommended by the
 * {@link org.simplemes.mes.system.service.ScanService#scan} method.
 * This action is recommended based on the barcode scanned.
 */
@EqualsAndHashCode()
class ScanAction implements ScanActionInterface, TypeableJSONInterface {

  /**
   * The recommended client action.
   */
  String type

  /**
   * The server.
   */
  String source = 'server'
}
