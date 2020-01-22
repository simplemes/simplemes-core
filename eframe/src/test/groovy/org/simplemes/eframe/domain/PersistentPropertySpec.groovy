/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import sample.domain.Order
import spock.lang.Unroll

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

  def "verify that the field constructor works - column maxLength"() {
    given: 'a field'
    def field = Order.getDeclaredField('order')

    when: 'the constructor is used'
    def prop = new PersistentProperty(field)

    then: 'the values are correct'
    prop.name == 'order'
    prop.maxLength == 30
  }

  @Unroll
  def "verify that maxLength works for the supported cases"() {
    given: 'a domain with a field'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
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
    definition                     | maxLength
    '@Column(length = 30) String ' | 30
    '@Column String '              | 255
    'String '                      | 255
    'Integer '                     | null
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
    '@Column(nullable=true)'            | true
    '@Column(nullable=false)'           | false
    '@Nullable @Column(nullable=true)'  | true
    '@Nullable @Column(nullable=false)' | false  // When both are given, the @Column takes precedence.
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
}
