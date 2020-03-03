/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification
import sample.controller.SampleParentController

/**
 * Tests.
 */
class NameUtilsSpec extends BaseSpecification {

  def "test uppercaseFirstLetter with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.uppercaseFirstLetter(value) == result

    where:
    value   | result
    'a'     | 'A'
    'A'     | 'A'
    'I'     | 'I'
    'value' | 'Value'
    'Value' | 'Value'
    ''      | ''
    null    | null
  }

  def "test lowercaseFirstLetter with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.lowercaseFirstLetter(value) == result

    where:
    value   | result
    'a'     | 'a'
    'A'     | 'a'
    'I'     | 'i'
    'value' | 'value'
    'Value' | 'value'
    ''      | ''
    null    | null
  }

  def "test lowercaseFirstWord with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.lowercaseFirstWord(value) == result

    where:
    value         | result
    'LSNSequence' | 'lsnSequence'
    'Sequence'    | 'sequence'
    'sequence'    | 'sequence'
    'ABC'         | 'abc'
    'A'           | 'a'
    ''            | ''
    null          | null
  }

  def "test hasAnyLowerCase with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.hasAnyLowerCase(value) == result

    where:
    value        | result
    'URL2'       | false
    '1234'       | false
    'URL'        | false
    'propertyUp' | true
    ''           | false
    null         | false
  }

  def "test convertToHTMLID with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.convertToHTMLID(value) == result

    where:
    value             | result
    'abc'             | 'abc'
    'abc123'          | 'abc123'
    'abc 1^&&#)$()23' | 'abc123'
    'abc_123_DEF'     | 'abc_123_DEF'
    ''                | ''
    null              | null
  }

  def "test isLegalIdentifier with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.isLegalIdentifier(value) == result

    where:
    value      | result
    'abc'      | true
    'abc123'   | true
    'ABC123_$' | true
    'abc 2'    | false
    ' abc 2'   | false
    'abc+2'    | false
    'abc=2'    | false
    ''         | false
    null       | false
  }

  def "verify that toDomainName handles supported cases"() {
    expect: 'the correct domain name is returned'
    //noinspection GroovyAssignabilityCheck
    NameUtils.toDomainName(value) == result

    where:
    value                  | result
    'TheDomain'            | 'theDomain'
    'TheDomainController'  | 'theDomain'
    SampleParentController | 'sampleParent'
    null                   | null
    ''                     | null
  }

  def "test buildDisplayName with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.buildDisplayFieldNameForJSON(value) == result

    where:
    value | result
    'a'   | '_aDisplay_'
  }

  def "test isDisplayName with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.isDisplayFieldNameForJSON(value) == result

    where:
    value        | result
    '_aDisplay_' | true
    '_XYZ_'      | false
    'status'     | false
  }

  def "test convertFromColumnName with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.convertFromColumnName(value) == result

    where:
    value                  | result
    'column'               | 'column'
    'overall_status'       | 'overallStatus'
    'order_id'             | 'orderId'
    'REPORT_TIME_INTERVAL' | 'reportTimeInterval'
    ''                     | ''
    null                   | null
  }

  def "test toColumnName with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.toColumnName(fieldName) == result

    where:
    fieldName       | result
    'column'        | 'column'
    'overallStatus' | 'overall_status'
    'orderId'       | 'order_id'
    ''              | ''
  }

  def "test toLegalIdentifier with supported inputs"() {
    expect: 'the right value is returned'
    NameUtils.toLegalIdentifier(value) == result

    where:
    value | result
    'a'   | 'a'
    'a.b' | 'a_b'
    ''    | ''
    null  | null
  }


}
