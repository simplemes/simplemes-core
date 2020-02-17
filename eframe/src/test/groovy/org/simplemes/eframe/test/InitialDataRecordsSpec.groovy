/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import org.simplemes.eframe.security.domain.Role

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


}
