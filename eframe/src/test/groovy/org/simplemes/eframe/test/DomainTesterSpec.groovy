/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.SampleChild
import sample.domain.SampleParent

/**
 * Tests for the domainTester itself.
 */
class DomainTesterSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "verify that child required values are added to the list for save properly."() {
    given: 'a child record'
    def sampleChild = new SampleChild(key: 'ABC1')

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain SampleParent
      requiredValues name: 'ABC', sampleChildren: [sampleChild]
      maxSize 'name', 40
      notInFieldOrder(['notDisplayed'])
      //fieldOrderCheck true
    }
  }
}
