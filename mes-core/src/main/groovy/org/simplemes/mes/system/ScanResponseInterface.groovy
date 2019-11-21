package org.simplemes.mes.system

import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.web.undo.UndoableInterface
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The response for the scan() method.  This scan resolution is typically handled by the ScanService.
 * This POGO is returned after the scan has been evaluated.
 */
interface ScanResponseInterface extends UndoableInterface {

  /**
   * The original barcode input string.
   */
  String getBarcode()

  /**
   * The original barcode input string.
   */
  void setBarcode(String barcode)

  /**
   * Set to true if the barcode was resolved successfully.
   */
  boolean getResolved()

  /**
   * Set to true if the barcode was resolved successfully.
   */
  void setResolved(boolean resolved)

  /**
   * The order the scan resolved to.
   */
  Order getOrder()

  /**
   * The order the scan resolved to.
   */
  void setOrder(Order order)

  /**
   * The LSN the scan resolved to.
   */
  LSN getLsn()

  /**
   * The LSN the scan resolved to.
   */
  void setLsn(LSN lsn)

  /**
   * The operation sequence to process. If not provided, then the ResolveService may find the correct operation.
   * Only needed when the order/LSN uses a routing.
   *
   */
  int getOperationSequence()

  /**
   * The operation sequence to process. If not provided, then the ResolveService may find the correct operation.
   * Only needed when the order/LSN uses a routing.
   *
   */
  void setOperationSequence(int operationSequence)

  /**
   * The recommended actions the client will need to take to respond to the scan.  This can include
   * button presses or Dashboard event triggering.
   */

  List<ScanActionInterface> getScanActions()

  /**
   * Messages from the actions performed by the scan service logic (e.g. from the Start, Complete, etc).
   */
  MessageHolder getMessageHolder()

  /**
   * Sets the parsed barcode content.  This is a map with the key being the standardized logic name for the barcode element.
   * For example: [BUTTON: 'START']
   * @param map The barcode parse map.
   */
  void setParsedBarcode(Map map)

  /**
   * Returns the parsed barcode content.  This is a map with the key being the standardized logic name for the barcode element.
   * For example: [BUTTON: 'START']
   * @return The barcode parse map.
   */
  Map getParsedBarcode()

}
