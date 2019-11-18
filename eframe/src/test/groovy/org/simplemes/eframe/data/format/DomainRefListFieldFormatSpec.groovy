package org.simplemes.eframe.data.format

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.MockFieldDefinitions
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
class DomainRefListFieldFormatSpec extends BaseSpecification {

  static dirtyDomains = [SampleParent, AllFieldsDomain]

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    DomainRefListFieldFormat.instance.id == DomainRefListFieldFormat.ID
    DomainRefListFieldFormat.instance.toString() == 'DomainRefList'
    DomainRefListFieldFormat.instance.type == List
    BasicFieldFormat.coreValues.contains(DomainRefListFieldFormat)
  }

  def "verify that the format method works"() {
    given: 'some domain references'
    def list = DataGenerator.generate {
      domain AllFieldsDomain
      count 3
    }

    when: 'the method is called'
    def s = DomainRefListFieldFormat.instance.format(list, null, null)

    then: 'the encoded values are correct'
    s == "ABC001, ABC002, ABC003"
  }

  def "verify that the format method handles empty list"() {
    when: 'the method is called'
    def s = DomainRefListFieldFormat.instance.format([], null, null)

    then: 'the encoded values are correct'
    s == ""
  }

  def "verify that the parse method handles empty list of values"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    expect: 'the method is called'
    DomainRefListFieldFormat.instance.parse('', null, fieldDef) == null
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Rollback
  def "verify that the parse method handles list of 1 value"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    and: 'a record for the domain ref'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
    }

    when: 'the method is called'
    def list = (List) DomainRefListFieldFormat.instance.parse("${allFieldsDomain.id}", null, fieldDef)

    then: 'the list contains the value'
    list[0].id == allFieldsDomain.id
  }

  @Rollback
  def "verify that the parse method handles list of multiple values"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    and: 'a record for the domain ref'
    def allFieldsDomains = DataGenerator.generate {
      domain AllFieldsDomain
      count 10
    }

    when: 'the method is called'
    def list = (List) DomainRefListFieldFormat.instance.parse(allFieldsDomains*.id.join(','), null, fieldDef)

    then: 'the list contains the value'
    list.size() == 10
    for (afd in allFieldsDomains) {
      assert list.find { it.id == afd.id }
    }
  }

  def "verify that the encode method encodes a list correctly"() {
    given: 'some domain references'
    def list = DataGenerator.generate {
      domain AllFieldsDomain
      count 3
    }

    when: 'the method is called'
    def s = DomainRefListFieldFormat.instance.encode(list, null)

    then: 'the encoded value is correct'
    s == list*.id.join(',')
  }

  def "verify that the encode method encodes handles an empty list gracefully"() {
    when: 'the method is called'
    def s = DomainRefListFieldFormat.instance.encode([], null)

    then: 'the encoded value is correct'
    s == ''
  }

  def "verify that the encode method encodes handles an null list gracefully"() {
    when: 'the method is called'
    def s = DomainRefListFieldFormat.instance.encode(null, null)

    then: 'the encoded value is correct'
    s == ''
  }

  @Rollback
  def "verify that the decode method decodes a list of IDs correctly"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    and: 'some domain references'
    def list = DataGenerator.generate {
      domain AllFieldsDomain
      count 3
    }

    and: 'the encoded IDs in a list'
    def ids = list*.id
    def s = ids.join(', ')

    when: 'the method is called'
    def list2 = DomainRefListFieldFormat.instance.decode(s, fieldDef)

    then: 'the decoded list is correct'
    list2 == list
  }

  @Rollback
  def "verify that the getValidValues provides the valid values"() {
    given: 'domain records'
    DataGenerator.generate {
      domain AllFieldsDomain
      count 10
    }

    and: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    expect: 'the valid values contains the right values'
    def list = DomainRefListFieldFormat.instance.getValidValues(fieldDef)
    list.size() == 10
  }


}
