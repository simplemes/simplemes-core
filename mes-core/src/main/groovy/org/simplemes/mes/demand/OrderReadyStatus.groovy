package org.simplemes.mes.demand

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The order is ready to be processed.
 */
class OrderReadyStatus extends OrderStatus {
  /**
   * The singleton-like instance.
   */
  static OrderReadyStatus instance = new OrderReadyStatus()

  /**
   * The database representation of this status.
   */
  public static final String ID = 'READY'

  /**
   * Empty constructor.
   */
  OrderReadyStatus() {
    defaultChoice = true
  }

  /**
   * Returns true if this status means that the object is workable.
   * @return True if workable.
   */
  @Override
  boolean isWorkable() {
    return true
  }

  /**
   * Returns true if this status means that the object is done.
   * @return True if done.
   */
  @Override
  boolean isDone() {
    return false
  }

  /**
   * Returns the encoded ID of this value.
   * @return The encoded value.
   */
  @Override
  String getId() {
    return ID
  }
}
