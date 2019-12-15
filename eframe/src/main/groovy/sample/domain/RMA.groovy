package sample.domain

//import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.annotation.ExtensibleFields

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A sample domain class that simulates an RMA approval.
 * Fields include: rma, status, product, qty, returnDate, rmaType
 */
//@Entity
// TODO: Replace with non-hibernate alternative
@ExtensibleFields
@ToString(includePackage = false, includeNames = true,
  excludes = ['dateCreated', 'lastUpdated'])
@EqualsAndHashCode(includes = ['rma'])
@SuppressWarnings("unused")
class RMA {
  String rma
  String status = 'Approved'
  String product
  BigDecimal qty = 1.0
  Date returnDate
  FlexType rmaType
  Date dateCreated
  Date lastUpdated

  /**
   * A transient list of the fields defined for this flex type.
   */
  String rmaSummary

  static constraints = {
    rma nullable: false, blank: false, maxSize: 30, unique: true
    status nullable: false, length: 20
    product nullable: true, blank: true, maxSize: 40
    returnDate nullable: true
    qty nullable: false, scale: 2
  }

  static fieldOrder = ['rma', 'status', 'product', 'qty', 'returnDate', 'rmaType']

  static transients = ['rmaSummary']

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

  /**
   * Finds all of the user roles this user has assigned and returns it as a comma-delimited list of roles (titles).
   * This reads the roles from the roles for the user and formats them for display.
   * @return The list of roles as a string.
   */
  String getRmaSummary() {
    rmaSummary = ExtensibleFieldHelper.instance.formatConfigurableTypeValues('rmaType', this)
    return rmaSummary
  }
}
