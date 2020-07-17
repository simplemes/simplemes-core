package org.simplemes.mes.tracking.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.domain.Product

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.ManyToOne

/**
 * This class represents a single action by a user in the system.
 */

@MappedEntity
@DomainEntity
@SuppressWarnings("unused")
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(includes = ['uuid'])
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
  @ManyToOne(targetEntity = Order)
  @MappedProperty(type = DataType.UUID)
  Order order

  /**
   * The LSN affected.
   */
  @Nullable
  @ManyToOne(targetEntity = LSN)
  @MappedProperty(type = DataType.UUID)
  LSN lsn

  /**
   * The quantity processed during this action.
   */
  @Nullable
  BigDecimal qty

  /**
   * The Product for the LSN/Order.  Determined automatically on save.
   */
  @Nullable
  @ManyToOne(targetEntity = Product)
  @MappedProperty(type = DataType.UUID)
  Product product

  /**
   * The Work Center this action took place at.
   */
  @Nullable
  @ManyToOne(targetEntity = WorkCenter)
  @MappedProperty(type = DataType.UUID)
  WorkCenter workCenter

  /**
   * The custom field holder.
   */
  @Nullable
  @ExtensibleFieldHolder
  @MappedProperty(type = DataType.JSON)
  String fields

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid


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
