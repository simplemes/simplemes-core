package org.simplemes.mes.product

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the common behavior for all operation definition classes (e.g. MasterOperation, a ProductOperation
 * and OrderOperation routing).
 */
trait OperationTrait {

  /**
   * Gets the operation's title (display text).
   * @return The title.
   */
  abstract String getTitle()

  /**
   * Gets the operation's sequence.
   * @return The sequence.
   */
  abstract int getSequence()
}