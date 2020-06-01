/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification

/**
 * Tests.
 */
class ArchiveSearchHitSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that constructor handles a search hit from the JSON map"() {
    given: 'a response as a map'
    def uuid1 = UUID.randomUUID()
    def response = [_id    : uuid1,
                    _source: [sampleParent: [class: 'sample.domain.SampleParent', _archiveReference: 'unit/ref1.arc']]]

    when: 'the result is built'
    def searchHit = new ArchiveSearchHit(response)

    then: 'the values are copied correctly'
    searchHit.uuid == uuid1
    searchHit.archiveReference == 'unit/ref1.arc'

    and: 'the empty constructor works'
    new ArchiveSearchHit() != null
  }

  def "verify that search hit provides the correct link HREF and display value for an archived domain object"() {
    given: 'a response as a map'
    def response = [_id    : UUID.randomUUID(),
                    _source: [sampleParent: [class: 'sample.domain.SampleParent', _archiveReference: 'unit/ref1.arc']]]

    when: 'the result is built'
    def hit = new ArchiveSearchHit(response)

    then: 'the link HREF is correct'
    hit.link == "/sampleParent/showArchive?ref=unit/ref1.arc"

    and: 'the display value is correct'
    // archivedObject.label=Archived {0} on file {1}
    hit.displayValue == GlobalUtils.lookup('archivedObject.label', 'SampleParent', 'unit/ref1.arc')
  }

  def "verify that isArchiveHit detects the correct cases"() {
    expect: 'the result is built'
    ArchiveSearchHit.isArchiveHit(hit) == result

    where:
    hit                                                             | result
    [_source: [sampleParent: [class: 'abc']]]                       | false
    [_source: [sampleParent: [_archiveReference: 'unit/ref1.arc']]] | true
  }
}
