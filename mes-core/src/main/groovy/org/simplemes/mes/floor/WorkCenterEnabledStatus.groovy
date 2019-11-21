package org.simplemes.mes.floor
/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The Work Center is enabled for processing.
 */
class WorkCenterEnabledStatus extends WorkCenterStatus {
  /**
   * The singleton-like instance.
   */
  static WorkCenterEnabledStatus instance = new WorkCenterEnabledStatus()

  /**
   * The database representation of this status.
   */
  public static final String ID = 'ENABLED'

  /**
   * Empty constructor.
   */
  WorkCenterEnabledStatus() {
    defaultChoice = true
  }

  /**
   * Returns true if this status means that the object is enabled.
   * @return True if workable.
   */
  @Override
  boolean isEnabled() {
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
