package org.simplemes.eframe.test

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.security.domain.User
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DataGeneratorSpec extends BaseSpecification {

  //static specNeeds = [HIBERNATE]
  static dirtyDomains = [SampleParent]

  def "verify that simple case works without rollback annotation"() {
    when: 'some data is generated'
    def records = DataGenerator.generate {
      domain SampleParent
      count 10
      values notes: 'XYZ${i}xyz$r'
    } as List<SampleParent>

    then: 'the generated records are expected'
    records.size() == 10
    records[0].name == 'ABC001'
    records[0].title == 'abc010'
    records[0].notes == 'XYZ001xyz010'
    records[9].name == 'ABC010'
    records[9].title == 'abc001'
    records[9].notes == 'XYZ010xyz001'
  }

  @Rollback
  def "verify that simple case works with rollback annotation"() {
    when: 'some data is generated'
    def records = DataGenerator.generate {
      domain SampleParent
      values notes: 'XYZ${i}xyz$r'
    } as List<SampleParent>

    then: 'the generated records are expected'
    records.size() == 1
    records[0].name == 'ABC001'
  }

  @Rollback
  def "verify that various types can be incremented correctly"() {
    given: 'the starting values'
    def dateOnly = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def date = new Date() - 50

    when: 'some data is generated'
    def records = DataGenerator.generate {
      domain AllFieldsDomain
      count 3
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true,
             dateTime: date, dueDate: dateOnly
    } as List<AllFieldsDomain>

    then: 'the generated records are expected'
    records.size() == 3
    records[0].name == 'ABC-001'
    records[0].qty == 1.0
    records[0].count == 10
    records[0].enabled
    records[0].dateTime == date
    records[0].dueDate == dateOnly

    and: 'the second record has the right incremented values'
    records[1].name == 'ABC-002'
    records[1].qty == 2.0
    records[1].count == 11
    !records[1].enabled
    records[1].dateTime == date + 1
    records[1].dueDate == dateOnly + 1

    and: 'the number keeps increment'
    records[2].count == 12

  }

  @Rollback
  def "verify that generate gracefully handles no domain"() {
    when: 'some data is generated'
    DataGenerator.generate {
      values notes: 'XYZ${i}xyz$r'
    } as List<SampleParent>

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['domain'])
  }

  @Rollback
  def "verify that a foreign domain reference can be used in the values"() {
    when: 'some data is generated'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
      values name: 'ABC-$i', title: 'abc-$r', qty: 1.0, count: 10, enabled: true
    }
    def (SampleParent sampleParent) = DataGenerator.generate {
      domain SampleParent
      values allFieldsDomain: allFieldsDomain
    }

    then: 'the generated records are expected'
    sampleParent.allFieldsDomain == allFieldsDomain
  }

  @Rollback
  def "verify that buildTestUser works for the none user "() {
    when: 'some data is generated'
    DataGenerator.buildTestUser('none')

    then: 'the record is created'
    User.findByUserName('none')
  }

  @Rollback
  def "verify that buildTestUser works for a user with a role"() {
    given: 'a role for the user'
    def role = new Role(authority: 'DUMMY1', title: 'dummy1').save()

    when: 'some data is generated'
    DataGenerator.buildTestUser('DUMMY1')

    then: 'the record is created'
    def user = User.findByUserName('DUMMY1')
    user.userRoles.contains(role)
  }

}
