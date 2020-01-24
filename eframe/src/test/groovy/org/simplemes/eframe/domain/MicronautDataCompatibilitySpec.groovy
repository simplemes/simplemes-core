/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain


import org.simplemes.eframe.test.BaseSpecification
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/**
 * Tests for Micronaut Data compatibility.  Tests basic behaviors that might break
 * for new releases.
 */
class MicronautDataCompatibilitySpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that simple domain references can be updated"() {
    given: 'a domain with a reference'
    def afd1 = new AllFieldsDomain(name: 'ABC-01', title: 'orig').save()
    def p = new SampleParent(name: 'SAMPLE', title: 'Sample', allFieldsDomain: afd1)
    p.save()

    when: 'the record is changed and saved'
    p.title = 'new'
    p.save()

    then: 'the value is correct in the DB'
    def sampleParent2 = SampleParent.findByUuid(p.uuid)
    sampleParent2.title == 'new'
    sampleParent2.allFieldsDomain == afd1
  }


}
