/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.UnitTestUtils

/**
 * Tests.
 */
class SearchValidationSpec extends BaseSpecification {

  def setup() {
    CompilerTestUtils.printCompileFailureSource = false
  }

  void cleanup() {
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that the validation logic detects missing JsonFilter annotation for the domain entity transformation"() {
    given: 'a class with the annotation'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import groovy.transform.ToString
      
      @DomainEntity
      @ToString(includeNames = true)
      class TestClass {
        UUID uuid
        static searchable = {exclude = ['uuid']}
      }
    """

    when: 'the domain is compiled and a new value is used'
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    //println "ex = $ex"
    UnitTestUtils.assertExceptionIsValid(ex, ["@JsonFilter('searchableFilter')", 'annotation', 'exclude'])
  }

  def "verify that the validation logic detects non-static searchable field"() {
    given: 'a class with the annotation'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import groovy.transform.ToString
      
      @DomainEntity
      @ToString(includeNames = true)
      class TestClass {
        UUID uuid
        def searchable = true
      }
    """

    when: 'the domain is compiled and a new value is used'
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['searchable', 'static', 'TestClass'])
  }

  def "verify that the validation logic detects invalid class for searchable field"() {
    given: 'a class with the annotation'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import groovy.transform.ToString
      
      @DomainEntity
      @ToString(includeNames = true)
      class TestClass {
        UUID uuid
        static String searchable = "true"
      }
    """

    when: 'the domain is compiled and a new value is used'
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['searchable', 'boolean', 'closure', 'TestClass'])
  }

  def "verify that the validation logic allows JsonFilter is correctly configured"() {
    given: 'a class with the annotation'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import groovy.transform.ToString
      import com.fasterxml.jackson.annotation.JsonFilter
      
      @DomainEntity
      @ToString(includeNames = true)
      @JsonFilter("searchableFilter")
      class TestClass {
        UUID uuid
        static searchable = {exclude = ['uuid']}
      }
    """

    when: 'the domain is compiled and a new value is used'
    CompilerTestUtils.compileSource(src)

    then: 'no exception is thrown'
    notThrown(Exception)
  }

  def "verify that the validation logic issues a warning without exception for missing JsonFilter annotation"() {
    given: 'a class with the annotation'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import groovy.transform.ToString
      import com.fasterxml.jackson.annotation.JsonFilter
      
      @DomainEntity
      @ToString(includeNames = true)
      class TestClass {
        UUID uuid
        static searchable = true
      }
    """

    when: 'the domain is compiled and a new value is used'
    CompilerTestUtils.compileSource(src)

    then: 'no exception is thrown'
    notThrown(Exception)

    and: 'the warning has the right details'
    UnitTestUtils.assertContainsAllIgnoreCase(SearchValidation.lastWarningMessage, ['JsonFilter', 'annotation', 'searchableFilter'])
  }
}
