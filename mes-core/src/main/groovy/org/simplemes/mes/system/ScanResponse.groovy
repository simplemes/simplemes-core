package org.simplemes.mes.system


import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import groovy.transform.ToString
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.json.JSONByKey
import org.simplemes.eframe.web.undo.UndoActionInterface
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
@JsonTypeName(value = "scanResponse")
// Forces the top-level element to be 'scanResponse' as the client expects.
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@ToString(includeNames = true, includePackage = false)
class ScanResponse implements ScanResponseInterface {

  /**
   * The original barcode input string.
   */
  String barcode

  /**
   * Set to true if the barcode was resolved successfully.
   */
  boolean resolved = false

  /**
   * The order the scan resolved to.
   */
  @JSONByKey
  Order order

  /**
   * The LSN the scan resolved to.
   */
  @JSONByKey
  LSN lsn

  /**
   * The operation sequence to process. If not provided, then the ResolveService may find the correct operation.
   * Only needed when the order/LSN uses a routing.
   *
   */
  int operationSequence

  /**
   * The recommended actions the client will need to take to respond to the scan.  This can include
   * button presses or Dashboard event triggering.
   * The Maps in this list use the action's properties as the key.  This allows easier conversion to JSON for
   * the client.
   */
  List scanActions = []

  /**
   * Messages from the actions performed by the scan service logic (e.g. from the Start, Complete, etc).
   */
  MessageHolder messageHolder = new MessageHolder()

  /**
   * The parsed barcode content.  This is a map with the key being the standardized logic name for the barcode element.
   * For example: [BUTTON: 'START']
   */
  Map parsedBarcode

  /**
   * The undo action(s) needed to undo the previous user action.
   */
  List<UndoActionInterface> undoActions = []

  /**
   * The copy constructor to use with a generic ScanRequest.  Copies the common fields to the new object.
   * @param scanRequest The scan request to create the response from.
   */
  ScanResponse(ScanRequestInterface scanRequest) {
    this.barcode = scanRequest.barcode
  }


}
