/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain

import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain
import sample.domain.RMA
import sample.domain.SampleChild
import sample.domain.SampleParent

/**
 * Tests.
 */
class DomainUtilsSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [User, SampleParent, AllFieldsDomain, RMA, FlexType]

  def "verify getStaticFieldOrder works with a normal domain class"() {
    expect: 'the method works'
    DomainUtils.instance.getStaticFieldOrder(SampleParent) == SampleParent.fieldOrder
  }

  def "verify getStaticFieldOrder works with sub-classes"() {
    given: 'a sub-class with its own fieldOrder'
    def src = """
    package sample
    
    import sample.domain.SampleParent
    
    class SampleClass extends SampleParent {
      static fieldOrder = ['customField']
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    expect: 'the combined fieldOrder is found'
    DomainUtils.instance.getStaticFieldOrder(clazz) == SampleParent.fieldOrder + 'customField'
  }

  def "verify that getPersistentFields finds the right fields"() {
    when: 'the persistent fields are found'
    def fields = DomainUtils.instance.getPersistentFields(SampleParent)

    then: 'all of the expected fields are found'
    def names = fields*.name
    names.containsAll(['name', 'title', 'notes', 'dateCreated', 'dateUpdated'])

  }

  def "verify that getAllDomains works"() {
    when: 'the domains are found'
    def all = DomainUtils.instance.allDomains

    then: 'the known domains are in the list'
    all.contains(User)
    all.contains(Role)
  }

  def "verify that getPrimarySortField returns the first field in the fieldOrder"() {
    expect: ''
    DomainUtils.instance.getPrimaryKeyField(SampleParent) == 'name'
  }

  def "verify that getKeyFields works with support types of domain configurations"() {
    expect: ''
    DomainUtils.instance.getKeyFields(domain) == res

    where:
    domain       | res
    User         | ['userName']
    SampleParent | ['name']
    String       | []
  }

  def "verify that getKeyFields works with specified keys in domain"() {
    given: 'a domain with a fieldOrder and specific keys'
    def src = """
    package sample
    
    class SampleClass {
      static keys = ['aKey']
      static fieldOrder = ['name', 'title']
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    expect: ''
    DomainUtils.instance.getKeyFields(clazz) == ['aKey']
  }

  def "verify that getFieldType works in domain"() {
    given: 'a simple domain '
    def src = """
    package sample
    
    class SampleClass {
      Integer count
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    expect: ''
    DomainUtils.instance.getFieldType(clazz, 'count') == Integer
  }

  def "verify that getFieldDefinitions works for domain class"() {
    when: 'the fields are retrieved'
    def fieldDefs = DomainUtils.instance.getFieldDefinitions(AllFieldsDomain)

    then: 'the fields are correct'
    fieldDefs['name'].type == String
    fieldDefs['title'].type == String
    fieldDefs['count'].type == Integer
    fieldDefs['qty'].type == BigDecimal
    fieldDefs['enabled'].type == Boolean
    fieldDefs['dateTime'].type == Date
    fieldDefs['transientField'].type == String
  }

  def "verify that getFieldDefinitions skips the reference to another domain ID field"() {
    when: 'the fields are retrieved'
    def fieldDefs = DomainUtils.instance.getFieldDefinitions(SampleParent)

    then: 'the foreign reference ID field is not in the list'
    !fieldDefs['allFieldsDomainId']
  }

  def "verify that getFieldDefinitions skips the custom field definitions"() {
    when: 'the fields are retrieved'
    def fieldDefs = DomainUtils.instance.getFieldDefinitions(SampleParent)

    then: 'the complex custom data holder field is not in the list'
    !fieldDefs[ExtensibleFieldHolder.COMPLEX_CUSTOM_FIELD_NAME]

    and: 'the custom data holder field is not in the list'
    !fieldDefs['customFields']
  }

  def "verify that getFieldDefinitions works for POGO class"() {
    given: 'a simple POGO with several field types'
    def src = """
    package sample
    class SampleClass {
      String name 
      String title
      BigDecimal qty
      Integer count
      Boolean enabled
      Date dateTime
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the fields are retrieved'
    def fieldDefs = DomainUtils.instance.getFieldDefinitions(clazz)

    then: 'the fields are correct'
    fieldDefs['name'].type == String
    fieldDefs['title'].type == String
    fieldDefs['count'].type == Integer
    fieldDefs['qty'].type == BigDecimal
    fieldDefs['enabled'].type == Boolean
    fieldDefs['dateTime'].type == Date
  }

  def "verify that getFieldDefinitions works fast enough for POGO class"() {
    given: 'a simple POGO with several field types'
    def src = """
    package sample
    class SampleClass {
      String name 
      String title
      BigDecimal qty
      Integer count
      Boolean enabled
      Date dateTime
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the fields are retrieved a number of times'
    def start = System.currentTimeMillis()
    for (i in 1.100) {
      DomainUtils.instance.getFieldDefinitions(clazz)
    }

    then: 'the time per iteration is reasonable'
    def elapsed = System.currentTimeMillis() - start
    elapsed / 100.0 < 1.0
  }

  @Rollback
  def "verify that getValidationMessages can build a message from a validation error"() {
    given: 'a domain record with multiple errors.'
    def badRecord = new AllFieldsDomain(name: 'ABC', count: 1000000)

    when: 'the errors are converted to messages'
    def msg = DomainUtils.instance.getValidationMessages(badRecord)
    //println "msg = $msg"

    then: 'the message holder has the message'
    msg.otherMessages == null

    // Just check 3 digits to avoid thousands separator issues.
    UnitTestUtils.assertContainsAllIgnoreCase(msg.text, ['count', '000', '999'])
  }

  @Rollback
  def "verify that getValidationMessages can handle multiple validation errors"() {
    given: 'a domain record with multiple errors.'
    def badRecord = new AllFieldsDomain(name: 'ABC', qty: 1000000.0, count: 1000000)

    when: 'the errors are converted to messages'
    def msg = DomainUtils.instance.getValidationMessages(badRecord)
    //println "msg = $msg"

    then: 'the message holder has all of the messages'
    msg.otherMessages.size() == 1
    msg.text != null

    msg.toString().contains('qty')
    msg.toString().contains('count')
  }

  @Rollback
  def "verify that findDomainRecord finds the record by UUID"() {
    given: 'a domain record with several children added the wrong way - simple insert to list'
    def sampleParent = new SampleParent(name: 'ABC').save()

    when: 'the record is found'
    def sampleParent2 = DomainUtils.instance.findDomainRecord(SampleParent, sampleParent.uuid)

    then: 'the record is found correctly'
    sampleParent2 == sampleParent
  }

  @Rollback
  def "verify that findDomainRecord finds the record by UUID - String input"() {
    given: 'a domain record with several children added the wrong way - simple insert to list'
    def sampleParent = new SampleParent(name: 'ABC').save()

    when: 'the record is found'
    def sampleParent2 = DomainUtils.instance.findDomainRecord(SampleParent, sampleParent.uuid.toString())

    then: 'the record is found correctly'
    sampleParent2 == sampleParent
  }

  @Rollback
  def "verify that findDomainRecord finds the record by key"() {
    given: 'a domain record with several children added the wrong way - simple insert to list'
    def sampleParent = new SampleParent(name: 'ABC').save()

    when: 'the record is found'
    def sampleParent2 = DomainUtils.instance.findDomainRecord(SampleParent, sampleParent.name)

    then: 'the record is found correctly'
    sampleParent2 == sampleParent
  }

  @Rollback
  def "verify that findDomainRecord gracefully fails when argument is null"() {
    expect:
    def n = null
    DomainUtils.instance.findDomainRecord(SampleParent, (UUID) n) == null
    DomainUtils.instance.findDomainRecord(SampleParent, n as String) == null
  }

  @Rollback
  def "verify that findDomainRecord gracefully fails when ID is not found"() {
    when: 'the record is found'
    def sampleParent2 = DomainUtils.instance.findDomainRecord(SampleParent, 'gibberish')

    then: 'the record is not found with no exception'
    sampleParent2 == null
  }

  @Rollback
  def "verify that findRelatedRecords works for a domain with the method implemented"() {
    given: 'a domain with some related files - an AllFieldsDomain with the same key field'
    def afd = new AllFieldsDomain(name: 'SAMPLE').save()
    def sampleParent = new SampleParent(name: 'SAMPLE', title: 'Sample').save()

    when: 'the related records are searched'
    def list = DomainUtils.instance.findRelatedRecords(sampleParent)

    then: 'the related record is in the list'
    list.contains(afd)
  }

  @Rollback
  def "verify that findRelatedRecords works for a domain without the method implemented"() {
    given: 'a domain with some related files - an AllFieldsDomain with the same key field'
    def afd = new AllFieldsDomain(name: 'SAMPLE').save()

    when: 'the related records are searched'
    def list = DomainUtils.instance.findRelatedRecords(afd)

    then: 'no records are found'
    !list
  }

  def "verify that isDomainEntity works with supported domain entities"() {
    expect: 'the entity is detected'
    AllFieldsDomain.withTransaction {
      assert DomainUtils.instance.isDomainEntity(clazz) == results
      true
    }

    where:
    clazz           | results
    String          | false
    null            | false
    AllFieldsDomain | true
  }

  def "verify that getDomain works for supported cases"() {
    expect: 'the method works'
    DomainUtils.instance.getDomain(name) == clazz

    where:
    name           | clazz
    'sampleParent' | SampleParent
    'SampleParent' | SampleParent
    'SAMPLEParent' | SampleParent
  }

  @Rollback
  def "verify that loadChildRecords loads the child records"() {
    given: 'a domain record with several children'
    def sampleParent = new SampleParent(name: 'ABC-021')
    sampleParent.sampleChildren << (new SampleChild(key: 'C1'))
    sampleParent.sampleChildren << (new SampleChild(key: 'C2'))
    sampleParent.save()

    when: 'the record is read without a loaded child list'
    def sampleParent1 = SampleParent.findByUuid(sampleParent.uuid)

    then: 'the children records are not loaded yet'
    def field = SampleParent.getDeclaredField('sampleChildren')
    field.setAccessible(true)
    field.get(sampleParent1) == null

    when: 'the load method is called with an unloaded child list'
    DomainUtils.instance.loadChildRecords(sampleParent1)

    then: 'the list is loaded'
    def list = field.get(sampleParent1)
    list.size() == 2
  }

  def "verify getURIRoot works for supported name styles"() {
    expect: 'the method works'
    DomainUtils.instance.getURIRoot(domainClass) == result

    where:
    domainClass  | result
    SampleParent | 'sampleParent'
    RMA          | 'rma'
  }

  def "verify getParentFieldName works for supported name styles"() {
    expect: 'the method works'
    DomainUtils.instance.getParentFieldName(domainClass) == result

    where:
    domainClass | result
    SampleChild | 'sampleParent'
    RMA         | null
  }

}
