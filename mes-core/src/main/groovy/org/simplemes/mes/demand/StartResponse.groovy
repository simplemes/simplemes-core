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
 * The response for Start requests.  This returns the element that was started.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.demand.service.WorkService#start} method.
 */
@ToString(includeNames = true, includePackage = false)
class StartResponse {

  /**
   * The order started
   */
  @JSONByKey
  Order order

  /**
   * The Lot/Serial Number (LSN) started (<b>May be null:</b>).
   */
  @JSONByKey
  LSN lsn

  /**
   * The operation sequence started. Can be 0 to indicate no operation.
   *
   */
  int operationSequence

  /**
   * The quantity started.
   */
  BigDecimal qty

  /**
   * If false, then no undo actions will be provided.
   */
  boolean allowUndo = true

  /**
   * The undo action(s) needed to undo this start.
   */
  List<UndoActionInterface> getUndoActions() {
    if (allowUndo) {
      return [new StartUndoAction(this)]
    }
    return []
  }

}
