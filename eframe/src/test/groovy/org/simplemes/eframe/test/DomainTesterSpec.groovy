package org.simplemes.eframe.test

import grails.gorm.transactions.Rollback
import sample.domain.SampleChild
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for the domainTester itself.
 */
class DomainTesterSpec extends BaseSpecification {

  static specNeeds = HIBERNATE

  @Rollback
  def "verify that child required values are added to the list for save properly."() {
    given: 'a child record'
    def sampleChild = new SampleChild(key: 'ABC1')

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain SampleParent
      requiredValues name: 'ABC', sampleChildren: [sampleChild]
      maxSize 'name', 40
      fieldOrderCheck false
    }
  }
}
