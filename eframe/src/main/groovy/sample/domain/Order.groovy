package sample.domain

//import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.data.annotation.ExtensibleFields
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.EnabledStatus

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A sample domain class that simulates an order.
 * Fields include: order, qtyToBuild, product, status, dueDate
 */
//@Entity
@ExtensibleFields
@ToString(includePackage = false, includeNames = true, excludes = ['dateCreated', 'lastUpdated'])
@EqualsAndHashCode(includes = ['order'])
class Order {
  String order
  BigDecimal qtyToBuild = 1.0
  String product
  BasicStatus status = EnabledStatus.instance
  DateOnly dueDate
  Date dateCreated
  Date lastUpdated

  static constraints = {
    order nullable: false, blank: false, maxSize: 30, unique: true
    status nullable: false, length: 20
    product nullable: true, blank: true, maxSize: 40
    dueDate nullable: true
  }

  static fieldOrder = ['order', 'product', 'qtyToBuild', 'status', 'dueDate']

  static mapping = {
    table 'ordr'           // ORDER is not a legal table or column name, so use ORDR
    order column: 'ordr', index: 'Ordr_Idx'
    autoTimestamp true
  }

  /**
   * Load initial records - test data.
   */
/*
  @SuppressWarnings("UnnecessaryQualifiedReference")
  static Map<String, List<String>> initialDataLoad() {
    def products = ['BIKE-24','BIKE-27','SEAT','WHEEL','FRAME-24','FRAME-27']
    def statuses = [EnabledStatus.instance, DisabledStatus.instance]
    for (i in 1901..2101) {
      def random = new Random()
      def date2 = new DateOnly(new DateOnly().time - DateUtils.MILLIS_PER_DAY * (300 - random.nextInt(300)))
      //println "date2 = $date2"
    }
    Order.withTransaction {
      if (Order.list().size()==0) {
        def random = new Random()
        for (i in 1901..2101) {
          def date = new DateOnly(new DateOnly().time-DateUtils.MILLIS_PER_DAY* (300-random.nextInt(300)))
          new Order(order: "M$i",
            product: products[random.nextInt(products.size())],
            status: statuses[random.nextInt(statuses.size())],
            dueDate: date,
            qtyToBuild: new BigDecimal(random.nextInt(99)+1)
          ).save()
        }
      }
    }
    return null
  }

*/

  @Override
  String toString() {
    return order
  }
}
