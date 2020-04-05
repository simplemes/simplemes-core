package org.simplemes.mes.product.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.domain.LSNSequence
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.OperationTrait
import org.simplemes.mes.product.RoutingTrait

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.OneToMany

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Defines the a product (part) built or used within the system.
 * A product is a part or object that is produced on your shop floor or purchased from external sources.
 * This is sometimes known as a part number or model number.  This product defines the optional Bill of Material and
 * Routing needed to produce the product.
 *
 */
@MappedEntity
@DomainEntity
@SuppressWarnings("unused")
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(includes = ["product"])
class Product implements RoutingTrait {

  /**
   * The Product as known to the users.  <b>Primary Key Field</b>.
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_PRODUCT_LENGTH}.
   */
  @Column(length = FieldSizes.MAX_PRODUCT_LENGTH, nullable = false)
  String product
  // TODO: Add unique constraint, index

  /**
   * The product's title (short description).
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_TITLE_LENGTH}.
   */
  @Column(length = FieldSizes.MAX_TITLE_LENGTH, nullable = true)
  String title

  @Column(nullable = true)
  @MappedProperty(type = DataType.STRING, definition = 'TEXT') String description

  /**
   * Defines if LSNs are used with the product.  Affects how orders can use LSNs.
   */
  @Column(length = LSNTrackingOption.Sizes.COLUMN_SIZE, nullable = false)
  LSNTrackingOption lsnTrackingOption = LSNTrackingOption.LSN_ALLOWED

  /**
   * The LSN sequence to use.  If not specified, then LSNs will not be
   * created for the order on creation.  If not defined, then the default LSN Sequence will be used.
   */
  @Nullable
  LSNSequence lsnSequence

  /**
   * The lot size (size of child LSN if LSNs are used).
   */
  BigDecimal lotSize = 1.0

  /**
   * Defines a shared master routing to be used to produce this product.
   * This is ignored if the Product-level `operations` are defined.
   */
  @Nullable
  MasterRouting masterRouting

  /**
   * The operations used only by this product.  No other products will use this list of operations.
   */
  @OneToMany(mappedBy = "product")
  List<ProductOperation> operations

  /**
   * The custom field holder.  Max size: {@link FieldSizes#MAX_CUSTOM_FIELDS_LENGTH}
   */
  @ExtensibleFieldHolder
  @Column(length = FieldSizes.MAX_CUSTOM_FIELDS_LENGTH, nullable = true)
  @SuppressWarnings("unused")
  String customFields

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  Integer version = 0

  @Id @AutoPopulated UUID uuid

  /**
   * This domain is a top-level searchable element.
   */
  static searchable = true

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['product', 'title', 'lsnTrackingOption', 'lsnSequence', 'lotSize', 'description',
                       'group:routing', 'masterRouting', 'operations']

  /**
   * Determines the effective routing to use for this product.
   * @return The routing  to use.  Can be null if no routing defined.
   */
  RoutingTrait determineEffectiveRouting() {
    if (operations) {
      return this
    }
    return masterRouting
  }

  /**
   * Sets the operations.
   * @param operations The operations.
   */
  @Override
  void setOperations(List<OperationTrait> operations) {
    // This method is added due to Groovy issues with use of the trait on the list.
    //noinspection GroovyAssignabilityCheck
    this.operations = operations
  }
}
