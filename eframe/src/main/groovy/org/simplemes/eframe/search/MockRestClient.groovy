/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search


import org.apache.http.HttpEntity
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.Request
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.UUIDUtils

/**
 * This is a mock client that can simulate the external search engine's response to various requests.
 * This is not suitable for production.
 * <p>
 * The options for this mock generally define the expected inputs to the performRequest() method.
 * The values used for the generated response is stored in the <code>response</code> Map argument.
 * <p>
 * <b>Note:</b> The <code>response</code> argument is used to create the response.  Your test should verify that it is returned correctly.
 * <p>
 * <h3>Example - getStatus()</h3>
 * <pre>
 * <b>given:</b> 'a search engine client with a mock rest client'
 * def mockRestClient = new MockRestClient(method: 'GET', uri: "/_cluster/health",
 *                                         response: [status:'unknown'])
 * def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)
 * </pre>
 *
 * <h3>Example - indexObject()</h3>
 * <pre>
 * <b>given:</b> 'a search engine client with a mock rest client'
 * def object = new ...
 * def mockRestClient = new MockRestClient(method: 'PUT', uri: SearchEngineClient.buildURIForIndexRequest(object),
 *                                         content: object, response: [result:'created', id: object.id])
 * def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)
 * </pre>
 *
 * <h3>Example - globalSearch()</h3>
 * <pre>
 * <b>given:</b> 'a search engine client with a mock rest client'
 * def mockRestClient = new MockRestClient(method: 'GET', uri: '/_search?q=abc*',_index:'sample-parent',
 *                                         response: [hits: [[code: 'abc1'], [code: 'abc2']]])
 * def searchEngineClient = new SearchEngineClient(restClient: mockRestClient)
 * </pre>
 */
class MockRestClient {

  /**
   * The expected HTTP method to use for the simulated request.
   */
  String method = 'GET'

  /**
   * The expected URI the simulated request should be called with.  This is enforced if provided.
   */
  String uri

  /**
   * Added to the indexName in the simulated response.
   */
  String indexSuffix = ''

  /**
   * The expected JSON content for the simulated request.  This is enforced if provided.
   */
  Object content

  /**
   * Values to be used for the generated response.
   */
  Map response

  /**
   * The simulated request method.
   * @return The simulated response.
   */
  @SuppressWarnings(["GroovyUnusedDeclaration"])
  Object performRequest(Request request) {
    // String method, String endpoint, Map<String, String> params, HttpEntity entity, Header[] headers) {
    def expectedMethod = this.method
    assert request.method == expectedMethod

    if (this.uri) {
      def expectedURI = this.uri
      assert request.endpoint == expectedURI
    }

    switch (request.method) {
      case 'PUT':
        return simulatePut(request.endpoint, request.entity)
      case 'POST':
        return simulatePost(request.endpoint, request.entity)
      case 'DELETE':
        return simulateDelete(request.endpoint)
      case 'GET':
        return simulateGet(request.endpoint, request.entity)
    }

    return null
  }

  /**
   * Simulates a PUT action (e.g. document index).
   * The document indexObject action supports the response value 'id', 'result'.
   * @param endpoint The URI.
   * @param entity The input content.
   * @return The simulated response.
   */
  def simulatePut(String endpoint, HttpEntity entity) {
    if (content) {
      // Get the content object as JSON and make sure it matches the value passed to the mocked method.
      def expectedContent = SearchEngineClient.formatForIndex(content)
      def usedContent = EntityUtils.toString(entity)
      assert usedContent == expectedContent
    }

    def l = endpoint.tokenize('/')
    def indexName = l[0]
    return new MockResponse("""{"result": "${response?.result}", "_index": "${indexName}","_id": "${response?.uuid}"}""")
  }

  /**
   * Simulates a POST action (e.g. document bulk index).
   * The document bulkIndex action supports the response value 'items' which is a list of domain object that should be
   * (simulated) indexed
   * @param endpoint The URI.
   * @param entity The input content.
   * @return The simulated response.
   */
  @SuppressWarnings(["GroovyUnusedDeclaration", "GroovyVariableNotAssigned"])
  def simulatePost(String endpoint, HttpEntity entity) {
    def sb = new StringBuilder()

    // Now, add simulated results from any indexed objects in the simulated response.
    for (object in response?.items) {
      if (sb.length()) {
        sb.append(',')
      }
      def indexName = NameUtils.convertToHyphenatedName(object.getClass().simpleName) + indexSuffix
      sb.append("""{"index":{"_index":"$indexName","_id":"${object.uuid}","result":"created"}}""")
    }

    return new MockResponse("""{"took": 30,"errors": false,"items": [${sb.toString()}]}""")
  }

  /**
   * Simulates a GET action.  Supports: getStatus checks.
   * The document indexObject action supports the response value 'id', 'result'.
   * @param endpoint The URI.
   * @param entity The input content.
   * @return The simulated response.
   */
  @SuppressWarnings(["GroovyUnusedDeclaration", "GroovyVariableNotAssigned", "GroovyAssignabilityCheck"])
  def simulateGet(String endpoint, HttpEntity entity) {
    def res = """{"error": true}"""
    if (endpoint.startsWith('/_cluster/health')) {
      res = """{"status": "${response?.status ?: 'green'}"}"""
    } else if (endpoint.startsWith('/_search') || endpoint =~ '^/(.*)/_search') {
      if (uri) {
        assert endpoint == uri
      }
      // Build a hits list for the simulated result.
      def count = 1
      def hits = []
      def className = response?.className ?: 'sample.SamplePanelDomain'
      def indexName = response?._index
      for (responseHit in response?.hits) {
        def hit = [_id: responseHit.uuid ?: UUID.randomUUID(), _index: indexName]
        hit._source = [samplePanelDomain: responseHit]
        responseHit.class = className
        hits << hit
        count++
      }

      def totalHits = response?.totalHits ?: response?.hits?.size() ?: 0
      res = Holders.objectMapper.writeValueAsString([took: response?.took ?: 247, _index: indexName,
                                                     hits: [total: totalHits, hits: hits]])
    }

    return new MockResponse(res)
  }

  /**
   * Simulates a DELETE action (e.g. delete all indices or remove single document from index).
   * @param endpoint The URI.
   * @return The simulated response.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  def simulateDelete(String endpoint) {
    // Find last element and see if it is a UUID
    def loc = endpoint.lastIndexOf('/')
    def s = endpoint[loc + 1..-1]
    if (UUIDUtils.isUUID(s)) {
      return new MockResponse("""{"result":"deleted"}""")
    } else {
      return new MockResponse("""{"acknowledged":true}""")
    }


  }

  /**
   * A mock for the response from the rest client.
   */
  class MockResponse {

    String content

    /**
     * The constructor.
     * @param content The response content (a JSON string).
     */
    MockResponse(String content) {
      this.content = content
    }

    HttpEntity getEntity() {
      return new StringEntity(content)
    }

  }

}
