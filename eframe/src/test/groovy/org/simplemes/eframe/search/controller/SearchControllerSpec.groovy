/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.search.SearchResult
import org.simplemes.eframe.search.SearchStatus
import org.simplemes.eframe.search.service.SearchService
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockPrincipal

/**
 * Tests.
 */
class SearchControllerSpec extends BaseAPISpecification {
  @SuppressWarnings('unused')
  static specNeeds = SERVER

  SearchController controller

  def setup() {
    controller = new SearchController()
  }

  def "verify that the controller passes the standard controller test - security, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller SearchController
      secured 'admin', 'ADMIN'
      secured 'status', 'ADMIN'
      secured 'startBulkIndex', 'ADMIN'
      secured 'clearStatistics', 'ADMIN'
      taskMenu name: 'globalSearch', uri: '/search', clientRootActivity: true, folder: 'search:6000', displayOrder: 6010
      taskMenu name: 'searchAdmin', uri: '/search/admin', clientRootActivity: true, folder: 'admin:7000', displayOrder: 7030
    }
  }

  def "verify that the index method calls the right SearchService method and handles the results correctly"() {
    given: "a mocked service"
    def searchService = Mock(SearchService)
    controller.searchService = searchService
    1 * searchService.globalSearch('qString', _) >> new SearchResult([totalHits: 237, elapsedTime: 137], [])

    when: "the search is executed"
    def res = controller.index(mockRequest([query: 'qString']), new MockPrincipal())
    def model = res.model.get()

    then: 'the correct model is used'
    model.searchResult.totalHits == 237
    model.searchResult.elapsedTime == 137

    and: 'the correct view is rendered'
    res.view.get() == 'search/index'
  }

  def "verify that index calls supports the from and size options"() {
    given: "a mocked service"
    def searchService = Mock(SearchService)
    controller.searchService = searchService
    1 * searchService.globalSearch('qString', _) >> new SearchResult([totalHits: 237, elapsedTime: 137, from: 12, size: 13], [])

    when: "the search is executed"
    def res = controller.index(mockRequest([query: 'qString', from: 12, size: 13]), new MockPrincipal())
    def model = res.model.get()

    then: "the right view and model are used"
    model.searchResult.from == 12
    model.searchResult.size == 13
  }

  def "verify that admin provides the current search status"() {
    given: "a mocked service with a fixed status"
    def status = new SearchStatus()
    status.pendingRequests = 237
    status.bulkIndexStatus = 'unknown'
    def searchService = Mock(SearchService)
    controller.searchService = searchService
    1 * searchService.status >> status

    when: "the search is executed"
    def res = controller.admin(new MockPrincipal())
    def model = res.model.get()

    then: "the right view and model are used"
    model.searchStatus.pendingRequests == 237
    model.searchStatus.bulkIndexStatus == 'unknown'
    res.view.get() == 'search/admin'
  }

  def "verify that status method provides the current search status"() {
    given: "a mocked service with a fixed status"
    def status = new SearchStatus()
    status.pendingRequests = 237
    status.bulkIndexStatus = 'unknown'
    def searchService = Mock(SearchService)
    controller.searchService = searchService
    1 * searchService.status >> status

    when: "the search is executed"
    def res = controller.status(new MockPrincipal())

    then: "the response is correct"
    res.pendingRequests == 237
    res.bulkIndexStatus == 'unknown'
  }

  def "verify that status method provides the current search status - as JSON in a live server"() {
    given: "a mocked service with a fixed status"
    def status = new SearchStatus()
    status.pendingRequests = 237
    status.bulkIndexStatus = 'unknown'
    def searchService = Mock(SearchService)
    controller = Holders.getBean(SearchController)
    def originalService = controller.searchService
    controller.searchService = searchService
    1 * searchService.status >> status

    when: "the search is executed"
    login()
    def res = sendRequest(uri: '/search/status', locale: Locale.US)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"
    def json = new JsonSlurper().parse(res.bytes)

    then: 'the response contains the status values'
    json.pendingRequests == 237
    json.bulkIndexStatus == 'unknown'

    cleanup:
    controller.searchService = originalService
  }

  def "verify that status method localizes the status - in a live server"() {
    given: "a mocked service with a fixed status"
    def status = new SearchStatus()
    status.pendingRequests = 237
    status.bulkIndexStatus = 'unknown'
    status.status = 'yellow'
    def searchService = Mock(SearchService)
    controller = Holders.getBean(SearchController)
    def originalService = controller.searchService
    controller.searchService = searchService
    1 * searchService.status >> status

    when: "the search is executed"
    login()
    def res = sendRequest(uri: '/search/status', locale: locale)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"
    def json = new JsonSlurper().parse(res.bytes)

    then: 'the response contains the localized status values'
    json.localizedBulkIndexStatus == GlobalUtils.lookup('searchStatus.unknown.label', locale)
    json.localizedStatus == GlobalUtils.lookup('searchStatus.yellow.label', locale)

    cleanup:
    controller.searchService = originalService

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that the global exception handler works in  alive server"() {
    given: "a mocked service with a fixed status"
    def status = new SearchStatus()
    status.pendingRequests = 237
    status.bulkIndexStatus = 'unknown'
    status.status = 'green'
    controller = Holders.getBean(SearchController)
    def searchService = Mock(SearchService)
    def originalService = controller.searchService
    controller.searchService = searchService
    1 * searchService.status >> { throw new IllegalArgumentException('bad mojo') }

    and: 'no logging of stack trace'
    disableStackTraceLogging()

    when: "the search is executed"
    waitForInitialDataLoad()
    login()
    def res = sendRequest(uri: '/search/status', locale: Locale.US, status: HttpStatus.BAD_REQUEST)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"
    def json = new JsonSlurper().parse(res.bytes)

    then: 'the response contains the exception'
    json.message.text.contains('bad mojo')

    cleanup:
    controller.searchService = originalService
  }

  def "verify that startBulkIndex starts the rebuild - live server"() {
    given: "a mocked service with a fixed status"
    def searchService = Mock(SearchService)
    controller = Holders.getBean(SearchController)
    def originalService = controller.searchService
    controller.searchService = searchService

    and: 'the flag is set'
    def json = """{"deleteAllIndices": $deleteAllIndices}"""

    when: "the method is executed"
    login()
    sendRequest(uri: '/search/startBulkIndex', method: 'post', content: json)

    then: "service was called correctly"
    1 * searchService.startBulkIndex(deleteAllIndices)

    cleanup:
    controller.searchService = originalService

    where:
    deleteAllIndices | _
    true             | _
    false            | _
  }

  def "verify that clearStatistics works - in a live server"() {
    given: "a mocked service with a fixed status"
    def searchService = Mock(SearchService)
    controller = Holders.getBean(SearchController)
    def originalService = controller.searchService
    controller.searchService = searchService

    when: "the method is executed"
    login()
    sendRequest(uri: '/search/clearStatistics', method: 'post', content: '')

    then: 'the stats are reset'
    1 * searchService.clearStatistics()

    cleanup:
    controller.searchService = originalService
  }

}
