package org.simplemes.mes.assy.demand.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product

import javax.persistence.ManyToOne

/**
 * Defines a single component required for a given order.  This is basically a copy of the ProductComponent
 * records defined for the product.
 * <p>
 * This element informally belongs to the MES Core Order domain.  Since the Order is in another module,
 * the normal child relationship can't be used.
 * Instead, the {@link org.simplemes.mes.assy.application.AssemblyAddition} adds this as a custom child to the core domain.
 *
 */
@DomainEntity
@MappedEntity('order_bom_component')
@ToString(includeNames = true, includePackage = false, excludes = ['order'])
@EqualsAndHashCode(includes = ['uuid'])
class OrderBOMComponent {

  /**
   * Empty constructor that copies the important fields from the given ProductComponent.
   */
  OrderBOMComponent() {
  }

  /**
   * A copy constructor that copies the important fields from the given ProductComponent.
   * @param productComponent The product component record to copy the values from.
   */
  OrderBOMComponent(ProductComponent productComponent) {
    this.sequence = productComponent.sequence
    this.component = productComponent.component
    this.qty = productComponent.qty
  }

  /**
   * The Order this component is required for.
   */
  @ManyToOne
  Order order

  /**
   * The sequence this component should be displayed in.
   * Duplicates allowed.
   */
  Integer sequence

  /**
   * This is the component (product) required for the main assembly. <b>(Required)</b>
   */
  @ManyToOne(targetEntity = Product)
  Product component

  /**
   * The number of pieces required for each assembly (<b>required</b>).
   */
  BigDecimal qty = 1.0

  @Id @AutoPopulated UUID uuid

  /**
   * The primary key(s) for this child element.
   */
  static keys = ['order', 'sequence']

  /**
   * Internal definitions.
   */
  static fieldOrder = ['sequence', 'component', 'qty']

}
