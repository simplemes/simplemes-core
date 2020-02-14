/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format

import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.MockFieldDefinitions
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/**
 * Tests.
 */
class DomainRefListFieldFormatSpec extends BaseSpecification {

  @SuppressWarnings("unused")
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
    def list = (List) DomainRefListFieldFormat.instance.parse("${allFieldsDomain.uuid}", null, fieldDef)

    then: 'the list contains the value'
    list[0].uuid == allFieldsDomain.uuid
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
    def list = (List) DomainRefListFieldFormat.instance.parse(allFieldsDomains*.uuid.join(','), null, fieldDef)

    then: 'the list contains the value'
    list.size() == 10
    for (afd in allFieldsDomains) {
      assert list.find { it.uuid == afd.uuid }
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
    s == list*.uuid.join(',')
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
    def ids = list*.uuid
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
      values name: 'ABC-$r'
    }

    and: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    expect: 'the valid values contains the right values'
    def list = DomainRefListFieldFormat.instance.getValidValues(fieldDef)
    list.size() == 10

    and: 'the values are sorted correctly'
    list[0].displayValue == 'ABC-001'
    list[9].displayValue == 'ABC-010'
  }

  @Rollback
  def "verify that the getValidValues sorts when there is no key field for the domain"() {
    given: 'some domain records'
    new FieldGUIExtension(domainName: 'D' + FieldGUIExtension.name).save()
    new FieldGUIExtension(domainName: 'C' + FieldGUIExtension.name).save()
    new FieldGUIExtension(domainName: 'B' + FieldGUIExtension.name).save()
    new FieldGUIExtension(domainName: 'A' + FieldGUIExtension.name).save()

    and: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([fge: FieldGUIExtension])
    def fieldDef = fieldDefinitions.fge

    and: 'the test domain has no key field'
    // If this assertion fails, then someone added a primary key to FieldGUIExtension
    // (via static keys or fieldOrder values in the class).
    assert DomainUtils.instance.getPrimaryKeyField(FieldGUIExtension) == null

    when: 'the valid values are retrieved'
    def list = DomainRefListFieldFormat.instance.getValidValues(fieldDef)

    then: 'the list is in the correct order'
    list.size() == 4

    list[0].displayValue.contains('A' + FieldGUIExtension.name)
    list[1].displayValue.contains('B' + FieldGUIExtension.name)
    list[2].displayValue.contains('C' + FieldGUIExtension.name)
    list[3].displayValue.contains('D' + FieldGUIExtension.name)
  }


}
