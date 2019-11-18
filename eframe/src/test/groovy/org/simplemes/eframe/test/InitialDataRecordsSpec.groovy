package org.simplemes.eframe.test

import org.simplemes.eframe.security.domain.Role
import sample.domain.SampleSubClass

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class InitialDataRecordsSpec extends BaseSpecification {

  def "verify that register handles adding new elements to the records list"() {
    given: 'an empty list of records'
    def original = InitialDataRecords.instance
    InitialDataRecords.instance = new InitialDataRecords()

    and: 'some original values are in the list'
    InitialDataRecords.instance.register([Role: ['Admin', 'Customizer', 'Manager', 'Designer']], Role)

    when: 'new records are added'
    InitialDataRecords.instance.register([Role: ['Lead', 'Dummy']], Role)

    then: 'the records are only in the list once'
    def res = InitialDataRecords.instance.records
    res.Role.containsAll(['Lead', 'Dummy', 'Admin', 'Customizer', 'Manager', 'Designer'])

    cleanup: 'restore list'
    InitialDataRecords.instance = original
  }

  def "verify that register handles re-running with the same values"() {
    given: 'an empty list of records'
    def original = InitialDataRecords.instance
    InitialDataRecords.instance = new InitialDataRecords()

    and: 'a list of records'
    def records = [User: ['admin'],
                   Role: ['Admin', 'Customizer', 'Manager', 'Designer']]

    when: 'a set of records is added twice'
    InitialDataRecords.instance.register(records, Role)
    InitialDataRecords.instance.register(records, Role)

    then: 'the records are only in the list once'
    def res = InitialDataRecords.instance.records
    res.User == ['admin']
    res.Role == ['Admin', 'Customizer', 'Manager', 'Designer']

    cleanup: 'restore list'
    InitialDataRecords.instance = original
  }

  def "verify that register gracefully handles null and empty input"() {
    when: 'a set of records is added twice'
    InitialDataRecords.instance.register(null, Role)
    InitialDataRecords.instance.register([:], Role)

    then: 'the records are only in the list once'
    notThrown(Exception)
  }

  def "verify that register handles the parent domain class case"() {
    // This handles the case when a sub-class of a domain loads data records.
    // The BaseSpecification finds the records in both domains, so the register function duplicates
    // the allowed records for the parent class.
    given: 'an empty list of records'
    def original = InitialDataRecords.instance
    InitialDataRecords.instance = new InitialDataRecords()

    and: 'a list of records'
    def records = [SampleSubClass: ['Admin', 'Customizer']]

    when: 'a set of records is added for the sub-class'
    InitialDataRecords.instance.register(records, SampleSubClass)

    then: 'the records are only in the list once'
    def res = InitialDataRecords.instance.records
    res.SampleSubClass == ['Admin', 'Customizer']
    res.SampleParent == ['Admin', 'Customizer']

    cleanup: 'restore list'
    InitialDataRecords.instance = original
  }

}
