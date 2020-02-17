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
import io.micronaut.data.model.DataType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntity

import javax.annotation.Nullable
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

//import grails.gorm.annotation.Entity

/**
 * A test/Sample parent domain class.
 * Has child and foreign references.
 * <p>
 * <b>Fields</b> Include: name, title, notes, notDisplayed, moreNotes. allFieldsDomains, allFieldDomain, sampleChildren,
 * dateCreated, lastUpdated
 */
@MappedEntity
@DomainEntity
@ToString(includePackage = false, includeNames = true, includes = ['name', 'title', 'notes', 'notDisplayed',
  'moreNotes', 'dateCreated', 'dateUpdated', 'allFieldsDomain', 'allFieldsDomains', 'sampleChildren', 'uuid'])
@EqualsAndHashCode(includes = ['name'])
@SuppressWarnings("unused")
class SampleParent implements SampleParentInterface {

  // ********************************************************
  // * Note: Do not change these without running all tests.
  // *       These fields are used by many tests.
  // ********************************************************

  @Column(length = 40, nullable = false)
  String name

  @Column(length = 20, nullable = true)
  String title

  @Nullable String notes
  @Nullable String notDisplayed
  @Nullable String moreNotes = 'Default Notes'

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE') Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated


  /**
   * A reference to another domain object.
   */
  @Nullable @ManyToOne(targetEntity = AllFieldsDomain) AllFieldsDomain allFieldsDomain

  /**
   * A list of foreign references.
   */
  @ManyToMany(cascade = CascadeType.ALL, mappedBy = "sample_parent_all_fields_domain")
  List<AllFieldsDomain> allFieldsDomains

  /**
   * A list of children.
   */
  @OneToMany(mappedBy = "sampleParent")
  List<SampleChild> sampleChildren

  @ExtensibleFieldHolder
  @Column(nullable = true, length = 255)
  String customFields

  Integer version = 0

  @Id @AutoPopulated UUID uuid

  static fieldOrder = ['name', 'title', 'notes', 'moreNotes', 'allFieldsDomain', 'allFieldsDomains',
                       'sampleChildren']

  static keys = ['name']

  /**
   * If true, then the initial data load will load a record.
   */
  static allowInitialDataLoad = false

  /**
   * Load initial records.  Dummy test records for test mode only.
   */
  static initialDataLoad() {
    if (allowInitialDataLoad && !findByName('SAMPLE')) {
      def x = new SampleParent(name: 'SAMPLE').save()
      x.title = "set"
      x.save()
      x.delete()
    }
    return null
  }


  /**
   * Support routine for archiving.  Finds related records that should be archived when this record
   * is archived.  This sample class finds any AllFieldsDomains with the same key value.
   * @return The list of related records.
   */
  List findRelatedRecords() {
    return AllFieldsDomain.findAllByName(name)
  }

  /**
   * Returns a short representation of this object.  Suitable for drop-down lists.
   * @return The short string.
   */
  String toShortString() {
    return name
  }

}

