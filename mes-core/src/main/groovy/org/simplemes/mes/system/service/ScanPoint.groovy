package org.simplemes.mes.system.service


import org.simplemes.mes.system.ScanRequestInterface
import org.simplemes.mes.system.ScanResponseInterface

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the API for the scan extensions.
 */
@SuppressWarnings("unused")
interface ScanPoint {
  /**
   * Pre-scan extension method for the scan() method.
   * See the core method {@link ScanService#scan(org.simplemes.mes.system.ScanRequestInterface)} for details.
   * <p>
   * <b>Note</b>: This method is part of the Stable API.
   * @param orderReleaseRequest The details of the request.
   */
  void preScan(ScanRequestInterface scanRequest)

  /**
   * Post-scan extension method for the scan() method.
   * See the core method {@link ScanService#scan(org.simplemes.mes.system.ScanRequestInterface)} for details.
   * <p>
   * <b>Note</b>: This method is part of the Stable API.
   * @param coreResponse The response from the core method (or from other extensions that provide a response).
   * @param scanRequest The details of the request .
   * @return Return a new response if your extension needs to return a different value. Return null to use core response.
   *         You can also modify the coreResponse if it is a POGO that allows modification.
   */
  ScanResponseInterface postScan(ScanResponseInterface coreResponse, ScanRequestInterface scanRequest)


}