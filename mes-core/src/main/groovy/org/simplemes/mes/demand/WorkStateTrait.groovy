package org.simplemes.mes.demand


import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.misc.TypeUtils

import javax.annotation.Nullable

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the Domain classes with a generic Work State.
 * This object holds the work state of an operation or an overall object (e.g. Order or LSN).
 * This work state contains the quantities in queue, work and done.
 * <p/>
 * This abstract base class provides the basic quantity fields and interface methods.  Sub-classes
 * will normally provide foreign keys to link this work state to the parent object (e.g. Order or LSN).
 * <p>
 * <b>Note</b>: Implementers of this trait should provide a meaningful toString() method (used for error messages).
 *
 * <h3>Required Fields in Domain Classes</h3>
 * The following fields must be implemented in the domain entity class:
 * <ul>
 *   <li><b>qtyInQueue</b> - The number of pieces waiting to be worked (in queue) for this object.</li>
 *   <li><b>qtyInWork</b> - The number of pieces actively in work for this object.</li>
 *   <li><b>qtyDone</b> - The number of pieces to completed (done) on this object.</li>
 *   <li><b>dateQtyQueued</b> - The date/time any quantity was queued at this point (operation or top-level).
 *                              Can be null if the nothing is in queue.</li>
 *   <li><b>dateQtyStarted</b> - The date/time any quantity was started at this point (operation or top-level).
 *                               Can be null if the nothing is in work.</li>
 *   <li><b>dateFirstQueued</b> - The date/time any quantity was first queued at this point (operation or top-level).</li>
 *   <li><b>dateFirstStarted</b> - The date/time any quantity was first started at this point (operation or top-level).</li>
 * </ul>
 */
trait WorkStateTrait {

  /**
   * Starts the given qty at this order (top-level) location.  This moves the qty from 'in queue'
   * to 'in work'. Saves the changes to the order.
   * @param qty The quantity to start (<b>Default:</b> all quantity in queue).
   * @param dateTime The date/time this qty is started (<b>Default:</b> now).
   * @return The quantity actually started.
   */
  BigDecimal startQty(BigDecimal qty, Date dateTime = null) {
    qty = validateStartQty(qty)

    // Now, just move the qty to the right place.
    qtyInWork += qty
    qtyInQueue -= qty
    setDatesAsNeeded(dateTime)
    // Set the date started if not already set to the given value or now.
    dateQtyStarted = dateQtyStarted ?: dateTime ?: new Date()
    // and make sure the dateQtyQueued is cleared if nothing is left in queue.
    if (!qtyInQueue) {
      dateQtyQueued = null
    }
    saveChanges()
    return qty
  }

  /**
   * Validates that the given qty can be started at this workable location.
   * @param qty The quantity to start.  <b>Default:</b> all quantity in queue
   * @return The quantity that would be actually started.
   */
  BigDecimal validateStartQty(BigDecimal qty) {
    qty = qty ?: qtyInQueue
    if (qty <= 0) {
      // error.3002.message=Quantity ({0}) must be greater than 0
      throw new BusinessException(3002, [qty])
    }
    if (qty > qtyInQueue) {
      // error.3003.message=Quantity to start ({0}) must be less than or equal to the quantity in queue ({1}) at {2}
      throw new BusinessException(3003, [qty, qtyInQueue, TypeUtils.toShortString(this)])
    }

    return qty
  }

  /**
   * Reverses the start of work on an Order/LSN.
   * This moves the in work qty back to in queue.
   * @param qty The quantity to reverse the start for.
   * @param dateTime The date/time this qty start is reversed (<b>Default:</b> now).
   * @return The quantity actually reversed.
   */
  BigDecimal reverseStartQty(BigDecimal qty, Date dateTime = null) {
    // Make sure we have something in work to reverse.
    validateCompleteQty(qty)
    // Now, just move the qty to the right place.
    qtyInWork -= qty
    qtyInQueue += qty
    // Set the date queued if not already set to the given value or now.
    dateQtyQueued = dateQtyQueued ?: dateTime ?: new Date()
    // and make sure the dateQtyStarted is cleared if nothing is left in work.
    if (!qtyInWork) {
      dateQtyStarted = null
    }
    saveChanges()
    return qty
  }

  /**
   * Completes the given qty at this order (top-level) location.  This moves the qty from 'in work'
   * to 'done'.
   * @param qty The quantity to complete.  <b>Default:</b> all quantity in work
   * @return The quantity actually completed.
   */
  BigDecimal completeQty(BigDecimal qty) {
    qty = validateCompleteQty(qty)

    // Now, just move the qty to the right place.
    qtyInWork -= qty
    qtyDone += qty
    if (!qtyInWork) {
      dateQtyStarted = null
    }
    setDatesAsNeeded()
    saveChanges()

    return qty
  }

  /**
   * Validates that the given qty can be completed at this workable location.
   * @param qty The quantity to complete.  <b>Default:</b> all quantity in work
   * @return The quantity that would be actually completed.
   */
  BigDecimal validateCompleteQty(BigDecimal qty) {
    qty = qty ?: qtyInWork
    if (qty <= 0) {
      // error.3007.message=Quantity to process ({0}) must be greater than 0
      throw new BusinessException(3007, [qty])
    }
    if (qty > qtyInWork) {
      // error.3008.message=Quantity to process ({0}) must be less than or equal to the quantity in work ({1}) at {2}
      throw new BusinessException(3008, [qty, qtyInWork, TypeUtils.toShortString(this)])
    }

    return qty
  }

  /**
   * Queues the given qty at this workable. This method does not trigger the save() on the workable element.
   * @param qty The quantity to queue.
   * @param dateTime The date/time this qty is queued (<b>Default:</b> now).
   */
  void queueQty(BigDecimal qty, Date dateTime = null) {
    if (qty <= 0) {
      // error.3002.message=Quantity ({0}) must be greater than 0
      throw new BusinessException(3002, [qty])
    }
    qtyInQueue += qty
    // Set the date queued if not already set to the given value or now.
    dateQtyQueued = dateQtyQueued ?: dateTime ?: new Date()
    setDatesAsNeeded(dateTime)
    // TODO: Is this needed still? saveChanges()
  }

  /**
   * Sets the dates queued/started if the appropriate qty is non-zero.
   * @param dateTime The date/time this qty is started (<b>Default:</b> now).
   *
   */
  def setDatesAsNeeded(Date dateTime = null) {
    if (!dateFirstQueued && qtyInQueue) {
      dateFirstQueued = dateTime ?: new Date()
    }
    if (!dateFirstStarted && qtyInWork) {
      dateFirstStarted = dateTime ?: new Date()
    }
  }

  /**
   * Basic setter.  Also sets the dateFirstQueued is needed.
   * @param qtyInQueue The qty.
   */
  void setQtyInQueue(BigDecimal qtyInQueue) {
    this.qtyInQueue = qtyInQueue
    setDatesAsNeeded()
  }

  /**
   * Basic setter.  Also sets the dateFirstStarted is needed.
   * @param qtyInWork The qty.
   */
  void setQtyInWork(BigDecimal qtyInWork) {
    this.qtyInWork = qtyInWork
    setDatesAsNeeded()
  }

  /**
   * Implementers must provide a save() method.  This should save() changes to the record.
   */
  abstract void saveChanges()

  // Getters and setters for the required fields.
  abstract BigDecimal getQtyInQueue()


  abstract BigDecimal getQtyInWork()


  abstract BigDecimal getQtyDone()

  abstract void setQtyDone(BigDecimal qtyDone)

  abstract Date getDateQtyQueued()

  abstract void setDateQtyQueued(@Nullable Date dateQtyQueued)

  abstract Date getDateQtyStarted()

  abstract void setDateQtyStarted(@Nullable Date dateQtyStarted)

  abstract Date getDateFirstQueued()

  abstract void setDateFirstQueued(@Nullable Date dateFirstQueued)

  abstract Date getDateFirstStarted()

  abstract void setDateFirstStarted(@Nullable Date dateFirstStarted)

}