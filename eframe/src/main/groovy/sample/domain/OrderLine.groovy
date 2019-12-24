package sample.domain

//import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient
import org.simplemes.eframe.domain.annotation.DomainEntity

import javax.annotation.Nullable
import javax.persistence.ManyToOne

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A sample domain class that simulates an order line item.
 * Fields include: order, sequence, qty, product, notes
 */
@DomainEntity
@MappedEntity()
@ToString(includeNames = true, excludes = ['order'])
@EqualsAndHashCode(includes = ['uuid'])
@SuppressWarnings("unused")
class OrderLine {
  @ManyToOne
  Order order

  Integer sequence = 1

  BigDecimal qty = 1.0
  @Nullable String product
  @Nullable String notes

  @Id @AutoPopulated UUID uuid

  @Transient
  UUID _order_id = UUID.randomUUID()

  static fieldOrder = ['sequence', 'product', 'qty', 'notes']
  static keys = ['orderId', 'sequence']

  OrderLine() {
  }

}