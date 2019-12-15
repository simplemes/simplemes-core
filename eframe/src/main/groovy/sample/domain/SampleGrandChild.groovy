package sample.domain


//import grails.gorm.annotation.Entity
import groovy.transform.ToString

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A test/Sample grand child domain class.
 */
//@Entity
@ToString(includePackage = false, includeNames = true, excludes = ['sampleChild'])
class SampleGrandChild {

  // ********************************************************
  // * Note: Do not change these without running all tests.
  // *       These fields are used by many tests.
  // ********************************************************

  SampleChild sampleChild
  static belongsTo = [sampleChild: SampleChild]
  String grandKey
  String title

  static constraints = {
    grandKey nullable: false, blank: false, maxSize: 40
    title nullable: true, blank: true, maxSize: 20
  }

  static fieldOrder = ['grandKey', 'title']

}

