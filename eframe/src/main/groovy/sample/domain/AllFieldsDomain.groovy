package sample.domain

//import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.data.annotation.ExtensibleFields
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.EnabledStatus
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A test/Sample domain class with all field types as optional fields.
 * <p>
 * <b>Fields</b> Include: name, title,
 *   qty, count, enabled, dateTime, dueDate, transientField, notes, reportTimeInterval, order, status, dateCreated, lastUpdated
 */
//@Entity
@ToString(includePackage = false, includeNames = true, excludes = ['dateCreated', 'lastUpdated'])
@EqualsAndHashCode
// TODO: Replace with non-hibernate alternative
@ExtensibleFields(maxSize = 513, fieldName = 'anotherField')
//@JsonIgnoreProperties(['hibernateLazyInitializer','handler', 'readOnly', 'dirty'])
class AllFieldsDomain {

  // ********************************************************
  // * Note: Do not change these without running all tests.
  // *       These fields are used by many tests.
  // ********************************************************

  String name // Primary key
  String title
  BigDecimal qty
  Integer count
  Boolean enabled
  Date dateTime
  DateOnly dueDate
  String transientField = 'Transient Default'
  String displayOnlyText = 'Display Only'
  String notes
  ReportTimeIntervalEnum reportTimeInterval
  Order order
  BasicStatus status = EnabledStatus.instance

  Date dateCreated
  Date lastUpdated

  static constraints = {
    name nullable: false, blank: false, maxSize: 40, unique: true
    title nullable: true, blank: true, maxSize: 20
    qty nullable: true, max: 999999.99, min: 0.0, scale: 4
    count nullable: true, max: 999999, min: 0
    enabled nullable: true
    dateTime nullable: true
    dueDate nullable: true
    notes nullable: true, blank: true
    reportTimeInterval nullable: true
    order nullable: true
    status nullable: true, length: 8
    displayOnlyText nullable: true, length: 20
  }

  static fieldOrder = ['name', 'title', 'qty', 'count', 'enabled', 'dueDate', 'dateTime',
                       'group:details', 'notes', 'transientField', 'reportTimeInterval', 'order', 'status',
                       'displayOnlyText']

  static mapping = {
    //dueDate type: DateOnlyType
    //status type: EncodedType, length: 8
  }

  static transients = ['transientField']


/*
  void setDueDate(java.sql.Date dueDate) {
    this.dueDate = new DateOnly(dueDate.time)
  }
*/
  /**
   * Load initial records.  Dummy test records.
   */
/*
  static initialDataLoad() {
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
  }
*/

}
