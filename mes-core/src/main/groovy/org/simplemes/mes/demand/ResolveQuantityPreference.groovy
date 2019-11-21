package org.simplemes.mes.demand

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Defines the valid preferences needed for most resolve style functions.  This includes whether to prefer in queue or
 * in work quantities.
 * <p/>
 * Original Author: mph
 *
 */
enum ResolveQuantityPreference {

  /**
   * The logic prefers in queue quantities first, then in work.
   *
   */
  QUEUE_OR_WORK("O", true, true),

  /**
   * The logic prefers in queue quantities.  In work quantities are ignored.
   *
   */
  QUEUE("Q", true, false),

  /**
   * The logic prefers in work quantities.  In queue quantities are ignored.
   *
   */
  WORK("W", false, true),

  /**
   * The ID used for XML references.
   */
  final String id

  /**
   * If true, then this enum means the caller wants in queue quantities.  Used only by the
   * areQuantitiesPreferred() method to avoid large switch statements.
   */
  final boolean inQueuePreferred

  /**
   * If true, then this enum means the caller wants in work quantities.  Used only by the
   * areQuantitiesPreferred() method to avoid large switch statements.
   */
  final boolean inWorkPreferred

  /**
   * Build a ResolvePreference entry.  Used only for enums above.
   * @param id The ID for the status.
   * @param inQueuePreferred The quantity in queue is preferred.
   * @param inWorkPreferred The quantity in work is preferred.
   */
  ResolveQuantityPreference(String id, boolean inQueuePreferred, boolean inWorkPreferred) {
    this.id = id
    this.inQueuePreferred = inQueuePreferred
    this.inWorkPreferred = inWorkPreferred
  }

  /**
   * Determines if the given quantities match the desired preferences.
   * @param qtyInQueue The quantity in queue.
   * @param qtyInWork The quantity in work.
   * @return True if the given quantities match the preferences.
   */
  boolean areQuantitiesPreferred(BigDecimal qtyInQueue, BigDecimal qtyInWork) {
    if (inQueuePreferred && qtyInQueue > 0) {
      return true
    }
    if (inWorkPreferred && qtyInWork > 0) {
      return true
    }
    return false
  }


}
