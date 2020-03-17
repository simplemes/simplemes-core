/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.pogo

import groovy.transform.ToString

/**
 * The sample response from the WorkListService.findWork() service method.
 * This represents a single unit in queue or in work.  This can be an order and/or LSN.
 */
@SuppressWarnings("unused")
@ToString(includeNames = true, includePackage = false)
class FindWorkResponseDetail {

  /**
   * A unique ID for the order.
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
   * The the product.
   */
  String product = ''

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
   * True if the unit is in work.
   */
  boolean inWork

  /**
   * True if the unit is in queue.
   */
  boolean inQueue

  /**
   * THe work center.
   */
  String workCenter

}
