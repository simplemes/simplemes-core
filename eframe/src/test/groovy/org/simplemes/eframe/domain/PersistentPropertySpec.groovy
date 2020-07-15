/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import sample.domain.Order
import sample.domain.SampleParent

/**
 * Tests.
 */
class PersistentPropertySpec extends BaseSpecification {

  def "verify that the field constructor works"() {
    given: 'a field'
    def field = Order.getDeclaredField('uuid')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.name == 'uuid'
    prop.type == UUID
    !prop.nullable
    prop.field == field
  }

  def "verify that the columnName is correct"() {
    given: 'a field'
    def field = Order.getDeclaredField(fieldName)

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the column is correct'
    prop.columnName == columnName

    where:
    fieldName | columnName
    'order'   | 'ordr'
    'status'  | 'status'
    'product' | 'product'
  }

  def "verify that the field constructor works - column maxLength"() {
    given: 'a field'
    def field = Order.getDeclaredField('order')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.name == 'order'
    prop.maxLength == 30
  }

  def "verify that maxLength works for the supported cases"() {
    given: 'a domain with a field'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import io.micronaut.data.annotation.MappedProperty
      import io.micronaut.data.model.DataType
      import javax.persistence.Column
      import javax.annotation.Nullable

      @DomainEntity
      class TestClass {
        UUID uuid
        $definition title
      }
    """
    def field = CompilerTestUtils.compileSource(src).getDeclaredField('title')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.maxLength == maxLength

    where:
    definition                                      | maxLength
    '@Column(length = 30) String '                  | 30
    '@MappedProperty(definition = "TEXT") String '  | 0
    '@MappedProperty(type = DataType.JSON) String ' | 0
    '@Column String '                               | 255
    'String '                                       | 255
    'Integer '                                      | null
  }


  def "verify that the field constructor works - nullable field"() {
    given: 'a field'
    def field = Order.getDeclaredField('product')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.type == String
    prop.nullable
  }

  def "verify that the field constructor works with supported nullable settings in both annotation types"() {
    given: 'a domain with a field'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.persistence.Column
      import javax.annotation.Nullable

      @DomainEntity
      class TestClass {
        UUID uuid
        $annotations Integer title
      }
    """
    def field = CompilerTestUtils.compileSource(src).getDeclaredField('title')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.nullable == nullable

    where:
    annotations                         | nullable
    '@Column'                           | true
    '@Column(nullable=true)'            | true
    '@Column(nullable=false)'           | false
    '@Nullable @Column(nullable=true)'  | true   // When both are given, the @Column takes precedence.
    '@Nullable @Column(nullable=false)' | false
    '@Nullable @Column'                 | true
    ''                                  | false
  }

  def "verify that the getReferencedType works for supported scenarios"() {
    given: 'a domain with a field'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.persistence.OneToMany
      import javax.persistence.ManyToOne
      import sample.domain.Order

      @DomainEntity
      class TestClass {
        UUID uuid
        $definition order
      }
    """
    def field = CompilerTestUtils.compileSource(src).getDeclaredField('order')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.referenceType == referenceType

    where:
    definition                                  | referenceType
    '@OneToMany(mappedBy="dummy") List<Order> ' | Order
    '@ManyToOne Order '                         | Order
    'Order '                                    | Order
    'String'                                    | null
  }

  def "verify that the isParentReference works for supported scenarios"() {
    given: 'a domain with a field'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.persistence.OneToMany
      import javax.persistence.ManyToOne
      import sample.domain.Order

      @DomainEntity
      class TestClass {
        UUID uuid
        $definition order
      }
    """
    def field = CompilerTestUtils.compileSource(src).getDeclaredField('order')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.parentReference == parentReference

    where:
    definition                                  | parentReference
    '@OneToMany(mappedBy="dummy") List<Order> ' | false
    '@ManyToOne Order '                         | true
    '@ManyToOne(targetEntity=Order) Order ' | false
    'Order '                                    | false
    'String'                                    | false
  }

  def "verify that the isChild works for supported scenarios"() {
    given: 'a domain with a field'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.persistence.OneToMany
      import javax.persistence.ManyToOne
      import sample.domain.Order

      @DomainEntity
      class TestClass {
        UUID uuid
        $definition order
      }
    """
    def field = CompilerTestUtils.compileSource(src).getDeclaredField('order')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.child == child

    where:
    definition                                  | child
    '@OneToMany(mappedBy="dummy") List<Order> ' | true
    '@ManyToOne Order '                         | false
    'Order '                                    | false
    'String'                                    | false
  }

  def "verify that the map constructor works"() {
    when: 'the constructor is used'
    def prop = new PersistentProperty(name: 'ABC')

    then: 'the values are correct'
    prop.name == 'ABC'
  }

  def "verify that the setters work"() {
    when: 'the setters are used'
    def prop = new PersistentProperty()
    prop.setChild(true)
    prop.setColumnName('ABC')
    prop.setType(String)
    prop.setNullable(true)
    prop.setMaxLength(137)
    prop.setField(SampleParent.getDeclaredField('title'))
    prop.setReferenceType(SampleParent)
    prop.setParentReference(true)

    then: 'the values are correct'
    prop.isChild()
    prop.getColumnName() == 'ABC'
    prop.getType() == String
    prop.isNullable()
    prop.getMaxLength() == 137
    prop.getField() == SampleParent.getDeclaredField('title')
    prop.getReferenceType() == SampleParent
    prop.isParentReference()

    and: 'toString works'
    prop.toString()
  }


}
