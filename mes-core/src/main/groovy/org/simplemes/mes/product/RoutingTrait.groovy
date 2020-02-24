package org.simplemes.mes.product

import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.exception.BusinessException

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the common behavior for all routings (e.g. MasterRouting, a Product and Order routing).
 * A routing is a sequence of steps (operations) needed to manufacture a product.
 * These steps operations can be simple actions such as ASSEMBLE or TEST.
 * They may also be a composite operation that is made up of several actions.
 * <p/>
 * This trait actually contains the core routing logic and elements such as operations.
 * The implementers of this class are used to attach a routing to a product in different ways.
 *
 */
trait RoutingTrait {

  /**
   * Gets the operations.
   * @return The operations.
   */
  abstract List<OperationTrait> getOperations()

  /**
   * Sets the operations.
   * @param operations The operations.
   */
  abstract void setOperations(List<OperationTrait> operations)

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
        }
        return 0
      }
    }

    // Wrong sequence, so fail.
    //error.4001.message=Operation Sequence {0} not found one routing {1}
    throw new BusinessException(4001, [sequence, toString()])
  }

  /**
   * Validates that the operations are valid (no duplicate sequences and has at least one operation).
   * @param routing The routing the operations are on (used only for error messages).
   */
  List<ValidationError> validateOperations(String routing) {
    def res = []
    // Make sure we have some operations.
    if (operations?.size() <= 0) {
      //error.4002.message=Operations missing for routing "{1}".  At least one operation is required.
      res << new ValidationError(4002, 'operations', routing)
    }

    // Make sure there are no duplicates.
    for (op in operations) {
      if (operations.count { op.sequence == it.sequence } > 1) {
        //error.4003.message=Two or more routing operations have the same sequence {1} on routing "{2}".  Each sequence must be unique.
        res << new ValidationError(4003, 'sequence', op.sequence, routing)
      }
    }
    return res
  }

  /**
   * Sorts the list of operations.  Typically used in the beforeSave() method.
   */
  def sortOperations() {
    operations?.sort()
  }

}