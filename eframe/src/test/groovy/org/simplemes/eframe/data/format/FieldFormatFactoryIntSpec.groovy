/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format

import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import sample.domain.SampleParent

/**
 * Tests for the Factory.
 */
class FieldFormatFactoryIntSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static specNeeds = [SERVER]


  def "verify that the factory returns the right format for cases with a property passed in"() {
    expect: ''
    def property = DomainUtils.instance.getPersistentField(clazz, propertyName)
    FieldFormatFactory.build(property.type, property) == resultClass.instance

    where:
    clazz          | propertyName       | resultClass
    SampleParent   | 'sampleChildren'   | ChildListFieldFormat
    SampleParent   | 'allFieldsDomains' | DomainRefListFieldFormat
  }

}
