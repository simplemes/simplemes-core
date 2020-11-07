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
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

/**
 * A test/Sample child domain class.
 * <p>
 * <b>Fields</b> Include: key, title, sequence, format,
 *               qty, enabled, dateTime, dueDate, reportTimeInterval,sampleGrandChildren
 *
 */
@MappedEntity
@DomainEntity
@SuppressWarnings("unused")
@ToString(includePackage = false, includeNames = true, excludes = ['sampleParent'])
@EqualsAndHashCode(includes = ['sampleParent', 'key'])
class SampleChild implements Comparable {

  // ********************************************************
  // * Note: Do not change these without running all tests.
  // *       These fields are used by many tests.
  // ********************************************************

  @ManyToOne
  @MappedProperty(type = DataType.UUID)
  SampleParent sampleParent

  @Column(name = 'key_value', length = 30, nullable = false)
  String key
  @Nullable Integer sequence = 10
  @Nullable String title
  @Nullable BasicFieldFormat format
  @Nullable BigDecimal qty
  @Nullable Boolean enabled
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  @Nullable Date dateTime
  @Nullable DateOnly dueDate
  @Nullable ReportTimeIntervalEnum reportTimeInterval

  @Nullable
  @ManyToOne(targetEntity = Order)
  @MappedProperty(type = DataType.UUID)
  Order order

  /**
   * A list of grand children.
   */
  @OneToMany(mappedBy = "sampleChild")
  List<SampleGrandChild> sampleGrandChildren

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

/**
 * A searchable top-level domain.
 */
  static searchable = [parent: SampleParent]

  static fieldOrder = ['key', 'sequence', 'title', 'qty', 'enabled', 'dueDate', 'dateTime', 'format',
                       'reportTimeInterval', 'order']

  /**
   * Load initial records.  Does nothing.
   */
  static initialDataLoad() {
    return null
  }

  def validate() {
    if (key == 'xyzzy') {
      // Test validation
      //error.98.message=Invalid Value {0}. {1} should be {2}.
      return new ValidationError(98, 'key', 'key', "!=xyzzy")
    }
    return null
  }

  /**
   * Compares this object with the specified object for order.  Returns a
   * negative integer, zero, or a positive integer as this object is less
   * than, equal to, or greater than the specified object.
   *
   * @param o the object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   *          is less than, equal to, or greater than the specified object.
   *
   */

  @Override
  int compareTo(Object o) {
    return this.key <=> o.key
  }
}

