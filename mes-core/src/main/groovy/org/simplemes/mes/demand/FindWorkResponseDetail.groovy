package org.simplemes.mes.demand

import groovy.transform.ToString
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.LSNOperState
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.domain.OrderOperState
import org.simplemes.mes.floor.domain.WorkCenter

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The response from the WorkListService.findWork() service method.
 * This represents a single unit in queue or in work.  This can be an order and/or LSN.
 * <p/>
 * This is a POGO used by the {@link org.simplemes.mes.demand.service.WorkListService#findWork(org.simplemes.mes.demand.FindWorkRequest)} method.
 */
@ToString(includeNames = true, includePackage = false)
class FindWorkResponseDetail {

  /**
   * The unique ID of this row.  The lowest element for the row will be used (LSNOperState, OrderOperState, LNS then Order).
   * This will be consistent for later requests.  Suitable for use
   * in identifying the selected records.
   */
  def id

  /**
   * The order that is in work or queue.  This is a simple string to reduce the size of this object.
   */
  String order

  /**
   * The ID of the order that is in work or queue.  This can be used to find the original record.
   */
  def orderID

  /**
   * The LSN that is in work or queue.   This is a simple string to reduce the size of this object.
   */
  String lsn = ''

  /**
   * The ID of the LSN that is in work or queue.  This can be used to find the original record.
   */
  def lsnID

  /**
   * The operation this qty is at.  (0 means not at an operation)
   */
  int operationSequence = 0

  /**
   * The qty in queue.
   */
  BigDecimal qtyInQueue

  /**
   * The qty in queue.
   */
  BigDecimal qtyInWork

  /**
   * The work center the unit is in.
   */
  WorkCenter workCenter

  /**
   * True if the unit is in work.
   */
  boolean inWork

  /**
   * True if the unit is in queue.
   */
  boolean inQueue

  /**
   * Map constructor.  Provides the standard Groovy map constructor.
   * @param options The options map
   */
  FindWorkResponseDetail(Map options) {
    options?.each { k, v -> this[k as String] = v }
    init()
  }

  /**
   * Copy constructor.  Used to copy the state from the order into this detail record.
   * Copies the top-level qty's and sets the flags as needed.
   * @param order The order.
   */
  FindWorkResponseDetail(Order order) {
    this.order = order.order
    this.orderID = order.uuid
    qtyInQueue = order.qtyInQueue
    qtyInWork = order.qtyInWork
    id = order.uuid
    init()
  }

  /**
   * Copy constructor.  Used to copy the state from the LSN into this detail record.
   * Copies the top-level qty's and sets the flags as needed.
   * @param lsn The LSN.
   */
  FindWorkResponseDetail(LSN lsn) {
    this.order = lsn.order.order
    this.orderID = lsn.order.uuid
    this.lsn = lsn.lsn
    this.lsnID = lsn.uuid
    qtyInQueue = lsn.qtyInQueue
    qtyInWork = lsn.qtyInWork
    id = lsn.uuid
    init()
  }

  /**
   * Copy constructor.  Used to copy the state from the order step state into this detail record.
   * Copies the step state qty's and sets the flags as needed.
   * @param orderOperState The order step state.
   */
  FindWorkResponseDetail(OrderOperState orderOperState) {
    this.order = orderOperState.order.order
    this.orderID = orderOperState.order.uuid
    qtyInQueue = orderOperState.qtyInQueue
    qtyInWork = orderOperState.qtyInWork
    operationSequence = orderOperState.sequence
    id = orderOperState.uuid
    init()
  }

  /**
   * Copy constructor.  Used to copy the state from the LSN step state into this detail record.
   * Copies the step state qty's and sets the flags as needed.
   * @param lsnOperState The LSN step state.
   */
  FindWorkResponseDetail(LSNOperState lsnOperState) {
    order = lsnOperState.lsn.order.order
    orderID = lsnOperState.lsn.order.uuid
    lsn = lsnOperState.lsn.lsn
    lsnID = lsnOperState.lsn.uuid
    qtyInQueue = lsnOperState.qtyInQueue
    qtyInWork = lsnOperState.qtyInWork
    operationSequence = lsnOperState.sequence
    id = lsnOperState.uuid
    init()
  }

  /**
   * Finishes the common logic for most of the copy constructors.
   * Mainly sets the inWork/Queue flags based on the quantities.
   */
  def init() {
    inQueue = (qtyInQueue)
    inWork = (qtyInWork)
  }

}
