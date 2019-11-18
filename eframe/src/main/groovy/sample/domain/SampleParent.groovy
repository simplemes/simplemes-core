package sample.domain

import grails.gorm.annotation.Entity
import groovy.transform.ToString
import org.simplemes.eframe.data.annotation.ExtensibleFields

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A test/Sample parent domain class.
 * Has child and foreign references.
 * <p>
 * <b>Fields</b> Include: name, title, notes, notDisplayed, moreNotes. allFieldsDomains, allFieldDomain, sampleChildren,
 * dateCreated, lastUpdated
 */
@Entity
@ToString(includePackage = false, includeNames = true, includes = ['name', 'title', 'id', 'notes', 'notDisplayed',
  'moreNotes', 'dateCreated', 'lastUpdated', 'allFieldsDomain', 'allFieldsDomains', 'sampleChildren'])
@SuppressWarnings("unused")
@ExtensibleFields
class SampleParent {

  // ********************************************************
  // * Note: Do not change these without running all tests.
  // *       These fields are used by many tests.
  // ********************************************************

  String name
  String title
  String notes
  String notDisplayed
  String moreNotes = 'Default Notes'
  Date dateCreated
  Date lastUpdated

  /**
   * A reference to another domain object.
   */
  AllFieldsDomain allFieldsDomain

  /**
   * A list of foreign references.
   */
  List allFieldsDomains

  /**
   * A list of children.
   */
  List<SampleChild> sampleChildren = []
  static hasMany = [sampleChildren: SampleChild, allFieldsDomains: AllFieldsDomain]

  static constraints = {
    name nullable: false, blank: false, maxSize: 40, unique: true
    title nullable: true, blank: true, maxSize: 20
    notes nullable: true, blank: true, maxSize: 200
    moreNotes nullable: true, blank: true, maxSize: 200
    notDisplayed nullable: true, blank: true, maxSize: 200
    allFieldsDomain nullable: true
  }

  static fieldOrder = ['name', 'title', 'notes', 'moreNotes', 'allFieldsDomain', 'allFieldsDomains',
                       'sampleChildren',]
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
      new SampleParent(name: 'SAMPLE').save()
    }
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

