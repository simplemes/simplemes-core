/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/**
 * Tests.
 */
class ClassPathScannerSpec extends BaseSpecification {

  def "verify that scan works for reports file folders"() {
    when: 'a scan is preformed'
    def scanner = new ClassPathScanner('reports/*.jrxml')
    def list = scanner.scan()*.toString()

    then: 'the list contains the core report'
    list.contains('reports/eframe/ArchiveLog.jrxml')
    list.contains('reports/sample/SampleReport.jrxml')
  }

  def "verify that scan works without wildcard"() {
    when: 'a scan is preformed'
    def scanner = new ClassPathScanner('reports/eframe/ArchiveLog.jrxml')
    def list = scanner.scan()*.toString()

    then: 'the list contains the core report'
    list.contains('reports/eframe/ArchiveLog.jrxml')
    !list.contains('reports/sample/SampleReport.jrxml')
  }

  def "verify that scan works for class files in a .jar file"() {
    when: 'a scan is preformed'
    def scanner = new ClassPathScanner('io/micronaut/data/jdbc/annotation/*.class')
    def list = scanner.scan()

    then: 'the list contains the URL from the .jar file and can be opened as a stream'
    def url = list.find { it.toString().endsWith('io/micronaut/data/jdbc/annotation/JdbcRepository.class') }
    def stream = url.openStream()
    stream.close()
  }

  def "verify that scan works for sub folders in a .jar file"() {
    when: 'a scan is preformed'
    def scanner = new ClassPathScanner('io/micronaut/data/*.class')
    def list = scanner.scan()
    println "list = $list"

    then: 'the list contains the URL from the .jar file and can be opened as a stream'
    def url = list.find { it.toString().endsWith('io/micronaut/data/jdbc/annotation/JdbcRepository.class') }
    def stream = url.openStream()
    stream.close()
  }

  def "verify that scanner detects bad search path"() {
    when: 'a scan is preformed'
    def scanner = new ClassPathScanner(searchPath)
    scanner.scan()*.toString()

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['valid', '/'])
    if (searchPath) {
      UnitTestUtils.assertExceptionIsValid(ex, [searchPath])
    }

    where:
    searchPath         | _
    'gibberish*.jrxml' | _
    ''                 | _
    null               | _
  }

  def "verify that scanner factory works"() {
    when: 'a scan is preformed'
    def scanner = ClassPathScanner.factory.buildScanner('io/micronaut/data/jdbc/annotation/*.class')
    def list = scanner.scan()

    then: 'the list contains the URL from the .jar file and can be opened as a stream'
    def url = list.find { it.toString().endsWith('io/micronaut/data/jdbc/annotation/JdbcRepository.class') }
    def stream = url.openStream()
    stream.close()
  }

  def "verify that scanner factory can be replaced with a mock"() {
    given: 'a mocked scanner'
    List<URL> results = [new URL('file:abc'), new URL('file:def')]
    def mockScanner = Mock(ClassPathScanner)
    def mockFactory = Mock(ClassPathScannerFactory)
    ClassPathScanner.factory = mockFactory
    1 * mockScanner.scan() >> results
    1 * mockFactory.buildScanner(_) >> mockScanner

    when: 'a scan is preformed'
    def scanner = ClassPathScanner.factory.buildScanner('io/micronaut/data/jdbc/annotation/*.class')
    def list = scanner.scan()

    then: 'the list contains the URL from the .jar file and can be opened as a stream'
    list == results

    cleanup:
    ClassPathScanner.factory = new ClassPathScannerFactory()
  }

}
