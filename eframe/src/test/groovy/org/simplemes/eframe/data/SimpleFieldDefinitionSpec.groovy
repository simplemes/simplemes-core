/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data

import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import sample.domain.AllFieldsDomain
import sample.domain.SampleChild
import sample.domain.SampleParent
import sample.domain.SampleSubClass

/**
 * Tests.
 */
class SimpleFieldDefinitionSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  def "verify that persistent property constructor works"() {
    when: 'the constructor is used'
    def field = new SimpleFieldDefinition(DomainUtils.instance.getPersistentField(AllFieldsDomain, 'qty'))

    then: 'the correct field is created'
    field.name == 'qty'
    field.type == BigDecimal
    field.label == 'qty.label'
  }

  def "verify that Map constructor works"() {
    when: 'the constructor is used'
    def field = new SimpleFieldDefinition(name: 'title', type: Integer)

    then: 'the correct field is created'
    field.name == 'title'
    field.type == Integer
    field.label == 'title.label'
  }

  def "verify that isReference works for supported field types"() {
    expect: 'the isReference method works'
    new SimpleFieldDefinition(DomainUtils.instance.getPersistentField(clazz, name)).reference == results

    where:
    clazz        | name               | results
    SampleParent | 'title'            | false
    SampleParent | 'allFieldsDomain'  | true
    SampleParent | 'allFieldsDomains' | true
    SampleParent | 'sampleChildren'   | true
  }

  def "verify that getFieldFormat works child list"() {
    expect: 'the isReference method works'
    new SimpleFieldDefinition(DomainUtils.instance.getPersistentField(clazz, name)).format == results.instance

    where:
    clazz        | name             | results
    //SampleParent | 'allFieldsDomains' |
    SampleParent | 'sampleChildren' | ChildListFieldFormat
  }

  def "verify that getType works for supported field types"() {
    expect: 'the isReference method works'
    new SimpleFieldDefinition(DomainUtils.instance.getPersistentField(clazz, name)).type == results

    where:
    clazz        | name               | results
    SampleParent | 'title'            | String
    SampleParent | 'allFieldsDomain'  | AllFieldsDomain
    SampleParent | 'allFieldsDomains' | List
    SampleParent | 'sampleChildren'   | List
  }

  def "verify that getReferenceType works for supported field types"() {
    expect: 'the isReference method works'
    new SimpleFieldDefinition(DomainUtils.instance.getPersistentField(clazz, name)).referenceType == results

    where:
    clazz        | name               | results
    SampleParent | 'title'            | null
    SampleParent | 'allFieldsDomain'  | AllFieldsDomain
    SampleParent | 'allFieldsDomains' | AllFieldsDomain
    SampleParent | 'sampleChildren'   | SampleChild
  }

  def "verify that isChild works for supported field types"() {
    expect: 'the isChild method works'
    new SimpleFieldDefinition(DomainUtils.instance.getPersistentField(clazz, name)).child == results

    where:
    clazz          | name               | results
    SampleParent   | 'title'            | false
    SampleParent   | 'allFieldsDomain'  | false
    SampleParent   | 'allFieldsDomains' | false
    SampleParent   | 'sampleChildren'   | true
    SampleSubClass | 'sampleChildren'   | true
    SampleSubClass | 'allFieldsDomains' | false
  }

  def "verify that isParentReference works for supported field types"() {
    expect: 'the isParentReference method works'
    new SimpleFieldDefinition(DomainUtils.instance.getPersistentField(clazz, name)).parentReference == results

    where:
    clazz        | name               | results
    SampleChild  | 'sampleParent'     | true
    SampleParent | 'title'            | false
    SampleParent | 'allFieldsDomain'  | false
    SampleParent | 'allFieldsDomains' | false
    SampleParent | 'sampleChildren'   | false
  }

  def "verify that isPrimaryUuid works for supported field types"() {
    expect: 'the isParentReference method works'
    new SimpleFieldDefinition(DomainUtils.instance.getPersistentField(clazz, name)).primaryUuid == results

    where:
    clazz        | name           | results
    SampleChild  | 'uuid'         | true
    SampleChild  | 'sampleParent' | false
    SampleParent | 'title'        | false
  }

  def "verify that maxLength is set correctly"() {
    expect: 'the method works'
    new SimpleFieldDefinition(DomainUtils.instance.getPersistentField(clazz, name)).maxLength == results

    where:
    clazz           | name    | results
    SampleParent    | 'title' | 20
    AllFieldsDomain | 'qty'   | null
  }

  def "verify that required is set correctly for domain fields"() {
    expect: 'the method works'
    new SimpleFieldDefinition(DomainUtils.instance.getPersistentField(clazz, name)).required == results

    where:
    clazz           | name        | results
    SampleParent    | 'name'      | true
    SampleParent    | 'title'     | false
    AllFieldsDomain | 'qty'       | false
    DashboardConfig | 'dashboard' | true
    DashboardConfig | 'category'  | true
  }

  def "verify that getFieldValue works on a core field correctly"() {
    given: 'a field def and an object to get the value from'
    def fieldDefinition = new SimpleFieldDefinition(name: 'qty', format: StringFieldFormat.instance)
    def obj = new AllFieldsDomain(qty: 1.2)

    expect: 'the method works'
    fieldDefinition.getFieldValue(obj) == 1.2
  }

  def "verify that getFieldValue handles on a null domain object gracefully"() {
    given: 'a field def and an object to get the value from'
    def fieldDefinition = new SimpleFieldDefinition(name: 'qty', format: StringFieldFormat.instance)

    expect: 'the method works'
    fieldDefinition.getFieldValue(null) == null
  }

  def "verify that setFieldValue works on a core field correctly"() {
    given: 'a field def and an object to get the value from'
    def fieldDefinition = new SimpleFieldDefinition(name: 'title')
    def obj = new AllFieldsDomain()

    when: 'the field is set'
    fieldDefinition.setFieldValue(obj, 'abc')

    then: 'the value was set'
    obj.title == 'abc'
  }

}
