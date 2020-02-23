package org.simplemes.mes.demand.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.OperationTrait

import javax.persistence.Column
import javax.persistence.ManyToOne

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Defines a routing for a specific order and the status of the operations for the order.
 * A routing is a sequence of steps (operations) needed to manufacture a product.
 * These steps operations can be simple actions such as ASSEMBLE or TEST.
 * They may also be a composite operation that is made up of several actions.
 *
 */
@MappedEntity
@DomainEntity
@ToString(includeNames = true, includePackage = false, excludes = ['order'])
@EqualsAndHashCode(includes = ["order", 'sequence'])
class OrderOperation implements OperationTrait {

  /**
   * This operation belongs to the given order.
   */
  @ManyToOne
  Order order

  /**
   * Defines the sequence this operation should be performed in.  The order is relative to other operation records.
   * Zero is not allowed.
   */
  int sequence

  /**
   * The title (short description) of this operation.  This is usually visible to the production operator.
   */
  @Column(length = FieldSizes.MAX_TITLE_LENGTH, nullable = false)
  String title

  @ExtensibleFieldHolder
  @Column(length = 1024, nullable = true)
  String customFields

  @Id @AutoPopulated UUID uuid

  /**
   * The empty constructor.
   */
  OrderOperation() {}

  /**
   * A copy constructor to copy the operation info from another operation.
   * @param operation The routing to copy from.
   */
  OrderOperation(OperationTrait operation) {
    ArgumentUtils.checkMissing(operation, "operation")
    this.sequence = operation.sequence
    this.title = operation.title
    this.customFields = operation.customFields
  }

  /**
   * The primary keys this record.
   */
  @SuppressWarnings("unused")
  static keys = ['order', 'sequence']

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("unused")
  static fieldOrder = ['sequence', 'title']

  /**
   * Validates the record before save.
   * @return The list of errors.
   */
  List<ValidationError> validate() {
    return validateOperation()
  }

}
