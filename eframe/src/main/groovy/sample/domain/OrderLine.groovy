package sample.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A sample domain class that simulates an order.
 * Fields include: orderId, sequence,qty,product, notes
 */
@Entity
@ToString(includePackage = false, includeNames = true, excludes = ['errors', 'attached', 'dirtyPropertyNames'])
@EqualsAndHashCode(includes = ['orderId', 'sequence'])
class OrderLine {
  Long orderId
  Integer sequence = 1
  BigDecimal qty = 1.0
  String product
  String notes

  static constraints = {
    product nullable: false, blank: false, maxSize: 40
    notes nullable: true, blank: true, maxSize: 40
  }

  static fieldOrder = ['sequence', 'product', 'qty', 'notes']

  static keys = ['orderId', 'sequence']

}
