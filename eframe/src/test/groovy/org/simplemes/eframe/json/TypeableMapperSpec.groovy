package org.simplemes.eframe.json


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.pogo.SampleAlternatePOGO
import sample.pogo.SamplePOGO

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class TypeableMapperSpec extends BaseSpecification {

  def specNeeds = [JSON]

  def "verify that round-trip with multiple elements works"() {
    given: 'some POGO objects to be serialized'
    def o1 = new SamplePOGO(name: 'ABC')
    def o2 = new SampleAlternatePOGO(name: 'XYZ')

    and: 'a buffer to write to'
    def stringWriter = new StringWriter()

    when: 'the list is serialized'
    TypeableMapper.instance.writeList(stringWriter, [o1, o2])
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(stringWriter.toString())}"

    and: 'the JSON is de-serialized into objects'
    def list = TypeableMapper.instance.read(new StringReader(stringWriter.toString()))
    //println "list = $list"

    then: 'the right objects are returned'
    list.size() == 2
    list[0] == o1
    list[1] == o2
  }

  def "verify that round-trip with one object at a time works"() {
    given: 'some POGO objects to be serialized'
    def o1 = new SamplePOGO(name: 'ABC')
    def o2 = new SampleAlternatePOGO(name: 'XYZ')

    and: 'a buffer to write to'
    def stringWriter = new StringWriter()

    when: 'the objects are serialized, one at a time'
    TypeableMapper.instance.start(stringWriter)
    TypeableMapper.instance.writeOne(stringWriter, o1, true)
    TypeableMapper.instance.writeOne(stringWriter, o2, false)
    TypeableMapper.instance.finish(stringWriter)
    //println "stringWriter = $stringWriter"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(stringWriter.toString())}"

    and: 'the JSON is de-serialized into objects'
    def list = TypeableMapper.instance.read(new StringReader(stringWriter.toString()))
    //println "list = $list"

    then: 'the right objects are returned'
    list.size() == 2
    list[0] == o1
    list[1] == o2
  }

  def "verify that elements with wrong type cannot be serialized"() {
    when: 'the list is serialized'
    TypeableMapper.instance.writeList(new StringWriter(), ['ABC'])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['String', 'not', 'allowed', 'TypeableMapper'])
  }

  def "verify that elements with wrong type cannot be de-serialized"() {
    given: 'JSON with invalid class name'
    def src = """
    [ "java.lang.String", {
      "name": "ABC"
      }
    ]"""

    when: 'the list is serialized'
    TypeableMapper.instance.read(new StringReader(src))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['String', 'not', 'allowed', 'TypeableMapper'])
  }

  def "verify that elements with invalid class name cannot be de-serialized"() {
    given: 'JSON with invalid class name'
    def src = """
    [ "java.lang.Gibberish", {
      "name": "ABC"
      }
    ]"""

    when: 'the list is serialized'
    TypeableMapper.instance.read(new StringReader(src))

    then: 'the right exception is thrown'
    def ex = thrown(ClassNotFoundException)
    UnitTestUtils.assertExceptionIsValid(ex, ['Gibberish'])
  }

  def "verify that JSON with odd number of elements cannot be de-serialized"() {
    given: 'JSON with invalid class name'
    def src = """
    [ "sample.pogo.SampleAlternatePOGO" ]"""

    when: 'the list is serialized'
    TypeableMapper.instance.read(new StringReader(src))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['(1)', 'odd'])
  }

}
