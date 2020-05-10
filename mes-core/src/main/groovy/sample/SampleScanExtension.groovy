package sample


import org.simplemes.mes.system.ScanRequestInterface
import org.simplemes.mes.system.ScanResponseInterface
import org.simplemes.mes.system.service.GetBarcodePrefixPoint
import org.simplemes.mes.system.service.ScanPoint

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A sample extension to the order release extension point.
 * Not shipped with product.
 */
@Singleton
class SampleScanExtension implements ScanPoint, GetBarcodePrefixPoint {

  /**
   * Used for testing or pre method.
   */
  static preScanRequest

  /**
   * Pre-scan extension method for the scan() method.
   * @param orderReleaseRequest The details of the request.
   */
  @Override
  void preScan(ScanRequestInterface scanRequest) {
    preScanRequest = scanRequest
  }

  /**
   * Post-scan extension method for the scan() method.
   * @param coreResponse The response from the core method (or from other extensions that provide a response).
   * @param scanRequest The details of the request .
   * @return Return a new response if your extension needs to return a different value. Return null to use core response.
   *         You can also modify the coreResponse if it is a POGO that allows modification.
   */
  @Override
  ScanResponseInterface postScan(ScanResponseInterface coreResponse, ScanRequestInterface scanRequest) {
    coreResponse.operationSequence = 237

    if (scanRequest.barcode=='^XYZZY^ORDER_TEST') {
      coreResponse.messageHolder.addInfo([text:"Passed order ${scanRequest.order} for XYZZY input barcode prefix"])
      coreResponse.resolved = true
    }

    return null
  }

  /**
   * Used for testing or pre method.
   */
  static boolean preBarcodeCalled = false

  /**
   * Pre-scan extension method for the getBarcodePrefix() method.
   * See the core method {@link org.simplemes.mes.system.service.ScanService#getBarcodePrefixMapping()} for details.
   * <p>
   * <b>Note</b>: This method is part of the Stable API.
   */

  @Override
  void preGetBarcodePrefixMapping() {
    preBarcodeCalled = true
  }

  /**
   * Post-scan extension method for the getBarcodePrefix() method.
   * See the core method {@link org.simplemes.mes.system.service.ScanService#getBarcodePrefixMapping()} for details.
   * <p>
   * <b>Note</b>: This method is part of the Stable API.
   * @param coreResponse The response from the core method (or from other extensions that provide a response).
   * @return Return a new response if your extension needs to return a different value. Return null to use core response.
   *         You can also modify the coreResponse if it is a POGO that allows modification.
   */
  @Override
  Map<String, String> postGetBarcodePrefixMapping(Map<String, String> coreResponse) {
    coreResponse.extension = 'XYZZY'
    return null
  }
}
