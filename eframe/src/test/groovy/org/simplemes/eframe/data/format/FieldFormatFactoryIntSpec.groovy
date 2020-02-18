/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format


import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import sample.domain.SampleParent
import sample.domain.SampleSubClass

/**
 * Tests for the Factory.
 */
class FieldFormatFactoryIntSpec extends BaseSpecification {

  static specNeeds = [SERVER]


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
