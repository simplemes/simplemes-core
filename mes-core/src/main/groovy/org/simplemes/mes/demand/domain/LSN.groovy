package org.simplemes.mes.demand.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.mes.demand.DemandObject
import org.simplemes.mes.demand.LSNStatus
import org.simplemes.mes.demand.WorkStateTrait
import org.simplemes.mes.demand.WorkableInterface
import org.simplemes.mes.misc.FieldSizes

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

/**
 * Defines a lot/serial number (LSN) for tracking and identification of demand.  This a child of the
 * {@link org.simplemes.mes.demand.domain.Order}.
 *
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ['lsn', 'order'])
@ToString(includeNames = true, includePackage = false, excludes = ['order'])
class LSN implements WorkStateTrait, WorkableInterface, DemandObject {
  /**
   * The Lot/Serial Number (LSN) of the unit to be tracked.
   */
  @Column(length = FieldSizes.MAX_LSN_LENGTH, nullable = false)
  String lsn
  // TODO: Add unique constraint on order/lsn to DDL.  Index on LSN/Order?

  /**
   * An LSN always belongs to one order.  It is a child of the order and may be processed individually within
   * the order in many cases.
   */
  @ManyToOne
  Order order

  /**
   * The status of this LSN.  Uses the default LSNStatus if none is provided.
   */
  LSNStatus status

  /**
   * The number of pieces to be built for this LSN.
   */
  BigDecimal qty = 1.0

  /**
   * This is the list operation states that corresponds to the current LSN's orderRouting's operations.  This holds the quantities
   * in queue, in work, etc for a given operation. This list is sorted in the same order as the orderRouting's operations
   *
   */
  @OneToMany(mappedBy = "lsn")
  List<LSNOperState> operationStates

  /**
   * The number of pieces waiting to be worked (in queue) for this object.
   * <p/><b>WorkStateTrait field</b>.
   */
  BigDecimal qtyInQueue = 0.0

  /**
   * The number of pieces actively in work for this object.
   * <p/><b>WorkStateTrait field</b>.
   */
  BigDecimal qtyInWork = 0.0

  /**
   * The number of pieces to completed (done) on this object.
   * <p/><b>WorkStateTrait field</b>.
   */
  BigDecimal qtyDone = 0.0

  /**
   * The date/time any quantity was queued at this point (operation or top-level).
   * Can be null if the nothing is in queue.
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateQtyQueued

  /**
   * The date/time any quantity was started at this point (operation or top-level).
   * Can be null if the nothing is in work.
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateQtyStarted

  /**
   * The date/time any quantity was first queued at this point (operation or top-level).
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateFirstQueued

  /**
   * The date/time any quantity was first started at this point (operation or top-level).
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateFirstStarted

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  Integer version = 0

  @Id @AutoPopulated UUID uuid

  /**
   * The primary keys for this record are order and lsn.  Order is the parent key.
   */
  @SuppressWarnings("unused")
  static keys = ['lsn', 'order']

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['lsn', 'status', 'qty', 'qtyInQueue', 'qtyInWork', 'qtyDone']

  /**
   * Called before validate happens.
   */
  def beforeValidate() {
    setStatusIfNeeded()
  }

  /**
   * Sets the status if it is null.
   */
  private void setStatusIfNeeded() {
    status = status ?: (LSNStatus) LSNStatus.default
  }

  /**
   * Determines the next workable to be performed after this workable is completed.
   * @return The workable (null for this top-level tracking scenario).
   */
  WorkableInterface determineNextWorkable() {
    return null
  }

  /**
   * Determines the next workable to be performed after the given operation is done.
   * @param currentOperState The current operation to find the next operation for.
   * @return The workable.  Null if there is no other workable left to be processed.
   */
  WorkableInterface determineNextWorkable(LSNOperState currentOperState) {
    int nextSeq = order.determineNextOperation(currentOperState.sequence)
    if (nextSeq == 0) {
      return null
    }
    return operationStates.find { it.sequence == nextSeq }
  }

  /**
   * Saves any changes to this record.
   */
  void saveChanges() {
    save()
  }

  /**
   * Returns the important key field(s) as a short, simple string.
   * @return The short string.
   */
  String toShortString() {
    return lsn
  }

}
