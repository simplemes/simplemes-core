/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain


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
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.EnabledStatus
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum

import javax.annotation.Nullable
import javax.persistence.ManyToOne

/**
 * A test/Sample domain class with all field types as optional fields.
 * <p>
 * <b>Fields</b> Include: name, title,
 *   qty, count, enabled, dateTime, dueDate, transientField, notes, reportTimeInterval, order, status, dateCreated, lastUpdated
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ['name'])
//@CompileStatic
@SuppressWarnings("unused")
@ToString(includePackage = false, includeNames = true, excludes = ['dateCreated', 'dateUpdated'])
// TODO: Replace with non-hibernate alternative
//@ExtensibleFields(maxSize = 513, fieldName = 'anotherField')
class AllFieldsDomain {

  // ********************************************************
  // * Note: Do not change these without running all tests.
  // *       These fields are used by many tests.
  // ********************************************************

  String name // Primary key
  @Nullable String title
  @Nullable BigDecimal qty
  @Nullable Integer count
  @Nullable Boolean enabled
  @Nullable Date dateTime
  @Nullable DateOnly dueDate
  @Transient String transientField = 'Transient Default'
  @Nullable String displayOnlyText = 'Display Only'
  @Nullable String notes
  @Nullable ReportTimeIntervalEnum reportTimeInterval
  @Nullable @ManyToOne(targetEntity = Order) Order order
  @Nullable BasicStatus status = EnabledStatus.instance

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  @Id @AutoPopulated UUID uuid


  static fieldOrder = ['name', 'title', 'qty', 'count', 'enabled', 'dueDate', 'dateTime',
                       'group:details', 'notes', 'transientField', 'reportTimeInterval', 'order', 'status',
                       'displayOnlyText']

  /**
   * Load initial records.  Dummy test records.
   */
/*
  static initialDataLoad() {
    def order = new Order(order: 'ABC').save()
    def afd = new AllFieldsDomain(name:'XYZ', order:order).save()
    new AllFieldsDomain(name:'XYZ2').save()
    def afd2 = AllFieldsDomain.findByUuid(afd.uuid)
    //println "afd2 = $afd2.order"

    return null
  }
*/
/*
    def afd = findByName('B')
    afd.reportTimeInterval = ReportTimeIntervalEnum.LAST_6_MONTHS
    afd.save()
    findByName('EST')?.delete()
    findByName('EDT')?.delete()
    def sdf = new SimpleDateFormat('MM-dd HH:mm:ss zzz')
    def date = new Date(UnitTestUtils.SAMPLE_TIME_MS)
    def dueDate = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    new AllFieldsDomain(name: 'EST', dateTime: date, dueDate: dueDate, title: sdf.format(date)).save()
    date = new Date(UnitTestUtils.SAMPLE_TIME_NO_FRACTION_MS)
    new AllFieldsDomain(name: 'EDT', dateTime: date, dueDate: dueDate, title: sdf.format(date)).save()
*/

}
