package org.simplemes.mes.tracking

import groovy.transform.ToString
import org.simplemes.eframe.security.domain.User
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.product.domain.MasterRouting
import org.simplemes.mes.product.domain.Product

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The request argument for the ProductionLogService log() method.
 */
@ToString(includeNames = true, includePackage = false)
class ProductionLogRequest {

  /**
   * The action performed (<b>Required</b>).
   */
  String action

  /**
   * The date/time the action took place  (<b>Required</b>).
   */
  Date dateTime = new Date()

  /**
   * The date/time the action took place  (<b>Default:</b> dateTime).
   */
  Date startDateTime = new Date()

  /**
   * The elapsed time in milliseconds for the action (<b>Default:</b> The difference from startDateTime and dateTime or 0).
   */
  Long elapsedTime = 0

  /**
   * The user who performed this action (User ID) (<b>Defaults to current request user</b>).
   */
  User user

  /**
   * The Order processed.
   */
  Order order

  /**
   * The LSN processed.
   */
  LSN lsn

  /**
   * The Product for the LSN/Order.  (<b>Default:</b> The order's product).
   */
  Product product

  /**
   * The master routing this production action took place on.
   */
  MasterRouting masterRouting

  /**
   * The routing operation sequence where this action was performed.
   */
  Integer operationSequence

  /**
   * The Work Center this action took place at.
   */
  WorkCenter workCenter

  /**
   * The quantity processed during this action (<b>Default</b>: 0.0).
   */
  BigDecimal qty = 0.0

  /**
   * The quantity started that was removed from work on this action (<b>Default</b>: 0.0).
   */
  BigDecimal qtyStarted = 0.0

  /**
   * The quantity completed by this action (<b>Default</b>: 0.0).
   */
  BigDecimal qtyCompleted = 0.0

}
