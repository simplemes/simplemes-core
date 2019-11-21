package org.simplemes.eframe.security.domain

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.data.annotation.ExtensibleFields
import org.simplemes.eframe.data.format.DomainRefListFieldFormat
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
@SuppressWarnings("unused")
class UserSpec extends BaseSpecification {

  static specNeeds = [HIBERNATE]
  static dirtyDomains = [User, Role]

  def "verify that user domain enforces constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain User
      requiredValues userName: 'ADMIN', password: 'password'
      maxSize 'userName', FieldSizes.MAX_CODE_LENGTH
      maxSize 'password', 128
      notNullCheck 'userName'
      notNullCheck 'password'
      notInFieldOrder(['authoritySummary', 'password', ExtensibleFields.DEFAULT_FIELD_NAME])
    }
  }

  @Rollback
  def "verify that user record can be saved"() {
    when: 'a user is saved'
    def user = new User(userName: 'ABC', password: 'XYZ')
    user.save()

    then: 'the record is saved'
    User.findByUserName('ABC')
  }

  @Rollback
  def "verify that user domain encrypts the password on create"() {
    when: 'a user is saved'
    def user = new User(userName: 'ABC', password: 'XYZ')
    user.save()

    then: 'the password is encrypted correctly'
    def user2 = User.findByUserName('ABC')
    user2.passwordMatches('XYZ')
    //new PasswordEncoderService().matches('XYZ', user2.password)
  }

  def "verify that user domain encrypts the password on update"() {
    when: 'a user is saved'
    User.withTransaction {
      def user = new User(userName: 'ABC', password: 'XYZ')
      user.save()
      user.password = 'PDQ'
      user.save()
    }

    then: 'the password is encrypted correctly'
    User.withTransaction {
      def user = User.findByUserName('ABC')
      //assert new PasswordEncoderService().matches('PDQ', user.password)
      assert user.passwordMatches('PDQ')
      return true
    }

    cleanup:
    User.withTransaction {
      def user = User.findByUserName('ABC')
      user.delete()
    }

  }

  def "verify that the global failOnErrorPackages setting works"() {
    when: 'a user is saved with missing required field'
    User.withTransaction {
      def user = new User(userName: 'ABC')
      user.save()
    }

    then: 'an exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertContainsAllIgnoreCase(ex, ['password'])
  }

  @Rollback
  def "verify that the load initial data  works"() {
    given: 'all user records are deleted'
    deleteAllRecords(User, false)

    and: 'the roles are loaded'
    Role.initialDataLoad()

    when: ''
    assert User.list().size() == 0
    User.initialDataLoad()

    then: 'the expected record is loaded'
    def user = User.findByUserName('admin')
    //println "user = ${user.dump()}"
    user.userRoles.size() == Role.list().size()
  }

  @Rollback
  def "verify that the child userRoles can be populated"() {
    given: 'some roles'
    def role1 = new Role(authority: '1', title: '1').save()
    def role2 = new Role(authority: '2', title: '2').save()

    when: 'a user is created with roles'
    def user = new User(userName: 'ABC', password: 'xyz')
    user.addToUserRoles(role2)
    user.addToUserRoles(role1)
    user.save()

    then: 'the record can be reloaded'
    def user2 = User.findByUserName('ABC')
    user2.userRoles.size() == 2

    and: 'the userRoles list is the correct format'
    def fieldDefs = DomainUtils.instance.getFieldDefinitions(User)
    def fieldDef = fieldDefs['userRoles']
    fieldDef.format == DomainRefListFieldFormat.instance
  }

}