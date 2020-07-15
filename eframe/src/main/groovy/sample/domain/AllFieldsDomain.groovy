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
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.EnabledStatus
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.ManyToOne

/**
 * A test/Sample domain class with all field types as optional fields.
 * <p>
 * <b>Fields</b> Include: name, title,
 *   qty, count, enabled, dateTime, dueDate, transientField, notes, reportTimeInterval, order, status, dateCreated, dateUpdated
 * <p>
 *
 *  <h3>Sample use of all fields</h3>
 * <pre>
 *  new AllFieldsDomain(name: 'ABC', title: 'DEF', qty:1.2, count: 437, enabled: true,
 *                      dueDate: new DateOnly(), dateTime: new Date(),
 *                      reportTimeInterval:ReportTimeIntervalEnum.LAST_6_MONTHS,
 *                      status: EnabledStatus.instance).save()
 * </pre>
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ['name'])
//@CompileStatic
@SuppressWarnings("unused")
@JsonFilter('searchableFilter')
@ToString(includePackage = false, includeNames = true, excludes = ['dateCreated', 'dateUpdated'])
class AllFieldsDomain {

  // ********************************************************
  // * Note: Do not change these without running all tests.
  // *       These fields are used by many tests.
  // ********************************************************

  String name // Primary key
  @Nullable String title
  @Nullable BigDecimal qty
  @Nullable Integer count
  @Nullable int intPrimitive
  @Nullable Boolean enabled
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  @Nullable Date dateTime
  @Nullable DateOnly dueDate
  @Transient String transientField = 'Transient Default'
  @Nullable String displayOnlyText = 'Display Only'
  @Nullable String notes
  @Nullable String anotherField
  @Nullable ReportTimeIntervalEnum reportTimeInterval

  @Nullable
  @ManyToOne(targetEntity = Order)
  @MappedProperty(type = DataType.UUID)
  Order order

  @Column(length = 12, nullable = true)
  @Nullable BasicStatus status = EnabledStatus.instance

  @Nullable
  @ExtensibleFieldHolder
  @MappedProperty(type = DataType.JSON)
  String otherCustomFields


  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  int version = 0

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  // anotherField left out intentionally for custom field editor checks.  See DefinitionEditorGUISpec.
  static fieldOrder = ['name', 'title', 'qty', 'count', 'enabled', 'dueDate', 'dateTime',
                       'group:details', 'notes', 'transientField', 'reportTimeInterval', 'order', 'status',
                       'displayOnlyText']

  /**
   * A searchable top-level domain.
   */
  static searchable = {
    exclude = ['enabled']
    searchable = true
  }

  def validate() {
    def res = []
    if (count != null) {
      if (count > 999999) {
        //error.2.message=Value is too long (max={2}, length={1}) for field "{0}".
        res << new ValidationError(2, 'count', 999999, count)
      } else if (count < 0) {
        //error.136.message=Invalid Value "{1}" for {0}. Value should be greater than or equal to {2}.
        res << new ValidationError(136, 'count', count, 0)
      }
    }
    if (qty > 999999.99) {
      //error.2.message=Value is too long (max={2}, length={1}) for field "{0}".
      res << new ValidationError(2, 'qty', 999999.99, count)
    }
    return res
  }

  /**
   * Load initial records.  Dummy test records.
   */
/*
  static initialDataLoad() {
    def rng = new Random()
    for (i in (1..50)) {
      def s = String.format("%03d", i)
      def qty = 10.0 * rng.nextDouble() as BigDecimal
      def name = "XYZZY-$s"
      new AllFieldsDomain(name: name, title: "initialDataLoad()-$s", count: 51-i, qty:qty).save()
    }

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
