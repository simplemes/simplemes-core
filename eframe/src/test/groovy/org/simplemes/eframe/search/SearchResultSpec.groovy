/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import org.simplemes.eframe.test.BaseSpecification

/**
 * Tests.
 */
class SearchResultSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER


  def "verify that constructor handles search results fields"() {
    given: 'a response as a map'
    def uuid1 = UUID.randomUUID()
    def uuid2 = UUID.randomUUID()
    def hits = [[_id: uuid1, _index: 'sample-parent', _source: [:]],
                [_id: uuid2, _index: 'sample-child', _source: [:]],
    ]
    def response = [took: 237, hits: [total: 247, hits: hits]]

    when: 'the result is built'
    def result = new SearchResult(response)

    then: 'the values are copied correctly'
    result.totalHits == 247
    result.elapsedTime == 237
    result.hits.size() == 2
    result.hits[0].uuid == uuid1
    result.hits[0].className == 'sample.domain.SampleParent'
    result.hits[1].uuid == uuid2
    result.hits[1].className == 'sample.domain.SampleChild'
  }

  def "verify that constructor handles search results from an archived object"() {
    given: 'a response as a map'
    def hits = [[_id    : UUID.randomUUID(), _index: 'sample-parent',
                 _source: [_archiveReference: 'unit/ref2.arc']],
    ]
    def response = [took: 237, hits: [total: 247, hits: hits]]

    when: 'the result is built'
    def result = new SearchResult(response)

    then: 'the values are copied correctly'
    result.hits.size() == 1
    result.hits[0] instanceof ArchiveSearchHit
    result.hits[0].archiveReference == 'unit/ref2.arc'
  }
}
