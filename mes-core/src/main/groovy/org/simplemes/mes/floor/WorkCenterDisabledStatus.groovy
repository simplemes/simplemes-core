package org.simplemes.mes.floor
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The Work Center is not enabled for processing.
 */
class WorkCenterDisabledStatus extends WorkCenterStatus {
  /**
   * The singleton-like instance.
   */
  static WorkCenterDisabledStatus instance = new WorkCenterDisabledStatus()

  /**
   * The database representation of this status.
   */
  public static final String ID = 'DISABLED'

  /**
   * Returns true if this status means that the object is enabled.
   * @return True if workable.
   */
  @Override
  boolean isEnabled() {
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
