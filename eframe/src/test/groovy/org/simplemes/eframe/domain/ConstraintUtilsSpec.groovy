package org.simplemes.eframe.domain

import org.simplemes.eframe.test.BaseSpecification
import sample.domain.AllFieldsDomain

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ConstraintUtilsSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain]

  def "verify that getPropertyMaxSize works on a real domain with various properties"() {
    given: 'the domain property'
    def prop = AllFieldsDomain.gormPersistentEntity.persistentProperties.find { it.name == propertyName }

    expect: 'the maxSize is found'
    ConstraintUtils.instance.getPropertyMaxSize(prop) == maxSize

    where:
    propertyName     | maxSize
    'name'           | 40
    'title'          | 20
    'qty'            | null
    'transientField' | null
    null             | null
  }

  def "verify that getPropertyScale works on a real domain"() {
    given: 'the domain property'
    def prop = AllFieldsDomain.gormPersistentEntity.persistentProperties.find { it.name == propertyName }

    expect: 'the scale is found'
    ConstraintUtils.instance.getPropertyScale(prop) == scale

    where:
    propertyName | scale
    'qty'        | 4
    'name'       | null
  }

  def "verify that getProperty works on a real domain"() {
    given: 'the domain property'
    def prop = AllFieldsDomain.gormPersistentEntity.persistentProperties.find { it.name == propertyName }

    expect: 'the scale is found'
    ConstraintUtils.instance.getProperty(prop, constraintName) == value

    where:
    propertyName | constraintName | value
    'title'      | 'nullable'     | true
    'name'       | 'nullable'     | false
    null         | 'nullable'     | null
  }
}
