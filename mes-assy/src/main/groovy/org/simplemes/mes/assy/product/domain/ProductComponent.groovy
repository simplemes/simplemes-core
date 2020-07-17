package org.simplemes.mes.assy.product.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.mes.product.domain.Product

import javax.persistence.ManyToOne

/**
 * Defines a single component required for a given product.
 * <p>
 * This element informally belongs to the MES Core Product domain.  Since the Product is in another module,
 * the normal child relationship is not used.
 * Instead, the {@link org.simplemes.mes.assy.application.AssemblyAddition} adds this as a custom child to the core Product domain.
 * <p>
 * <b>Note:</b> On Test cleanup, you should delete these ProductComponent records before deleting the Product records.
 *
 */
@DomainEntity
@MappedEntity
@SuppressWarnings("unused")
@ToString(includeNames = true, includePackage = false, excludes = ['product'])
@EqualsAndHashCode(includes = ['uuid'])
class ProductComponent {

  /**
   * The Product (assembly) this component is required for.  
   */
  @ManyToOne
  @MappedProperty(type = DataType.UUID)
  Product product

  /**
   * The sequence this component should be displayed in.
   * Duplicates are not allowed.  Will be auto-assigned upon save.
   */
  Integer sequence = 10

  /**
   * This is the product required for the main assembly. <b>(Required)</b>
   */
  @ManyToOne(targetEntity = Product)
  @MappedProperty(type = DataType.UUID)
  Product component

  /**
   * The number of pieces required for each assembly (<b>required</b>).
   */
  BigDecimal qty = 1.0


  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  /**
   * The primary key(s) for this child element.
   */
  static keys = ['product', 'sequence']

  /**
   * Internal definitions.
   */
  static fieldOrder = ['sequence', 'component', 'qty']

  /**
   * Returns the short-format of this object.
   * @return The short format of this object.
   */
  String toShortString() {
    return "$sequence: ${TypeUtils.toShortString(component, true)}"
  }

  /**
   * Build a human-readable version of this object, localized.
   * Simply uses toShortString().
   * @param locale The locale to display the enum display text.
   * @return The human-readable string.
   */
  String toStringLocalized(Locale locale = null) {
    return toShortString()
  }

}
