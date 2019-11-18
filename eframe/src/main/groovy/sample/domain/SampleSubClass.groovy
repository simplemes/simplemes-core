package sample.domain

import grails.gorm.annotation.Entity
import groovy.transform.ToString

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of sub-class. Adds a field subTitle to the SampleParent.
 */
@Entity
@ToString(includeNames = true, includePackage = false)
class SampleSubClass extends SampleParent {

  String subTitle

  static constraints = {
    subTitle nullable: true, blank: true, maxSize: 20
  }

  static fieldOrder = ['subTitle']

  /**
   * Load initial records.  Dummy test records for test mode only.
   */
  static initialDataLoad() {
    if (allowInitialDataLoad && !findByName('SAMPLE_SUB')) {
      new SampleSubClass(name: 'SAMPLE_SUB').save()
    }
  }


}
