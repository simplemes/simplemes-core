/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import com.fasterxml.jackson.annotation.JsonFilter
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Transient
import io.micronaut.data.model.DataType
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntity

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.ManyToOne

/**
 * A sample domain class that simulates an RMA approval.
 * Fields include: rma, status, product, qty, returnDate, rmaType
 */
@MappedEntity
@DomainEntity
@JsonFilter("searchableFilter")
@ToString(includePackage = false, includeNames = true, excludes = ['dateCreated', 'dateUpdated'])
@EqualsAndHashCode(includes = ['rma'])
@SuppressWarnings("unused")
class RMA {
  String rma
  String status = 'Approved'
  @Nullable String product
  BigDecimal qty = 1.0
  @Nullable Date returnDate

  @Nullable
  @ManyToOne(targetEntity = FlexType)
  @MappedProperty(type = DataType.UUID)
  FlexType rmaType

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  int version = 0

  @ExtensibleFieldHolder
  @Column(nullable = true, length = 255)
  String customFields


  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  /**
   * A transient list of the fields defined for this flex type.
   */
  @Transient String rmaSummary

  static fieldOrder = ['rma', 'status', 'product', 'qty', 'returnDate', 'rmaType']

  /**
   * Non-Searchable domain for Search tests (SearchHelperSpec+).
   */
  static searchable = false

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
