package org.simplemes.mes.tracking.domain

import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.domain.Product

import javax.annotation.Nullable
import javax.persistence.Column

/**
 * This class represents a single action by a user in the system.
 */

@MappedEntity
@DomainEntity
@ToString(includeNames = true, includePackage = false)
class ActionLog {
  /**
   * The action performed.
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String action

  /**
   * The date/time the action took place.
   */
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateTime = new Date()

  /**
   * The user who performed this action (User name) (<b>Default:</b> current user).
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String userName //= UserUtils.currentUsername

  /**
   * The Order affected.
   */
  @Nullable
  Order order

  /**
   * The LSN affected.
   */
  @Nullable
  LSN lsn

  /**
   * The quantity processed during this action.
   */
  BigDecimal qty
  // TODO: Set scale in DDL for STANDARD_DECIMAL_SCALE (4?).

  /**
   * The Product for the LSN/Order.  Determined automatically on save.
   */
  @Nullable
  Product product

  /**
   * The Work Center this action took place at.
   */
  @Nullable
  WorkCenter workCenter

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @Id @AutoPopulated UUID uuid


  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['action', 'dateTime', 'userName', 'order', 'lsn', 'qty', 'product', 'workCenter']

  /**
   * Called before validate happens.  Used to set the product if needed.
   */
  def beforeValidate() {
    product = product ?: order?.product
    userName = userName ?: SecurityUtils.currentUserName
  }

}
