/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search.service

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.search.MockSearchEngineClient
import org.simplemes.eframe.search.SearchHelper
import org.simplemes.eframe.search.SearchResult
import org.simplemes.eframe.search.SearchStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.RMA
import sample.domain.SampleParent

/**
 * Tests.
 */
class SearchServiceSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static specNeeds = SERVER


  SearchService service

  def setup() {
    service = Holders.getBean(SearchService)
  }

  void cleanup() {
    // Make sure we don't leave a mock search helper in place
    SearchHelper.instance = new SearchHelper()
  }

  def "verify that globalSearch delegates to the SearchHelper"() {
    given: 'a mock helper'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    and: 'some params to pass'
    def params = [code: '1234']

    when: 'the method is called'
    def res = service.globalSearch('abc', params)

    then: 'the search helper was used with the passed in arguments with the query string adjustment made'
    1 * searchHelper.globalSearch('abc*', params) >> new SearchResult([totalHits: 237], [])

    and: 'it returned the mock result'
    res.totalHits == 237
  }

  def "verify that getStatus delegates to the SearchHelper"() {
    given: 'a mock helper'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    when: 'the method is called'
    def res = service.status

    then: 'the search helper was used'
    1 * searchHelper.status >> new SearchStatus([status: 'purple'])

    and: 'it returned the mock result'
    res.status == 'purple'
  }

  def "verify that startBulkIndexRequest delegates to the SearchHelper"() {
    given: 'a mock helper'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    when: 'the method is called'
    service.startBulkIndex(true)

    then: 'the search helper was used'
    1 * searchHelper.startBulkIndexRequest(true)
  }

  def "verify that clearStatistics delegates to the SearchHelper"() {
    given: 'a mock helper'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    when: 'the method is called'
    service.clearStatistics()

    then: 'the search helper was used'
    1 * searchHelper.clearStatistics()
  }

  def "verify that adjustQuery adjusts the query by adding the wildcard when needed."() {
    expect: 'the adjustment is made on the simple queries'
    service.adjustQuery(query, null) == result

    where:
    query        | result
    null         | null
    ''           | ''
    'abc'        | 'abc*'
    'abc*'       | 'abc*'
    'abc('       | 'abc('
    'abc"'       | 'abc"'
    'abc or xyz' | 'abc or xyz'
  }

  def "verify that domainSearch delegates to the SearchHelper"() {
    given: 'a mock helper'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    and: 'some params to pass'
    def params = [code: '1234']

    when: 'the method is called'
    def res = service.domainSearch(SampleParent, 'abc', params)

    then: 'the search helper was used with the passed in arguments with the query string adjustment made'
    1 * searchHelper.domainSearch(SampleParent, 'abc*', params) >> new SearchResult([totalHits: 237], [])
    1 * searchHelper.isDomainSearchable(SampleParent) >> true

    and: 'it returned the mock result'
    res.totalHits == 237
  }

  def "verify that domainSearch on a non-searchable domain uses the simple DB query method"() {
    given: 'a mock helper'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    and: 'some params to pass'
    def params = [code: '1234']

    when: 'the method is called'
    def res = service.domainSearch(RMA, 'abc', params)

    then: 'the mock result is returned'
    res.totalHits == 237

    then: 'the search helper was used with the passed in arguments with the query string adjustment made'
    1 * searchHelper.domainSearchInDB(RMA, 'abc', params) >> new SearchResult([totalHits: 237], [])
    1 * searchHelper.isDomainSearchable(RMA) >> false
  }

  @Rollback
  def "verify that domainSearch on a searchable domain falls back to a simple DB list when no search query is given"() {
    given: 'some domain objects for the simulate search results'
    def parents = [new SampleParent(name: 'ABC1').save(),
                   new SampleParent(name: 'ABC2').save()]

    and: 'one domain object not in the simulated results'
    new SampleParent(name: 'XYZ').save()

    and: 'a mock client with a simulated response'
    def searchResult = new SearchResult([totalHits: 10, elapsedTime: 237], parents)
    def mockSearchEngineClient = new MockSearchEngineClient(domainSearchResult: searchResult,
                                                            expectedDomainClass: SampleParent,
                                                            expectedQueryString: 'queryABC')
    SearchHelper.instance = new SearchHelper(searchEngineClient: mockSearchEngineClient)

    when: 'the search is made'
    def res = service.domainSearch(SampleParent, '')

    then: 'original query is returned'
    res.query == ''

    and: 'the results include values not provided by the mock search engine'
    res.totalHits == 3
    List hits = res.hits
    hits.size() == 3
    hits[0].object == parents[0]
    hits[1].object == parents[1]
  }

}
