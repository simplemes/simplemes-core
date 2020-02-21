package org.simplemes.mes.demand.domain

import groovy.transform.EqualsAndHashCode
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.mes.demand.WorkStateTrait
import org.simplemes.mes.demand.WorkableInterface
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.RoutingUtils
import org.simplemes.mes.product.domain.RoutingOperation

import javax.persistence.ManyToOne

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * This object holds the work state of an operation for an order.  This domain uses the WorkStateTrait to provide
 * most of the logic and fields needed.
 * <p/>
 * This WorkStateTrait contains the quantities in queue, work and done.
 * This object also implements the WorkableInterface to encapsulate the process of working
 * on various objects.
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ['order', 'sequence'])
class OrderOperState implements WorkStateTrait, WorkableInterface {
  /**
   * A copy of the sequence for the operation this state applies to.
   */
  int sequence

  /**
   * This operation state always belongs to a single Order.
   */
  @ManyToOne
  Order order

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
  Date dateQtyQueued

  /**
   * The date/time any quantity was started at this point (operation or top-level).
   * Can be null if the nothing is in work.
   * <p/><b>WorkStateTrait field</b>.
   */
  Date dateQtyStarted

  /**
   * The date/time any quantity was first queued at this point (operation or top-level).
   * <p/><b>WorkStateTrait field</b>.
   */
  Date dateFirstQueued

  /**
   * The date/time any quantity was first started at this point (operation or top-level).
   * <p/><b>WorkStateTrait field</b>.
   */
  Date dateFirstStarted

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE') Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  @Id @AutoPopulated UUID uuid

  /**
   * Internal constraint definitions.
   */
  static constraints = {
    qtyInQueue(scale: FieldSizes.STANDARD_DECIMAL_SCALE)
    qtyInWork(scale: FieldSizes.STANDARD_DECIMAL_SCALE)
    qtyDone(scale: FieldSizes.STANDARD_DECIMAL_SCALE)
    dateQtyQueued(nullable: true)
    dateQtyStarted(nullable: true)
    dateFirstQueued(nullable: true)
    dateFirstStarted(nullable: true)
  }

  /**
   * The empty constructor.  Used by GORM to support Map as an argument.
   */
  OrderOperState() {
    setDatesAsNeeded()
  }

  /**
   * A copy constructor to copy the operation info from another operation.
   * @param operation The routing to copy from.
   */
  OrderOperState(RoutingOperation operation) {
    ArgumentUtils.checkMissing(operation, "operation")
    this.sequence = operation.sequence
    setDatesAsNeeded()
  }

  /**
   * Determines the next workable to be performed after this workable is completed.
   * This operation state delegates to the order/routing to figure this out.
   * @return The workable.  Null if there is no other workable left to be processed.
   */
  WorkableInterface determineNextWorkable() {
    return order?.determineNextWorkable(this)
  }

  /**
   * Saves any changes to this record.
   */
  void saveChanges() {
    setLastUpdated(new Date()) // Force GORM/Hibernate to update the record
    save()
  }

  /**
   * Provides a human readable string for this object.
   * @return The string.
   */
  @Override
  String toString() {
    return RoutingUtils.combineKeyAndSequence(order?.order, sequence)
  }
}
