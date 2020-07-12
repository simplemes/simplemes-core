/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity

import javax.annotation.Nullable
import javax.persistence.ManyToOne

/**
 * A sample domain class that simulates an order line item.
 * Fields include: order, sequence, qty, product, notes
 */
@DomainEntity
@MappedEntity()
@ToString(includeNames = true, excludes = ['order'])
@EqualsAndHashCode(includes = ['uuid'])
@SuppressWarnings("unused")
class OrderLine implements Comparable {
  @ManyToOne
  @MappedProperty(type = DataType.UUID)
  Order order

  Integer sequence = 1

  BigDecimal qty = 1.0
  @Nullable String product
  @Nullable String notes

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  static fieldOrder = ['sequence', 'product', 'qty', 'notes']
  static keys = ['order', 'sequence']

  OrderLine() {
  }

  /**
   * Compares this object with the specified object for order.  Returns a
   * negative integer, zero, or a positive integer as this object is less
   * than, equal to, or greater than the specified object.
   *
   * @param o the object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   *          is less than, equal to, or greater than the specified object.
   */
  @Override
  int compareTo(Object o) {
    return this.sequence <=> o.sequence
  }
}