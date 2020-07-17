package org.simplemes.mes.product

import org.simplemes.eframe.domain.validate.ValidationError

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the common behavior for all operation definition classes (e.g. MasterOperation, a ProductOperation
 * and OrderOperation routing).
 */
trait OperationTrait implements Comparable<OperationTrait> {

  /**
   * Gets the operation's title (display text).
   * @return The title.
   */
  abstract String getTitle()

  /**
   * Gets the operation's fields (JSON text).
   * @return The custom field values.
   */
  abstract String getFields()

  /**
   * Gets the operation's sequence.
   * @return The sequence.
   */
  abstract int getSequence()

  /**
   * Compare two operations.  Determines which should come first.  Uses only the sequence for this comparison.
   * @param o The other operation to compare this one too.
   * @return The compareTo value.
   */
  int compareTo(OperationTrait o) {
    return sequence <=> o.sequence
  }

  /**
   * Validates the record before save.
   * @return The list of errors.
   */
  List<ValidationError> validateOperation() {
    def res = []
    if (sequence <= 0) {
      //error.137.message=Invalid Value "{1}" for "{0}". Value should be greater than {2}.
      res << new ValidationError(137, 'sequence', sequence, 0)
    }

    return res
  }


}