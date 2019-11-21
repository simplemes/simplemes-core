package org.simplemes.mes.product.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.domain.LSNSequence
import org.simplemes.mes.misc.FieldSizes

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
@Entity
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(includes = ["product"])
class Product {

  /**
   * The Product as known to the users.  <b>Primary Key Field</b>.
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_PRODUCT_LENGTH}.
   */
  String product

  /**
   * The product's title (short description).
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_TITLE_LENGTH}.
   */
  String title

  /**
   * Defines if LSNs are used with the product.  Affects how orders can use LSNs.
   */
  LSNTrackingOption lsnTrackingOption = LSNTrackingOption.LSN_ALLOWED

  /**
   * The LSN sequence to use.  If not specified, then LSNs will not be
   * created for the order on creation.  If not defined, then the default LSN Sequence will be used.
   */
  LSNSequence lsnSequence

  /**
   * The lot size (size of child LSN if LSNs are used).
   */
  BigDecimal lotSize = 1.0

  /**
   * Defines a shared master routing to be used to produce this product.  If the productRouting is non-null,
   * then this master routing is generally ignored.
   */
  MasterRouting masterRouting

  /**
   * The date this record was last updated.
   */
  Date lastUpdated

  /**
   * The date this record was created
   */
  Date dateCreated

  /**
   * This productRouting is the routing used only by this product.  No other products will use this routing.
   */
  static hasOne = [productRouting: ProductRouting]

  /**
   * This domain is a top-level searchable element.
   */
  static searchable = true

  /**
   * Internal field constraints.
   */
  static constraints = {
    product(maxSize: FieldSizes.MAX_PRODUCT_LENGTH, unique: true, nullable: false, blank: false)
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, nullable: true)
    // TODO: Support maxSize lsnTrackingOption(maxSize: LSNTrackingOption.COLUMN_SIZE)
    lsnSequence(nullable: true)
    masterRouting(nullable: true)
    productRouting(nullable: true)
  }

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['product', 'title', 'lsnTrackingOption', 'lsnSequence', 'lotSize',
                       'group:routing', 'masterRouting', 'productRouting.operations']

  /**
   * Internal transient field list.
   */
  static transients = ['workAround5']

  /**
   * Work around for gap 5 to prevent stack overflow.  See below.
   */
  private transient boolean workAround5 = false

  /**
   * GORM Event handler.
   */
  def beforeValidate() {
    /*
    GORM work around 5.  A simple hasOne child of a domain is not validated by the cascading save() validation.
    The fix is to perform the validation below.  The alternative is to change to the 'belongsTo' notation.  This has a bug
    https://github.com/grails/grails-data-mapping/issues/874.  It is not clear if this will be fixed.  Also, this forces the
    foreign key into the parent table.  This is not preferred.  See Guidelines for Domains for more details.
    */
    // This work around is needed for some unit and integration tests (ControllerUtilsSpec and ControllerUtilsIntSpec)
    //println "SamplePanelDomain.beforeValidate()"

    // This additional workAround5 is used to make sure the the validate() methods don't recurse infinitely.
    // This will only let the method call the child's validate() method once per iteration.
    // TODO: Verify workaround 5 is still needed
    if (workAround5) {
      return
    }
    workAround5 = true
    productRouting?.product = this
    productRouting?.validate()
    workAround5 = false
  }


  /**
   * Determines the effective routing to use for this product.
   * @return The routing  to use.  Can be null if no routing defined.
   */
  Routing determineEffectiveRouting() {
    if (productRouting) {
      return productRouting
    } else {
      return masterRouting
    }
  }

  /**
   * Sets the routing.  Used to fix the reference ProductRouting.product in unit tests/
   * @param productRouting The routing.
   */
  void setProductRouting(ProductRouting productRouting) {
    this.productRouting = productRouting
    productRouting?.product = this
  }

}
