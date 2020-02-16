/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application

import ch.qos.logback.classic.Level
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.InitialDataRecords
import org.simplemes.eframe.test.MockAdditionHelper
import org.simplemes.eframe.test.MockAppender

/**
 *
 */
class InitialDataLoaderSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [User, Role]

  def "verify that initial data from real domains is loaded correctly"() {
    given: 'the loader has finished loading the users'
    waitForInitialDataLoad()
    def loader = new InitialDataLoader()

    when: 'the data is loaded'
    def loadedClasses = loader.dataLoad()

    then: 'the expected user exists'
    User.withTransaction {
      assert User.list().size() == 1
      true
    }

    and: 'the right domains are loaded'
    loadedClasses.containsAll([Role, User])

    and: 'the domains are loaded in the correct order'
    loadedClasses.indexOf(Role) < loadedClasses.indexOf(User)

    and: 'the records are registered as initial data loaded records for the test cleanup'
    def records = InitialDataRecords.instance.records
    records.Role.containsAll(['ADMIN', 'CUSTOMIZER', 'MANAGER', 'DESIGNER'])
    records.User.containsAll(['admin'])
  }

  def "verify that performance details are logged"() {
    given: 'the log appender is mock to capture the right messages'
    def mockAppender = MockAppender.mock(InitialDataLoader, Level.INFO)

    when: 'the data is loaded'
    new InitialDataLoader().dataLoad()

    then: 'the info message is logged'
    mockAppender.assertMessageIsValid(['time', 'ms'])
  }

  /**
   * Builds and compiles a temporary class with the given name and initialDataLoadAfter list.
   * @param className The class to compile.
   * @param beforeClassNames The classes to make sure this is executed before.
   * @param afterClassNames The classes to make sure this is executed after.
   * @param loadScript The script execute in the load method.  Optional.
   * @return The compiled class.
   */
  Class compileIDLClass(String className, List<String> beforeClassNames, List<String> afterClassNames, String loadScript = '') {
    def sb = new StringBuilder()
    def pkg = "sample"
    sb << "package $pkg\n"
    sb << "class $className {\n"
    sb << "static initialDataLoad() {$loadScript }\n"
    sb << "static withTransaction(Closure c) {c.call()}\n"
    if (afterClassNames) {
      def names = new StringBuilder()
      for (name in afterClassNames) {
        if (names) {
          names << ","
        }
        names << "'$pkg.$name'"
      }
      sb << "static initialDataLoadAfter = [${names}]\n"
    }
    if (beforeClassNames) {
      def names = new StringBuilder()
      for (name in beforeClassNames) {
        if (names) {
          names << ","
        }
        names << "'$pkg.$name'"
      }
      sb << "static initialDataLoadBefore = [${names}]\n"
    }

    sb << "}\n"
    //println "sb = $sb"

    return CompilerTestUtils.compileSource(sb.toString())
  }


  def "verify that sort handles the supported cases"() {
    given: 'an initial data loader'
    def loader = new InitialDataLoader()

    and: 'the temporary domains are compiled'
    def compiledDomains = []
    for (d in domains) {
      compiledDomains << compileIDLClass(d.name, d.before, d.after)
    }

    and: 'some mocked domains'
    DomainUtils.instance = Mock(DomainUtils)
    DomainUtils.instance.allDomains >> compiledDomains

    when: 'the domains to load are sorted'
    def loaded = loader.dataLoad()

    then: 'the classes are loaded in the right order'
    def loadedNames = loaded*.simpleName
    loadedNames == results

    cleanup:
    DomainUtils.instance = new DomainUtils()

    where:
    domains                                                                                   | results
    [[name: 'a1', after: []], [name: 'a2', after: []]]                                        | ['a1', 'a2']
    [[name: 'a1', after: ['a2']], [name: 'a2', after: []]]                                    | ['a2', 'a1']
    [[name: 'a1', after: ['a2']], [name: 'a2', after: []], [name: 'a3', after: ['a1', 'a2']]] | ['a2', 'a1', 'a3']
    [[name: 'a3', after: ['a1', 'a2']], [name: 'a1', after: ['a2']], [name: 'a2', after: []]] | ['a2', 'a1', 'a3']
    [[name: 'a3', before: []], [name: 'a1', before: ['a3']], [name: 'a2', before: ['a1']]]    | ['a2', 'a1', 'a3']
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that sort handles chain - B before A  and C before A"() {
    given: 'an initial data loader'
    def loader = new InitialDataLoader()

    and: 'the temporary domains are compiled'
    List<Map> list = [[name: 'B', before: ['A']], [name: 'A'], [name: 'C', before: ['A']]]
    def compiledDomains = []
    for (d in list) {
      compiledDomains << compileIDLClass(d.name, d.before, d.after)
    }

    and: 'some mocked domains'
    DomainUtils.instance = Mock(DomainUtils)
    DomainUtils.instance.allDomains >> compiledDomains

    when: 'the domains to load are sorted'
    def loaded = loader.dataLoad()

    then: 'the classes are loaded in the right order'
    def loadedNames = loaded*.simpleName
    loadedNames.size() == 3
    loadedNames[2] == 'A'

    cleanup:
    DomainUtils.instance = new DomainUtils()
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that sort handles both before and after as expected"() {
    given: 'an initial data loader'
    def loader = new InitialDataLoader()

    and: 'the temporary domains are compiled'
    //List<Map> list = [[name: 'B'], [name: 'A'], [name: 'C', before: ['A'], after: ['A']]]
    def compiledDomains = []
    for (d in domains) {
      compiledDomains << compileIDLClass(d.name, d.before, d.after)
    }

    and: 'some mocked domains'
    DomainUtils.instance = Mock(DomainUtils)
    DomainUtils.instance.allDomains >> compiledDomains

    when: 'the domains to load are sorted'
    def loaded = loader.dataLoad()

    then: 'C is always before A'
    def loadedNames = loaded*.simpleName
    loadedNames.indexOf('C') < loadedNames.indexOf('A')

    cleanup:
    DomainUtils.instance = new DomainUtils()

    where: 'the various initial orders are used'
    domains                                                              | _
    [[name: 'B'], [name: 'A'], [name: 'C', before: ['A'], after: ['A']]] | _
    [[name: 'B'], [name: 'C', before: ['A'], after: ['A']], [name: 'A']] | _
  }

  def "verify that the loader can handle exceptions in one of the loader domains"() {
    given: 'the log appender is mock to capture the right messages'
    def mockAppender = MockAppender.mock(InitialDataLoader, Level.ERROR)

    and: 'an initial data loader'
    def loader = new InitialDataLoader()

    and: 'some temporary domains are compiled - one will fail'
    def compiledDomains = []
    compiledDomains << compileIDLClass('a1', [], [])
    compiledDomains << compileIDLClass('a2', [], [], 'throw new IllegalArgumentException("bad data")')
    compiledDomains << compileIDLClass('a3', [], [])

    and: 'some mocked domains'
    DomainUtils.instance = Mock(DomainUtils)
    DomainUtils.instance.allDomains >> compiledDomains

    when: 'the domains to load are sorted'
    def loaded = loader.dataLoad()

    then: 'the classes are loaded in the right order, without the failed entry'
    def loadedNames = loaded*.simpleName
    loadedNames == ['a1', 'a3']

    and: 'the error is logged'
    mockAppender.assertMessageIsValid(['bad data', 'illegal'])

    cleanup:
    DomainUtils.instance = new DomainUtils()
    mockAppender.cleanup()
  }

  def "verify that initial loaders from an addition can be found"() {
    given: 'a simple mocked addition'
    def src = """
    package sample
    
    import org.simplemes.eframe.system.BasicStatus

    List<Class> getInitialDataLoaders() {
      return [org.simplemes.eframe.application.InitialDataLoad1]  
    }
    """

    def clazz = CompilerTestUtils.compileSource(src)
    new MockAdditionHelper(this, [clazz]).install()

    when: 'the data is loaded'
    def loadedClasses = new InitialDataLoader().dataLoad()

    then: 'the new loader is called'
    loadedClasses.contains(InitialDataLoad1)

    and: 'the method was really called'
    InitialDataLoad1.called
  }
}

/**
 * A test loader.
 */
class InitialDataLoad1 {

  static boolean called = false

  @SuppressWarnings("unused")
  static Map<String, List<String>> initialDataLoad() {
    called = true
    return [:]
  }
}

