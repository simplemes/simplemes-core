/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id

//import grails.gorm.annotation.Entity

import io.micronaut.data.annotation.MappedEntity
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.annotation.DomainEntity
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
@ToString(includePackage = false, includeNames = true, excludes = ['sampleParent'])
@EqualsAndHashCode(includes = ['sampleParent', 'key'])
class SampleChild {

  // ********************************************************
  // * Note: Do not change these without running all tests.
  // *       These fields are used by many tests.
  // ********************************************************

  @ManyToOne
  SampleParent sampleParent

  @Column(name = 'key_value', length = 30)
  String key
  @Nullable Integer sequence = 10
  @Nullable String title
  @Nullable BasicFieldFormat format
  @Nullable BigDecimal qty
  @Nullable Boolean enabled
  @Nullable Date dateTime
  @Nullable DateOnly dueDate
  @Nullable ReportTimeIntervalEnum reportTimeInterval
  @Nullable Order order

  /**
   * A list of grand children.
   */
  @OneToMany(mappedBy = "sampleChild")
  List<SampleGrandChild> sampleGrandChildren

  @Id @AutoPopulated UUID uuid

  @SuppressWarnings("unused")
  static fieldOrder = ['key', 'sequence', 'title', 'qty', 'enabled', 'dueDate', 'dateTime', 'format',
                       'reportTimeInterval', 'order']

}

