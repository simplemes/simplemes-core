package org.simplemes.mes.demand

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The LSN is scrapped.
 */
class LSNScrappedStatus extends LSNStatus {
  /**
   * The singleton-like instance.
   */
  static LSNScrappedStatus instance = new LSNScrappedStatus()

  /**
   * The database representation of this status.
   */
  public static final String ID = 'SCRAPPED'

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
    return false
  }

  /**
   * Returns true if this status means that the object has been scrapped.
   * @return True if scrapped.
   */
  @Override
  boolean isScrapped() {
    return true
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
