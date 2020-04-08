package org.simplemes.mes.assy.product.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import org.simplemes.eframe.domain.annotation.DomainEntity
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
@ToString(includeNames = true, includePackage = false, excludes = ['product'])
@EqualsAndHashCode(includes = ['uuid'])
class ProductComponent {

  /**
   * The Product (assembly) this component is required for.  
   */
  @ManyToOne
  Product product

  /**
   * The sequence this component should be displayed in.
   * Duplicates are not allowed.  Will be auto-assigned upon save.
   */
  Integer sequence = 10

  /**
   * This is the product required for the main assembly. <b>(Required)</b>
   */
  Product component

  /**
   * The number of pieces required for each assembly (<b>required</b>).
   */
  BigDecimal qty = 1.0


  @Id @AutoPopulated UUID uuid

  /**
   * The primary key(s) for this child element.
   */
  static keys = ['product', 'sequence']

  /**
   * Internal definitions.
   */
  static fieldOrder = ['sequence', 'component', 'qty']

}
