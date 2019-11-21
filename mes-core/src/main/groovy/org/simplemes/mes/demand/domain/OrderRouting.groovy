package org.simplemes.mes.demand.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.mes.product.domain.Routing
import org.simplemes.mes.product.domain.RoutingOperation

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
@Entity
@ToString(includeNames = true, includePackage = false, excludes = ['order'])
@EqualsAndHashCode(includes = ["order"])
class OrderRouting extends Routing {
  /**
   * The empty constructor.  Used by GORM to support Map as an argument.
   */
  OrderRouting() {}

  /**
   * A copy constructor to copy the routing info from a general routing.
   * @param routing The routing to copy from.
   */
  OrderRouting(Routing routing) {
    ArgumentUtils.checkMissing(routing, "routing")

    //TODO: this.customFields = routing.customFields plus test for the custom field copy.
    // Copy the operations
    for (fromOperation in routing.operations) {
      addToOperations(new RoutingOperation(fromOperation))
    }
  }

  /**
   * This OrderRouting always belongs to a single Order record.
   */
  Order order

}
