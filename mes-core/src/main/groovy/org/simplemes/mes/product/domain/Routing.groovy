package org.simplemes.mes.product.domain

import grails.gorm.annotation.Entity
import groovy.transform.ToString
import org.simplemes.eframe.exception.BusinessException

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

// Most functional tests are under MasterRoutingTests since this class is abstract.

/**
 * Defines a routing for the production of some product.
 * A routing is a sequence of steps (operations) needed to manufacture a product.
 * These steps operations can be simple actions such as ASSEMBLE or TEST.
 * They may also be a composite operation that is made up of several actions.
 * <p/>
 * This class actually contains the core routing logic and elements such as operations.
 * The sub-classes of this class are used to attach a routing to a product in different ways.
 *
 */
@Entity
@ToString(includeNames = true, includePackage = false)
class Routing {

  /**
   * This is the list of operations to be performed on this routing.  This list is automatically sorted on the sequence.
   */
  List<RoutingOperation> operations
  // This duplicate definition is needed since the normal hasMany injection uses a Set.  A List is easier to use.

  /**
   * This operations are a list of RoutingOperation entries to be performed to produce this product.
   */
  @SuppressWarnings("unused")
  static hasMany = [operations: RoutingOperation]

  /**
   * Internal values.
   */
  static constraints = {
    operations(validator: { val, obj -> checkOperations(val, obj) })
  }

  /**
   * Validates that the operations are valid (no duplicate sequences and has at least one operation).
   * @param routingOperations The operations.
   * @param obj The routing object.
   */
  @SuppressWarnings("unused")
  static checkOperations(List<RoutingOperation> routingOperations, Routing routing) {
    //println "routingOperations = $routingOperations"
    // Make sure we have some operations.
    if (routingOperations?.size() <= 0) {
      //routing.operations.noOperations.error=Operations missing for routing.  At least one operation is required.
      return ['noOperations.error']
    }

    // Make sure there are no duplicates.
    for (op in routingOperations) {
      if (routingOperations.count { op.sequence == it.sequence } > 1) {
        //println "FAILED at op = $op"
        return ['duplicate.error', op.sequence]
      }

    }
    return true
  }

  /**
   * Determines the next operation to be performed after the given operation is completed.
   * @param sequence The operation sequence to find the next operation for.
   * @return The sequence of the next operation.
   */
  int determineNextOperation(int sequence) {
    for (int i = 0; i < operations?.size(); i++) {
      if (operations[i].sequence == sequence) {
        if ((i + 1) < operations.size()) {
          return operations[i + 1].sequence
        } else {
          return 0
        }
      }
    }

    // Wrong sequence, so fail.
    //error.4001.message=Operation Sequence {0} not found one routing {1}
    throw new BusinessException(4001, [sequence, toString()])
  }

  /**
   * Called before validate happens.  Used to sort the list of operations.
   */
  @SuppressWarnings("unused")
  def beforeValidate() {
    operations?.sort()
    //println "operations = $operations"
  }
}
