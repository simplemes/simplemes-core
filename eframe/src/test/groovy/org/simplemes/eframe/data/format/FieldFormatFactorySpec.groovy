/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format

import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
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
    FlexType               | DomainReferenceFieldFormat  // With no declaring class, Configurable Types are treated as domain refs
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

  def "verify that the factory returns the right format a configurable type field without extensible field holder in the class"() {
    given: 'a domain with a flex type reference but no extensible field holder'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.custom.domain.FlexType
      import io.micronaut.data.model.DataType
      import io.micronaut.data.annotation.MappedProperty
      import javax.annotation.Nullable
      import javax.persistence.ManyToOne
      
      @DomainEntity(repository=sample.domain.OrderRepository)
      class TestClass {
        UUID uuid

        @Nullable
        @ManyToOne(targetEntity = FlexType)
        @MappedProperty(type = DataType.UUID)
        FlexType rmaType
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    expect: 'the field type is simple reference'
    def property = DomainUtils.instance.getPersistentField(clazz, 'rmaType')
    FieldFormatFactory.build(FlexType, property) == DomainReferenceFieldFormat.instance
  }
}
