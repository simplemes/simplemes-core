package org.simplemes.mes.tracking.domain

import grails.gorm.annotation.Entity
import groovy.transform.ToString
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.domain.Product

/**
 * This class represents a single action by a user in the system.
 */

@Entity
@ToString(includeNames = true, includePackage = false)
class ActionLog {
  /**
   * The action performed.
   */
  String action

  /**
   * The date/time the action took place.
   */
  Date dateTime = new Date()

  /**
   * The user who performed this action (User name) (<b>Default:</b> current user).
   */
  String userName //= UserUtils.currentUsername

  /**
   * The Order affected.
   */
  Order order

  /**
   * The LSN affected.
   */
  LSN lsn

  /**
   * The quantity processed during this action.
   */
  BigDecimal qty

  /**
   * The Product for the LSN/Order.  Determined automatically on save.
   */
  Product product

  /**
   * The Work Center this action took place at.
   */
  WorkCenter workCenter

  /**
   * The date this record was created
   */
  Date dateCreated

  static constraints = {
    action(maxSize: FieldSizes.MAX_CODE_LENGTH)
    userName(maxSize: FieldSizes.MAX_CODE_LENGTH, nullable: false, blank: false)
    lsn(nullable: true)
    order(nullable: true)
    product(nullable: true)
    workCenter(nullable: true)
    qty(nullable: true, scale: FieldSizes.STANDARD_DECIMAL_SCALE)
  }

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['action', 'dateTime', 'userName', 'order', 'lsn', 'qty', 'product', 'workCenter']

  /**
   * Called before insert happens.  Used to set the product if needed.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  def beforeValidate = {
    product = product ?: order?.product
    userName = userName ?: SecurityUtils.currentUserName
  }

}
