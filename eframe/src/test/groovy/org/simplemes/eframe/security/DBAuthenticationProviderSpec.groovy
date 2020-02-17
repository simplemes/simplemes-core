/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security


import io.micronaut.security.authentication.AuthenticationFailed
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.authentication.UsernamePasswordCredentials
import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback

/**
 * Tests.
 */
class DBAuthenticationProviderSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [User]

  @Rollback
  def "verify that authenticate works with User record in DB"() {
    given: 'a user record with roles'
    def role1 = new Role(authority: 'ROLE_1', title: '1').save()
    def role2 = new Role(authority: 'ROLE_2', title: '2').save()
    def user = new User(userName: 'ABC', password: 'XYZ')
    user.userRoles << role1
    user.userRoles << role2
    user.save()


    when: 'the authentication is checked'
    UserDetails res = new DBAuthenticationProvider().authenticate(new UsernamePasswordCredentials('ABC', 'XYZ')).call()

    then: 'the user is authenticated'
    res instanceof UserDetails

    and: 'the roles are correct'
    res.roles.contains(role1.authority)
    res.roles.contains(role2.authority)
  }

  @Rollback
  def "verify that authenticate detects missing user record"() {
    when: 'the authentication is checked'
    def res = new DBAuthenticationProvider().authenticate(new UsernamePasswordCredentials('ABC', 'XYZ')).call()

    then: 'the user is authenticated'
    res instanceof AuthenticationFailed
  }

  @Rollback
  def "verify that authenticate fails with disabled User record"() {
    given: 'a disabled record'
    def user = new User(userName: 'ABC', password: 'XYZ', enabled: false)
    user.save()

    when: 'the authentication is checked'
    def res = new DBAuthenticationProvider().authenticate(new UsernamePasswordCredentials('ABC', 'XYZ')).call()

    then: 'the user is authenticated'
    res instanceof AuthenticationFailed
  }

  def "verify that authenticate fails with the reason for supported cases - locked, expired, password expired"() {
    given: 'a disabled record'
    User.withTransaction {
      options.userName = 'ABC'
      options.password = 'XYZ'
      def user = new User(options)
      user.save()
    }

    when: 'the authentication is checked'
    AuthenticationFailed res = new DBAuthenticationProvider().authenticate(new UsernamePasswordCredentials('ABC', 'XYZ')).call()

    then: 'the user is authenticated'
    res instanceof AuthenticationFailed

    and: 'the message is correct'
    UnitTestUtils.assertContainsAllIgnoreCase(res.message.get(), msgs)

    where:
    options                 | msgs
    [accountLocked: true]   | ['locked']
    [accountExpired: true]  | ['account', 'expired']
    [passwordExpired: true] | ['password', 'expired']
  }
}
