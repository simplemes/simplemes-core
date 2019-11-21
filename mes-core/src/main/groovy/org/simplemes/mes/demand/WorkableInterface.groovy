package org.simplemes.mes.demand

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * This interface represents the smallest possible unit that can be worked.
 * Classes that implement this can be processed with the Work Service.
 * These classes will provide tha ability to start and complete work on the workable element.
 * <p/>
 * <b>Note</b>: These methods are normally provided by the {@link WorkStateTrait} logic.
 */
interface WorkableInterface {

  /**
   * Starts the given qty at this workable location.  This should move the qty from in queue
   * to in work.
   * @param qty The quantity to start.  <b>Default:</b> all quantity in queue
   * @return The quantity actually started.
   */
  BigDecimal startQty(BigDecimal qty)

  /**
   * Starts the given qty at this workable location.  This should move the qty from in queue
   * to in work.
   * @param qty The quantity to start.  <b>Default:</b> all quantity in queue
   * @param dateTime The date/time this qty is started (<b>Default:</b> now).
   * @return The quantity actually started.
   */
  BigDecimal startQty(BigDecimal qty, Date dateTime)

  /**
   * Validates that the given qty can be started at this workable location.
   * @param qty The quantity to start.  <b>Default:</b> all quantity in queue
   * @return The quantity that would be actually started.
   */
  BigDecimal validateStartQty(BigDecimal qty)

  /**
   * Reverses the start of work on an Order/LSN.
   * This moves the in work qty back to in queue.
   * @param qty The quantity to start.  <b>Default:</b> all quantity in work
   * @return The quantity actually reversed.
   */
  BigDecimal reverseStartQty(BigDecimal qty)

  /**
   * Completes the given qty at this workable location.  This should move the qty from in work
   * to the next step (if any)
   * @param qty The quantity to complete.  <b>Default:</b> all quantity in work
   * @return The quantity actually completed.
   */
  BigDecimal completeQty(BigDecimal qty)

  /**
   * Validates that the given qty can be completed at this workable location.
   * @param qty The quantity to complete.  <b>Default:</b> all quantity in work
   * @return The quantity that would be actually completed.
   */
  BigDecimal validateCompleteQty(BigDecimal qty)

  /**
   * Determines the next workable to be performed after this workable is completed.
   * @return The workable.  Null if there is no other workable left to be processed.
   */
  WorkableInterface determineNextWorkable()

  /**
   * Queues the given qty at this workable.
   * @param qty The quantity to queue.
   */
  void queueQty(BigDecimal qty)

}
