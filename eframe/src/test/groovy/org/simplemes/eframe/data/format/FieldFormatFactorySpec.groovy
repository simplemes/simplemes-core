package org.simplemes.eframe.data.format


import org.grails.datastore.mapping.model.types.Association
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import sample.domain.SampleChild
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class FieldFormatFactorySpec extends BaseSpecification {

  def "verify that the factory returns the right format"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, AllFieldsDomain).install()

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
    given: 'a mocked domain utils'
    new MockDomainUtils(this, [AllFieldsDomain, SampleParent, SampleChild]).install()

    and: 'a mocked persistent property'
    def property = Mock(Association)
    property.isOwningSide() >> { owningSide }


    expect: ''
    FieldFormatFactory.build(type, property) == resultClass.instance

    where:
    type       | owningSide | resultClass
    String     | false      | StringFieldFormat
    String     | true       | StringFieldFormat
    List       | false      | DomainRefListFieldFormat
    Set        | false      | DomainRefListFieldFormat
    Collection | false      | DomainRefListFieldFormat
    List       | true       | ChildListFieldFormat
    Set        | true       | ChildListFieldFormat
    Collection | true       | ChildListFieldFormat
    FlexType   | false      | ConfigurableTypeDomainFormat
  }
}
