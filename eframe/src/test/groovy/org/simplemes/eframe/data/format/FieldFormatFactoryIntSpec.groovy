package org.simplemes.eframe.data.format


import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import sample.domain.SampleParent
import sample.domain.SampleSubClass

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for the Factory, with full Hibernate running.
 */
class FieldFormatFactoryIntSpec extends BaseSpecification {

  static specNeeds = [HIBERNATE]


  def "verify that the factory returns the right format for cases with a property passed in"() {
    expect: ''
    def property = DomainUtils.instance.getPersistentField(clazz, propertyName)
    FieldFormatFactory.build(property.type, property) == resultClass.instance

    where:
    clazz          | propertyName       | resultClass
    SampleSubClass | 'sampleChildren'   | ChildListFieldFormat
    SampleParent   | 'sampleChildren'   | ChildListFieldFormat
    SampleSubClass | 'allFieldsDomains' | DomainRefListFieldFormat
    SampleParent   | 'allFieldsDomains' | DomainRefListFieldFormat
  }

}
