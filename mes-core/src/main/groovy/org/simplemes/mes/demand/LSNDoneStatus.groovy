package org.simplemes.mes.demand

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The LSN is done (finished all work).
 */
class LSNDoneStatus extends LSNStatus {
  /**
   * The singleton-like instance.
   */
  static LSNDoneStatus instance = new LSNDoneStatus()

  /**
   * The database representation of this status.
   */
  public static final String ID = 'DONE'

  /**
   * Returns true if this status means that the object is workable.
   * @return True if workable.
   */
  @Override
  boolean isWorkable() {
    return false
  }

  /**
   * Returns true if this status means that the object is done.
   * @return True if done.
   */
  @Override
  boolean isDone() {
    return true
  }

  /**
   * Returns true if this status means that the object has been scrapped.
   * @return True if scrapped.
   */
  @Override
  boolean isScrapped() {
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
