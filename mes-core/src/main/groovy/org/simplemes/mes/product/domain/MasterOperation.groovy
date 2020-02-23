package org.simplemes.mes.product.domain

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
 * This defines a single operation performed to produce a product.  These are normally grouped together in
 * a {@link org.simplemes.mes.product.RoutingTrait} to be performed in a specific sequence.  Operations can be assigned
 * to be worked in a given Work Center, but that is optional.
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode
@ToString(includeNames = true, includePackage = false, excludes = ['masterRouting'])
class MasterOperation implements OperationTrait {

  /**
   * This operation belongs to the given master routing.
   */
  @ManyToOne
  MasterRouting masterRouting

  /**
   * Defines the sequence this operation should be performed in.  The order is relative to other operation records.
   * Value must be greater than 0.
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
   * The empty constructor.  Used by GORM to support Map as an argument.
   */
  MasterOperation() {}

  /**
   * A copy constructor to copy the operation info from another operation.
   * @param operation The routing to copy from.
   */
  MasterOperation(MasterOperation operation) {
    ArgumentUtils.checkMissing(operation, "operation")
    this.sequence = operation.sequence
    this.title = operation.title
    this.customFields = operation.customFields
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
   * Validates the record before save.
   * @return The list of errors.
   */
  List<ValidationError> validate() {
    return validateOperation()
  }

}
