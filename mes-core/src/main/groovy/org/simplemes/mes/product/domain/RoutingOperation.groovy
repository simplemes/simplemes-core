package org.simplemes.mes.product.domain

import groovy.transform.EqualsAndHashCode
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.mes.misc.FieldSizes

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * This defines a single operation performed to produce a product.  These are normally grouped together in
 * a {@link org.simplemes.mes.product.domain.Routing} to be performed in a specific sequence.  Operations can be assigned
 * to be worked in a given Work Center, but that is optional.
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode
// TODO: Restore @ToString(includeNames = true, includePackage = false, excludes = ['routing'])
class RoutingOperation implements Comparable {

  @Id @AutoPopulated UUID uuid

  /**
   * The empty constructor.  Used by GORM to support Map as an argument.
   */
  RoutingOperation() {}

  /**
   * A copy constructor to copy the operation info from another operation.
   * @param operation The routing to copy from.
   */
  RoutingOperation(RoutingOperation operation) {
    ArgumentUtils.checkMissing(operation, "operation")
    this.sequence = operation.sequence
    this.title = operation.title
    //TODO: this.customFields = operation.customFields
  }

  /**
   * Defines the sequence this operation should be performed in.  The order is relative to other operation records.
   * Zero is not allowed.
   */
  int sequence

  /**
   * The title (short description) of this operation.  This is usually visible to the production operator.
   */
  String title

  /**
   * This operation always belongs to a single Routing record.
   */
  static belongsTo = [routing: Routing]

  /**
   * Internal constraints.
   */
  static constraints = {
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, nullable: false, blank: false)
    sequence(min: 1)
  }

  /**
   * The primary keys for this record are routing and sequence.  Routing is the parent key.
   */
  static keys = ['sequence']

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['sequence', 'title']

  /**
   * Compare two operations.  Determines which should come first.  Uses only the sequence for this comparison.
   * @param o The other operation to compare this one too.
   * @return The compareTo value.
   */
  int compareTo(Object o) {
    return sequence <=> o.sequence
  }

}
