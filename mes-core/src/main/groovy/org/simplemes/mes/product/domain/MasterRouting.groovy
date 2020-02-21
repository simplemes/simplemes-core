package org.simplemes.mes.product.domain

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
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.OperationTrait
import org.simplemes.mes.product.RoutingTrait

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
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(includes = ['routing'])
class MasterRouting extends Routing implements RoutingTrait {
  /**
   * The Routing as known to the users.  <b>Primary Code Field</b>.
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_PRODUCT_LENGTH}.
   */
  String routing

  /**
   * The routing's title (short description).
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_TITLE_LENGTH}.
   */
  String title

  /**
   * The operations used only by this product.  No other products will use this list of operations.
   */
  @OneToMany(mappedBy = "product")
  List<OperationTrait> operations

  /**
   * This domain is a top-level searchable element.
   */
  static searchable = true

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  Integer version = 0

  @Id @AutoPopulated UUID uuid

  /**
   * Internal values.
   */
  static constraints = {
    routing(maxSize: FieldSizes.MAX_PRODUCT_LENGTH, unique: true, nullable: false, blank: false)
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, nullable: true)
  }

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['routing', 'title', 'operations']

}
