/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import org.apache.http.HttpHost
import org.elasticsearch.client.Request
import org.elasticsearch.client.RestClient
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.search.page.SearchIndexPage
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import sample.domain.RMA
import sample.domain.SampleParent
import spock.lang.IgnoreIf
import spock.lang.Shared

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings(["ClassSize", "UnnecessaryGetter"])
class SearchGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, RMA]


  void cleanup() {
    SearchHelper.instance = new SearchHelper()
    //SearchHelper.instance.searchEngineClient = new SearchEngineClient()
  }


  /**
   * Waits for up to 5 seconds for the given search string to return a hit.
   * This is used to let the search engine process any request we just sent.
   * @param searchString The query string to wait for a hit on.
   */
  def waitForSearchHit(String searchString) {
    for (i in 1..20) {
      if (SearchHelper.instance.globalSearch(searchString).hits.size() > 0) {
        break
      }
      standardGUISleep()
      //println "waitForSearchHit $i"
    }
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that simple search of two domains works"() {
    given: 'two domain records'
    def (SampleParent sampleParent1) = DataGenerator.generate {
      domain SampleParent
      values name: 'XYZ'
    }
    def (RMA rma1) = DataGenerator.generate {
      domain RMA
      values rma: 'ABC'
    }

    and: 'a mock client with a simulated response'
    def searchResult = new SearchResult([totalHits: 10, elapsedTime: 237, query: 'queryString237*'], [sampleParent1, rma1])
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(globalSearchResult: searchResult)

    when: 'the search page is displayed and a search is started'
    login()
    to SearchIndexPage
    searchField.value('queryString237')
    searchButton.click()
    waitFor {
      searchResultsHeader.displayed
    }

    then: 'the results are displayed'
    searchResults[0].find('a').text().contains(TypeUtils.toShortString(sampleParent1, true))
    searchResults[1].find('a').text().contains(TypeUtils.toShortString(rma1, true))

    and: 'with the correct hyper-links'
    searchResults[0].find('a').@href.contains("/sampleParent/show/${sampleParent1.uuid}")
    searchResults[1].find('a').@href.contains("/rma/show/${rma1.uuid}")

    and: 'the header is correct'
    searchResultsHeader.text() == lookup('searchResultSummary.label', null, 10, 237)
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that simple search of archived domains works"() {
    given: 'a mock client with a simulated response'
    def uuid = UUID.randomUUID()
    def searchResult = new SearchResult([took: 237, hits: [total: 10]])
    searchResult.hits << new ArchiveSearchHit([_id    : uuid.toString(), _index: 'sample-parent-arc',
                                               _source: [_archiveReference: 'unit/ref1.arc']])
    //[_source: [_archiveReference: 'unit/ref1.arc']]
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(globalSearchResult: searchResult)

    when: 'the search page is displayed and a search is started'
    login()
    to SearchIndexPage
    searchField.value('queryString237')
    searchButton.click()
    waitFor {
      searchResultsHeader.displayed
    }

    then: 'the results are displayed'
    searchResults[0].find('a').text().contains('unit/ref1.arc')

    and: 'with the correct hyper-links'
    searchResults[0].find('a').@href.contains("/sampleParent/showArchive?ref=unit/ref1.arc")
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that paging works in search"() {
    given: 'a mock client'
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient()

    and: 'some domain records'
    def objects = []
    SampleParent.withTransaction {
      for (i in 1..20) {
        objects << new SampleParent(name: "ABC $i").save()
      }
    }

    and: 'a mock client with a simulated response'
    def page0Results = []
    for (i in (0..9)) {
      page0Results << objects[i]
    }
    def searchResult = new SearchResult([totalHits: 20, elapsedTime: 237, query: 'queryString237*'], page0Results)
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(globalSearchResult: searchResult)

    when: 'the search page is displayed and a search is started'
    login()
    to SearchIndexPage, query: 'qString', offset: '0', max: '10'

    then: 'the pagination is shown'
    pagination.displayed

    and: 'the first result is a link to the first object'
    searchResults[0].find('a').@href.endsWith("sampleParent/show/${objects[0].uuid.toString()}")

    and: 'the first page is highlighted in the pagination'
    $('span.webix_pager_item_selected').text() == '1'

    when: 'the next simulated page is defined'
    def page1Results = []
    for (i in (10..19)) {
      page1Results << objects[i]
    }
    def searchResult2 = new SearchResult([totalHits: 20, elapsedTime: 237, query: 'queryString237*', from: 10, size: 10],
                                         page1Results)
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(globalSearchResult: searchResult2)

    and: 'page two is clicked'
    pagerButtons[0].click()
    waitForCompletion()

    then: 'the second page is displayed'
    $('span.webix_pager_item_selected').text() == '2'

    and: 'the first result is a link to the first object'
    searchResults[0].find('a').@href.endsWith("sampleParent/show/${objects[10].uuid.toString()}")
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that pager is visible if the page size is 20"() {
    given: 'a mock client'
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient()

    and: 'some domain records'
    def objects = []
    SampleParent.withTransaction {
      for (i in 1..20) {
        objects << new SampleParent(name: "ABC $i").save()
      }
    }

    and: 'a mock client with a simulated response'
    def page0Results = []
    for (i in (0..9)) {
      page0Results << objects[i]
    }
    def searchResult = new SearchResult([totalHits: 30, elapsedTime: 237, query: 'queryString237*'], page0Results)
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(globalSearchResult: searchResult)

    when: 'the search page is displayed and a search is started'
    login()
    to SearchIndexPage, query: 'qString', offset: '0', max: '20'

    then: 'the pagination is shown'
    pagination.displayed
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  @IgnoreIf({ !isSearchServerUp() })
  def "verify that a live server will work"() {
    given: 'the live server is used'
    SearchHelper.instance.searchEngineClient = new SearchEngineClient(hosts: [new HttpHost('localhost', 9200)])
    SearchEnginePoolExecutor.startPool()

    and: 'a domain that is searchable'
    def uniqueName = "TestName${System.currentTimeMillis()}"
    def (SampleParent sampleParent1) = DataGenerator.generate {
      domain SampleParent
      values name: uniqueName
    }

    when: 'the search page is displayed and a search is started'
    login()
    to SearchIndexPage
    searchField.value(uniqueName)

    and: 'the search engine has finished indexing the unique object.'
    waitForSearchHit(uniqueName)
    searchButton.click()
    waitFor {
      searchResultsHeader.displayed
    }

    then: 'the results are displayed'
    searchResults[0].find('a').text().contains(TypeUtils.toShortString(sampleParent1, true))

    cleanup:
    SearchEnginePoolExecutor.shutdownPool()
  }

  def "verify that red and timeout statuses are displayed"() {
    given: 'a dummy search engine, helper and hosts'
    Holders.configuration.search.hosts = [[host: 'a-host']]
    SearchHelper.instance = new SearchHelper()
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(searchStatus: new SearchStatus([status: status]))

    when: 'the search page is displayed and a search is started'
    login()
    to SearchIndexPage
    waitFor {
      searchResultsFooter.displayed
    }

    then: 'the status is displayed - localized'
    searchResultsFooter.text().contains(displayText)

    cleanup:
    Holders.configuration.search.hosts = []

    where:
    status    | displayText
    'red'     | lookup('searchStatus.red.label')
    'timeout' | lookup('searchStatus.timeout.label')

  }

}

