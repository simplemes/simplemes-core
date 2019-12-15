package org.simplemes.eframe.data.format


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
class DomainReferenceFieldFormatSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, AllFieldsDomain]

  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    DomainReferenceFieldFormat.instance.id == DomainReferenceFieldFormat.ID
    DomainReferenceFieldFormat.instance.toString() == 'DomainRef'
    DomainReferenceFieldFormat.instance.type == Object
    BasicFieldFormat.coreValues.contains(DomainReferenceFieldFormat)
  }

  def "verify that basic formatting works"() {
    expect:
    DomainReferenceFieldFormat.instance.format(value, Locale.US, null) == result

    where:
    value                         | result
    null                          | ''
    new SampleParent(name: 'ABC') | 'ABC'
  }

  def "verify that the parseForm method works within a transaction"() {
    given: 'a domain record'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
    }

    and: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    expect: 'the reference can be found and returns as a proxy that can be used in another domain'
    AllFieldsDomain.withTransaction {
      def res = DomainReferenceFieldFormat.instance.parseForm(allFieldsDomain.id.toString(), Locale.US, fieldDef)
      assert res.id == allFieldsDomain.id
      //noinspection GroovyAssignabilityCheck
      assert new SampleParent(name: 'XYZ', allFieldsDomain: res).save()
      true
    }
  }

  def "verify that the parseForm method works within null ID"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    expect: 'the reference can be found and returns as a proxy that can be used in another domain'
    AllFieldsDomain.withTransaction {
      def res = DomainReferenceFieldFormat.instance.parseForm(null, Locale.US, fieldDef)
      assert res == null
      true
    }
  }

  def "verify that the encode methods works"() {
    expect:
    DomainReferenceFieldFormat.instance.encode(value, null) == result

    where:
    value                                  | result
    null                                   | null
    new SampleParent(name: 'ABC', id: 237) | '237'
  }

  def "verify that the decode methods finds the correct domain record"() {
    given: 'a domain record'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
    }

    and: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    expect: 'the reference can be found and returns as a proxy that can be used in another domain'
    AllFieldsDomain.withTransaction {
      def res = DomainReferenceFieldFormat.instance.decode(allFieldsDomain.id.toString(), fieldDef)
      assert res.id == allFieldsDomain.id
      //noinspection GroovyAssignabilityCheck
      assert new SampleParent(name: 'XYZ', allFieldsDomain: res).save()
      true
    }
  }

  def "verify that the getGridEditor returns the right editor"() {
    given: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    expect: 'the method is called'
    DomainReferenceFieldFormat.instance.getGridEditor(fieldDef) == 'combo'
  }

  //TODO: Find alternative to @Rollback
  def "verify that the getValidValues provides the valid values"() {
    given: 'a domain record'
    DataGenerator.generate {
      domain AllFieldsDomain
      count 10
    }

    and: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    expect: 'the valid values contains the right values'
    def list = DomainReferenceFieldFormat.instance.getValidValues(fieldDef)
    list.size() == 10

    list[0].value instanceof AllFieldsDomain
  }

  def "verify that the convertToJsonFormat works"() {
    given: 'a domain record'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
    }

    and: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    expect: 'the conversion works'
    DomainReferenceFieldFormat.instance.convertToJsonFormat(allFieldsDomain, fieldDef) == allFieldsDomain.id.toString()
  }

  def "verify that the convertFromJsonFormat works"() {
    given: 'a domain record'
    def (AllFieldsDomain allFieldsDomain) = DataGenerator.generate {
      domain AllFieldsDomain
    }

    and: 'a field definition'
    def fieldDefinitions = new MockFieldDefinitions([allFieldsDomain: AllFieldsDomain])
    def fieldDef = fieldDefinitions.allFieldsDomain

    expect: 'the conversion works'
    if (value == 'id') {
      // Since we can't use the variable allFieldsDomain above in the where clause, we substitute it here.
      value = allFieldsDomain.id.toString()
      result = allFieldsDomain
    }
    // Even if not in a database transaction
    DomainReferenceFieldFormat.instance.convertFromJsonFormat(value, fieldDef) == result

    where:
    value | result
    'id'  | 'id'
    ''    | null
    null  | null
  }

}
