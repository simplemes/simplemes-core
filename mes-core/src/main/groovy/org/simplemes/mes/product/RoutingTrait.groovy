package org.simplemes.mes.product

import org.simplemes.eframe.exception.BusinessException

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the common behavior for all routings (e.g. MasterRouting, a Product and Order routing).
 */
trait RoutingTrait {

  /**
   * Gets the operations.
   * @return The operations.
   */
  abstract List<OperationTrait> getOperations()

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


}