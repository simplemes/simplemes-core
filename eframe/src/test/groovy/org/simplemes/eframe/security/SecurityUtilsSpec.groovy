package org.simplemes.eframe.security

import ch.qos.logback.classic.Level
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class SecurityUtilsSpec extends BaseSpecification {

  def "verify that checkRoleFromSubClass works for valid case - single role on controller"() {
    given: 'a controller with a simple role'
    def src = """
    package sample
    import io.micronaut.security.annotation.Secured

    @Secured("MANAGER")        
    class _ATestController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the top-level security is checked'
    def res = SecurityUtils.instance.checkRoleFromSubClass(clazz.newInstance(), new MockPrincipal('joe', 'MANAGER'))

    then: 'the response is correct'
    res == null
  }

  def "verify that checkRoleFromSubClass works for valid case - multiple roles on controller"() {
    given: 'a controller with a simple role'
    def src = """
    package sample
    import io.micronaut.security.annotation.Secured

    @Secured(["ADMIN","MANAGER"])        
    class _ATestController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the top-level security is checked'
    def res = SecurityUtils.instance.checkRoleFromSubClass(clazz.newInstance(), new MockPrincipal('joe', 'MANAGER'))

    then: 'the response is correct'
    res == null
  }

  def "verify that checkRoleFromSubClass detects missing role - single role on controller"() {
    given: 'a controller with a simple role'
    def src = """
    package sample
    import io.micronaut.security.annotation.Secured

    @Secured("ADMIN")        
    class _ATestController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the top-level security is checked'
    def res = SecurityUtils.instance.checkRoleFromSubClass(clazz.newInstance(), new MockPrincipal('joe', 'MANAGER'))

    then: 'the response is correct'
    res.status == HttpStatus.FORBIDDEN
  }

  def "verify that checkRoleFromSubClass detects missing role - multiple roles on controller"() {
    given: 'a controller with a simple role'
    def src = """
    package sample
    import io.micronaut.security.annotation.Secured

    @Secured(["ADMIN","SUPERVISOR"])        
    class _ATestController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the top-level security is checked'
    def res = SecurityUtils.instance.checkRoleFromSubClass(clazz.newInstance(), new MockPrincipal('joe', 'MANAGER'))

    then: 'the response is correct'
    res.status == HttpStatus.FORBIDDEN
  }

  def "verify that checkRoleFromSubClass works for valid case - is anonymous role on controller"() {
    given: 'a controller with a simple role'
    def src = """
    package sample
    import io.micronaut.security.annotation.Secured
    import io.micronaut.security.rules.SecurityRule

    @Secured(SecurityRule.IS_ANONYMOUS)        
    class _ATestController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the top-level security is checked'
    def res = SecurityUtils.instance.checkRoleFromSubClass(clazz.newInstance(), new MockPrincipal('joe', 'MANAGER'))

    then: 'the response is correct'
    res == null
  }

  def "verify that checkRoleFromSubClass works for valid case - is authenticated role on controller"() {
    given: 'a controller with a simple role'
    def src = """
    package sample
    import io.micronaut.security.annotation.Secured
    import io.micronaut.security.rules.SecurityRule

    @Secured(SecurityRule.IS_AUTHENTICATED)        
    class _ATestController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the top-level security is checked'
    def res = SecurityUtils.instance.checkRoleFromSubClass(clazz.newInstance(), new MockPrincipal('joe', 'MANAGER'))

    then: 'the response is correct'
    res == null
  }

  def "verify that checkRoleFromSubClass works for valid case - is authenticated role on controller with none on user"() {
    given: 'a controller with a simple role'
    def src = """
    package sample
    import io.micronaut.security.annotation.Secured
    import io.micronaut.security.rules.SecurityRule

    @Secured(SecurityRule.IS_AUTHENTICATED)        
    class _ATestController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the top-level security is checked'
    def res = SecurityUtils.instance.checkRoleFromSubClass(clazz.newInstance(), new MockPrincipal('joe'))

    then: 'the response is correct'
    res == null
  }

  def "verify that checkRoleFromSubClass detects correct forbidden case - is authenticated role on controller with no user logged in"() {
    given: 'a controller with a simple role'
    def src = """
    package sample
    import io.micronaut.security.annotation.Secured
    import io.micronaut.security.rules.SecurityRule

    @Secured(SecurityRule.IS_AUTHENTICATED)        
    class _ATestController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the top-level security is checked'
    def res = SecurityUtils.instance.checkRoleFromSubClass(clazz.newInstance(), null)

    then: 'the response is correct'
    res.status == HttpStatus.FORBIDDEN
  }

  def "verify that checkRoleFromSubClass detects missing annotation on controller- returns forbidden"() {
    given: 'a controller with a simple role'
    def src = """
    package sample
  
    class _ATestController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a mock appender for ERROR logging'
    def mockAppender = MockAppender.mock(SecurityUtils, Level.ERROR)

    when: 'the top-level security is checked'
    def res = SecurityUtils.instance.checkRoleFromSubClass(clazz.newInstance(), new MockPrincipal('joe', 'ADMIN'))

    then: 'the response is correct'
    res.status == HttpStatus.FORBIDDEN

    and: 'an error log message is logged'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['ERROR', 'No', 'Secured', 'annotation', 'sample._ATestController'])
  }

  def "verify that isAllGranted works for valid case - single role on controller"() {
    expect: 'the grant is checked correctly'
    SecurityUtils.instance.isAllGranted(required, new MockPrincipal('joe', granted)) == results

    where:
    granted              | required        | results
    ['MANAGER']          | 'MANAGER'       | true
    ['MANAGER', 'ADMIN'] | 'ADMIN'         | true
    ['MANAGER', 'ADMIN'] | 'ADMIN,MANAGER' | true
    ['MANAGER']          | ''              | true
    ['MANAGER']          | null            | true
    []                   | ''              | true
    ['ADMIN']            | 'ADMIN,MANAGER' | false
    []                   | 'MANAGER'       | false
  }

}
