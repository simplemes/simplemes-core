package org.simplemes.mes.product.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import org.simplemes.eframe.domain.annotation.DomainEntity

import javax.persistence.OneToMany

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Defines a routing for the production of one specific product.
 * A routing is a sequence of steps (operations) needed to manufacture a product.
 * These steps operations can be simple actions such as ASSEMBLE or TEST.
 * They may also be a composite operation that is made up of several actions.
 * <p/>
 * This sub-class is used to attach a routing to a specific product.
 * This is a child of the product and can only be imported as part of the product.
 *
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ["product"])
@ToString(includeNames = true, includePackage = false, excludes = ['product'])
class ProductRouting extends Routing {

  /**
   * This ProductRouting  always belongs to a single Product record.
   */
  Product product

  /**
   * This is the list of operations to be performed on this routing.  This list is automatically sorted on the sequence.
   */
  @OneToMany(mappedBy = "routing")
  List<MasterOperation> operations
  // This duplicate definition is needed since the normal hasMany injection uses a Set.  A List is easier to use.

  @Id @AutoPopulated UUID uuid

  /**
   * Internal values.
   */
  static constraints = {
    //operations(validator: { val, obj -> checkOperations(val, obj) })
  }

}
