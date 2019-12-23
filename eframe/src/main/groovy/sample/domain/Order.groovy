package sample.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated

//import grails.gorm.annotation.Entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.annotation.DomainEntity

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.OneToMany

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A sample domain class that simulates an order.
 * Fields include: order, qtyToBuild, product, status, dueDate
 */
//@ExtensibleFields
@MappedEntity('ordr')
@DomainEntity
@ToString(includeNames = true)
@EqualsAndHashCode(includes = ['uuid'])
//@CompileStatic
class Order {
  @Column(name = 'ordr', length = 30)
  String order
  BigDecimal qtyToBuild = 1.0

  @Nullable String product

/*
  @MappedProperty(type = DataType.STRING)
  BasicStatus status = EnabledStatus.instance
*/

  @Nullable
  //@MappedProperty(type = DataType.DATE)
  DateOnly dueDate = new DateOnly()

  Integer version = 0

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  @OneToMany(mappedBy = "order")
  List<OrderLine> orderLines

  @Id @AutoPopulated UUID uuid

  Order() {
  }

  Order(String order) {
    this.order = order
  }

  Order(Map options) {
    options.each { k, v ->
      //noinspection GroovyAssignabilityCheck
      this[k] = v
    }

  }

  Order(String order, UUID uuid) {
    this.order = order
    this.uuid = uuid
  }

  static fieldOrder = ['order', 'product', 'qtyToBuild', 'status', 'dueDate']

  /**
   * Load initial records - test data.
   */
/*
  @SuppressWarnings("UnnecessaryQualifiedReference")
  static Map<String, List<String>> initialDataLoad() {
    def products = ['BIKE-24','BIKE-27','SEAT','WHEEL','FRAME-24','FRAME-27']
    def statuses = [EnabledStatus.instance, DisabledStatus.instance]
    Order.withTransaction {
      if (Order.list().size()==0) {
        def random = new Random()
        for (i in 1901..1902) {
          def date = new DateOnly(new DateOnly().time-DateUtils.MILLIS_PER_DAY* (300-random.nextInt(300)))
          new Order(order: "M$i",
            product: products[random.nextInt(products.size())],
            // TODO: Restore status: statuses[random.nextInt(statuses.size())],
            dueDate: date,
            qtyToBuild: new BigDecimal(random.nextInt(99)+1)
          ).save()
        }
      }
      //println "order.list() = ${Order.list()*.dateCreated}"
    }
    return null
  }

*/

}
