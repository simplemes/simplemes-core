/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format

import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import sample.domain.RMA
import sample.domain.SampleParent

/**
 * Tests.
 */
class FieldFormatFactorySpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that the factory returns the right format"() {
    expect: ''
    FieldFormatFactory.build(type) == resultClass.instance

    where:
    type                   | resultClass
    String                 | StringFieldFormat
    Integer                | IntegerFieldFormat
    int                    | IntegerFieldFormat
    Long                   | LongFieldFormat
    long                   | LongFieldFormat
    BigDecimal             | BigDecimalFieldFormat
    Boolean                | BooleanFieldFormat
    boolean                | BooleanFieldFormat
    Date                   | DateFieldFormat
    DateOnly               | DateOnlyFieldFormat
    AllFieldsDomain        | DomainReferenceFieldFormat
    ReportTimeIntervalEnum | EnumFieldFormat
    BasicStatus            | EncodedTypeFieldFormat
    Collection             | ChildListFieldFormat
    List                   | ChildListFieldFormat
    Set                    | ChildListFieldFormat
    FlexType               | ConfigurableTypeDomainFormat
  }

  def "verify that the factory returns the right format for cases with a property passed in"() {

    expect: ''
    def property = DomainUtils.instance.getPersistentField(domainClass, propertyName)
    FieldFormatFactory.build(type, property) == resultClass.instance

    where:
    type       | domainClass     | propertyName       | resultClass
    String     | AllFieldsDomain | 'name'             | StringFieldFormat
    List       | SampleParent    | 'allFieldsDomains' | DomainRefListFieldFormat
    Collection | SampleParent    | 'allFieldsDomains' | DomainRefListFieldFormat
    Set        | SampleParent    | 'allFieldsDomains' | DomainRefListFieldFormat
    List       | SampleParent    | 'sampleChildren'   | ChildListFieldFormat
    Collection | SampleParent    | 'sampleChildren'   | ChildListFieldFormat
    Set        | SampleParent    | 'sampleChildren'   | ChildListFieldFormat
    FlexType   | RMA             | 'rmaType'          | ConfigurableTypeDomainFormat
  }
}
