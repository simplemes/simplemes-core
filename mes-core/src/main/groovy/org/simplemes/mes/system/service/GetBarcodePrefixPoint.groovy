package org.simplemes.mes.system.service
/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the API for the barcode prefix logic extensions.
 */
@SuppressWarnings("unused")
interface GetBarcodePrefixPoint {
  /**
   * Pre-scan extension method for the getBarcodePrefix() method.
   * See the core method {@link ScanService#getBarcodePrefixMapping()} for details.
   * <p>
   * <b>Note</b>: This method is part of the Stable API.
   */
  void preGetBarcodePrefixMapping()

  /**
   * Post-scan extension method for the getBarcodePrefix() method.
   * See the core method {@link ScanService#getBarcodePrefixMapping()} for details.
   * <p>
   * <b>Note</b>: This method is part of the Stable API.
   * @param coreResponse The response from the core method (or from other extensions that provide a response).
   * @return Return a new response if your extension needs to return a different value. Return null to use core response.
   *         You can also modify the coreResponse if it is a POGO that allows modification.
   */
  Map<String, String> postGetBarcodePrefixMapping(Map<String, String> coreResponse)


}