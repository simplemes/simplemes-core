package org.simplemes.mes.product.domain

import com.fasterxml.jackson.annotation.JsonFilter
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.OperationTrait
import org.simplemes.mes.product.RoutingTrait

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.OneToMany

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Defines a routing for the production of one or more products.
 * A routing is a sequence of steps (operations) needed to manufacture a product.
 * These steps operations can be simple actions such as ASSEMBLE or TEST.
 * They may also be a composite operation that is made up of several actions.
 * <p/>
 * This sub-class is used to attach a routing to multiple products in general but not to a specific
 * product.
 *
 */
@MappedEntity
@DomainEntity
@JsonFilter("searchableFilter")
@SuppressWarnings('unused')
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(includes = ['routing'])
class MasterRouting implements RoutingTrait {
  /**
   * The Routing as known to the users.  <b>Primary Code Field</b>.
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_PRODUCT_LENGTH}.
   */
  @Column(length = FieldSizes.MAX_PRODUCT_LENGTH, nullable = false)
  String routing

  /**
   * The routing's title (short description).
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_TITLE_LENGTH}.
   */
  @Column(length = FieldSizes.MAX_TITLE_LENGTH, nullable = true)
  String title

  /**
   * The operations used only by this master routing.
   */
  @OneToMany(targetEntity = MasterOperation, mappedBy = "masterRouting")
  List<OperationTrait> operations

  /**
   * This domain is a top-level searchable element.
   */
  static searchable = true

  /**
   * The custom field holder.
   */
  @Nullable
  @ExtensibleFieldHolder
  @MappedProperty(type = DataType.JSON)
  String fields

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  Integer version = 0

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['routing', 'title', 'operations']

  /**
   * Validates the record before save.
   * @return The list of errors.
   */
  List<ValidationError> validate() {
    return validateOperations(routing)
  }

  /**
   * Sorts the operations before save.
   */
  @SuppressWarnings("unused")
  def beforeSave() {
    sortOperations()
  }
}
