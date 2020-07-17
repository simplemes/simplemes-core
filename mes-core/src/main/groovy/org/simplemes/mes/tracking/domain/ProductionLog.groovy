package org.simplemes.mes.tracking.domain

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
import org.simplemes.mes.misc.FieldSizes

import javax.annotation.Nullable
import javax.persistence.Column

/**
 * This class represents the result of a production action on the shop floor.
 * Typically, this is written when the order/LSN is taken out of work so that the
 * elapsed time can be calculated.
 * This includes actions like complete and reverse start.
 * <p>
 * These records are designed to exist without direct references to other domain objects.  This means
 * the references use the primary key field for the object (e.g. Order, LSN, etc).  Those referenced
 * objects can be archived and these production log records can be kept in the database for a different time
 * scale.
 */

@MappedEntity
@DomainEntity
@SuppressWarnings('unused')
@ToString(includeNames = true, includePackage = false)
class ProductionLog {
  /**
   * The action performed (<b>Required</b>).
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String action

  /**
   * The date/time the action took place  (<b>Default:</b> now).
   */
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateTime = new Date()

  /**
   * The date/time the action took place  (<b>Default:</b> dateTime).
   */
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date startDateTime = new Date()

  /**
   * The elapsed time in milliseconds for the action (<b>Default:</b> The difference from startDateTime and dateTime or 0).
   */
  Long elapsedTime = 0

  /**
   * The user who performed this action (User ID) (<b>Required</b>).
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String userName

  /**
   * The Order processed.
   */
  @Column(name = 'ordr', length = FieldSizes.MAX_CODE_LENGTH, nullable = true)
  String order

  /**
   * The LSN processed.
   */
  @Column(length = FieldSizes.MAX_LSN_LENGTH, nullable = true)
  String lsn

  /**
   * The Product for the LSN/Order.  Determined automatically on save.
   */
  @Column(length = FieldSizes.MAX_PRODUCT_LENGTH, nullable = true)
  String product

  /**
   * The master routing this production action took place on.
   */
  @Column(length = FieldSizes.MAX_PRODUCT_LENGTH, nullable = true)
  String masterRouting

  /**
   * The routing operation sequence where this action was performed.
   */
  @Nullable
  Integer operationSequence = 0

  /**
   * The Work Center this action took place at.
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = true)
  String workCenter

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

  //BigDecimal qtyWithDefects
  //BigDecimal qtyScrapped

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  /**
   * The custom field holder.
   */
  @Nullable
  @ExtensibleFieldHolder
  @MappedProperty(type = DataType.JSON)
  String fields

  @Id @AutoPopulated
  @SuppressWarnings('unused')
  UUID uuid


  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['action', 'dateTime', 'order', 'lsn', 'product', 'workCenter', 'userName', 'qtyStarted', 'qtyCompleted', 'qty']

  /**
   * Called before insert happens.  Used to set the user, if needed.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  def beforeValidate() {
    userName = userName ?: SecurityUtils.currentUserName
    //println "userName = $userName"
  }

}
