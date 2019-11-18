package sample.domain

import grails.gorm.annotation.Entity
import groovy.transform.ToString
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A test/Sample child domain class.
 * <p>
 * <b>Fields</b> Include: key, title, sequence, format,
 *               qty, enabled, dateTime, dueDate, reportTimeInterval,sampleGrandChildren
 *
 */
@Entity
@ToString(includePackage = false, includeNames = true, excludes = ['errors', 'dirtyPropertyNames', 'attached', 'dirty', 'sampleParent'])
class SampleChild {

  // ********************************************************
  // * Note: Do not change these without running all tests.
  // *       These fields are used by many tests.
  // ********************************************************

  static belongsTo = [sampleParent: SampleParent]
  String key
  Integer sequence = 10
  String title
  BasicFieldFormat format
  BigDecimal qty
  Boolean enabled
  Date dateTime
  DateOnly dueDate
  ReportTimeIntervalEnum reportTimeInterval
  Order order

  /**
   * A list of grand children.
   */
  List<SampleGrandChild> sampleGrandChildren = []
  static hasMany = [sampleGrandChildren: SampleGrandChild]

  static constraints = {
    key nullable: false, blank: false, maxSize: 40
    sequence nullable: true
    title nullable: true, blank: true, maxSize: 20
    format nullable: true, length: 2
    qty nullable: true
    enabled nullable: true
    dateTime nullable: true
    dueDate nullable: true
    reportTimeInterval nullable: true
    order nullable: true
  }

  /**
   * Internal mappings.   
   */
  static mapping = {
    key column: 'key_value'
  }

  static fieldOrder = ['key', 'sequence', 'title', 'qty', 'enabled', 'dueDate', 'dateTime', 'format',
                       'reportTimeInterval', 'order']

}

