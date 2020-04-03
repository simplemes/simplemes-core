package org.simplemes.mes.demand

import groovy.transform.ToString
import org.simplemes.eframe.json.JSONByKey
import org.simplemes.eframe.web.undo.UndoActionInterface
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The response for Complete requests.  This returns the element that was completed.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.demand.service.WorkService#complete} method.
 */
@ToString(includeNames = true, includePackage = false)
class CompleteResponse {

  /**
   * The order completed
   */
  @JSONByKey
  Order order

  /**
   * The Lot/Serial Number (LSN) completed (<b>May be null:</b>).
   */
  @JSONByKey
  LSN lsn

  /**
   * The operation sequence completed. Can be 0 to indicate no operation.
   *
   */
  int operationSequence = 0

  /**
   * The quantity completed.
   */
  BigDecimal qty

  /**
   * Set to true if the order/LSN is done.
   */
  boolean done = false

  /**
   * If false, then no undo actions will be provided.
   */
  boolean allowUndo = true

  /**
   * The undo action(s) needed to undo this complete.
   */
  List<UndoActionInterface> getUndoActions() {
    if (allowUndo) {
      return [new CompleteUndoAction(this)]
    }
    return []
  }


}
