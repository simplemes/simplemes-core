/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search


import com.fasterxml.jackson.databind.ser.FilterProvider
import com.fasterxml.jackson.databind.ser.PropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import groovy.util.logging.Slf4j
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.entity.ContentType
import org.apache.http.nio.entity.NStringEntity
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.Request
import org.elasticsearch.client.RestClient
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.misc.URLUtils

/**
 * This helper encapsulates the search interface to the external search engine (Elastic Search in this case).
 * This allows for easier use of the search engine and
 * supports unit testing with an interface that can be easily mocked.
 * This helper provides synchronous access to the search engine actions such as indexObject, indexObjects and search.
 * <p>
 * This helper class can be created once when the application starts up and should never need refreshing.
 * <p>
 * This class generally converts the search engines inputs/outputs between JSON and POGOs.
 * This isolates the search engine specific logic from most of the framework.
 *
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>info</b> - Performance timing. </li>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs. </li>
 * </ul>
 *
 */
@Slf4j
class SearchEngineClient implements SearchEngineClientInterface {

  /**
   * The list of external search engines that the RestClient will connect to.  Typically, this is defined
   * in the application.yml configuration file.
   *
   * <p>
   * <b>Note:</b> The internal used of RestClient and Response are left loosely typed as Object to allow easier mocking.
   */
  List<HttpHost> hosts

  /**
   * The shared client used to communicate with the search engine.
   */
  Object restClient

  /**
   * Internal flag to avoid spamming the 'no hosts in application.yml' log messages.
   */
  private boolean alreadyWarnedNoHosts = false

  /**
   * Returns the client used to talk to the search engine.  Will create a shared client and re-use it for
   * all requests.
   * @return The client.
   */
  @SuppressWarnings("CouldBeElvis")
  Object getRestClient() {
    if (!restClient) {
      if (hosts) {
        restClient = RestClient.builder(hosts as HttpHost[]).build()
        log.info('getRestClient: Built the rest client {} for hosts {}', restClient, hosts)
      } else {
        if (!alreadyWarnedNoHosts) {
          log.info('getRestClient: No hosts found in application configuration. External search engine disabled. ')
          alreadyWarnedNoHosts = true
        }
      }
    }
    return restClient
  }

  /**
   * Returns the status of the search engine's cluster.
   * Can log performance as level INFO and values as DEBUG log messages.
   * @return The status.
   */
  SearchStatus getStatus() {
    try {
      def start = System.currentTimeMillis()
      log.debug('getStatus: GET {}', "/_cluster/health?pretty=true")
      def response = getRestClient()?.performRequest(new Request("GET", "/_cluster/health"))
      def content = EntityUtils.toString((HttpEntity) response.entity)
      //println "content = $content"
      //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
      def json = Holders.objectMapper.readValue(content, Map)
      //println "json = ${json}"
      log.info('getStatus: Elapsed time {}ms', System.currentTimeMillis() - start)
      log.debug('getStatus: result = {}', json)

      def searchStatus = new SearchStatus(json)

      // Figure out what the search request pool is doing.
      def pool = SearchEnginePoolExecutor.pool

      if (pool) {
        searchStatus.pendingRequests = pool?.queue?.size()
      }

      return searchStatus
    } catch (ConnectException ignored) {
      def searchStatus = new SearchStatus()
      searchStatus.status = 'timeout'
      return searchStatus
    }
  }

  /**
   * Indexes a single object, waiting for completion.  This can be used directly, but is typically called by the request
   * queue logic.
   * Can log performance as level INFO and values as DEBUG log messages.
   *
   * @param object The domain object to index.  Must be saved.
   * @return Status of the index creation.  A Map.
   */
  Map indexObject(Object object) {
    def start = System.currentTimeMillis()
    def uri = buildURIForIndexRequest(object)
    def jsonString = formatForIndex(object)
    log.debug('indexObject: PUT {}, content = {}', uri, jsonString)
    def request = new Request("PUT", uri)
    request.entity = new NStringEntity(jsonString, ContentType.APPLICATION_JSON)
    def response = getRestClient()?.performRequest(request)

    def content = EntityUtils.toString((HttpEntity) response.entity)
    //println "content = $content"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = Holders.objectMapper.readValue(content, Map)
    //println "json = ${json}"

    log.debug('indexObject: result = {}', json)
    def elapsed = System.currentTimeMillis() - start
    log.info('indexObject: Elapsed time {}ms', elapsed)
    return json
  }

  /**
   * Removes a single object from the index.
   *
   * @param object The domain object to remove from the index.  Must have a record id..
   * @return Status of the index creation.  A Map.
   */
  Map removeObjectFromIndex(Object object) {
    def start = System.currentTimeMillis()
    def uri = buildURIForIndexRequest(object)
    log.debug('removeObjectFromIndex: DELETE {}', uri)
    def response = getRestClient()?.performRequest(new Request("DELETE", uri))

    def content = EntityUtils.toString((HttpEntity) response.entity)
    //println "content = $content"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = Holders.objectMapper.readValue(content, Map)
    //println "json = ${json}"

    log.debug('removeObjectFromIndex: result = {}', json)
    def elapsed = System.currentTimeMillis() - start
    log.info('removeObjectFromIndex: Elapsed time {}ms', elapsed)
    return json
  }

  /**
   * Indexes a single object, waiting for completion.  This can be used directly, but is typically called by the request
   * queue logic.
   * Can log performance as level INFO and values as DEBUG log messages.
   *
   * @param objects The domain objects to index.  Must be saved.
   * @param archiveReferenceList The list of archive references associated with the list of objects.  (Optional).
   *        This will modify the index used to be the archive index and add the element '_archiveReference' to the
   *        document indexed.  The size of the objects list and this must match.
   * @return Status of the index creation.  A Map with the response from the engine.
   */
  Map bulkIndex(List objects, List<String> archiveReferenceList = null) {
    def start = System.currentTimeMillis()
    def uri = '/_bulk'
    //Map<String, String> params = Collections.emptyMap()
    def jsonString = buildBulkIndexContent(objects, archiveReferenceList)
    log.debug('bulkIndex: POST {}, content = {}', uri, LogUtils.limitedLengthString(jsonString, 2000))
    def request = new Request("POST", uri)
    request.entity = new NStringEntity(jsonString, ContentType.APPLICATION_JSON)
    def response = getRestClient()?.performRequest(request)

    def content = EntityUtils.toString((HttpEntity) response.entity)
    //println "response = $content"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(content)}"
    def json = Holders.objectMapper.readValue(content, Map)
    //println "json = ${json}"

    log.debug('bulkIndex: result = {}', LogUtils.limitedLengthString(json.toString(), 200))
    log.info('bulkIndex: Elapsed time {}ms', System.currentTimeMillis() - start)
    return json
  }

  /**
   * Performs a global search using the given query string.
   * @param query The query string.  If it starts with &#123; then the string is used as the body for the GET request.
   * @param params Optional query parameters.  Supported elements: from and size
   * @param The search results.
   */
  SearchResult globalSearch(String query, Map params = null) {
    return search('/_search', query, params)
  }

  /**
   * Performs a standard domain search with the query string.  
   * @param domainClass The domain class to search.
   * @param query The query string.
   * @param params Optional query parameters.  Supported elements: from and size
   * @return The search result, containing the list of values found.
   */
  SearchResult domainSearch(Class domainClass, String query, Map params = null) {
    if (!SearchHelper.instance.isSearchable(domainClass)) {
      throw new IllegalArgumentException("Domain ${domainClass} is not searchable.")
    }
    def uri = buildURIForSearchRequest(domainClass)
    return search(uri, query, params)
  }

  /**
   * Performs an internal search (global or domain) for the given query string and the given URI.
   * @param uri The URI to search on.
   * @param query The query string.  If it starts with &#123; then the string is used as the body for the GET request.
   * @param params Optional query parameters.  Supported elements: from and size.
   * @param The search results.
   */
  protected SearchResult search(String uri, String query, Map params = null) {
    def from = 0
    def size = 10
    try {
      ArgumentUtils.checkMissing(query, 'query')
      ArgumentUtils.checkMissing(uri, 'uri')
      def start = System.currentTimeMillis()
      HttpEntity entity = null
      if (query.startsWith('{')) {
        entity = new NStringEntity(query, ContentType.APPLICATION_JSON)
      } else {
        // simple query string
        uri += "?q=$query"
      }

      if (params?.size) {
        size = ArgumentUtils.convertToInteger(params.size)
      }
      if (params?.from) {
        from = ArgumentUtils.convertToInteger(params.from)
      }
      uri = URLUtils.addParametersToURI(uri, [size: params?.size?.toString(), from: params?.from?.toString()])

      log.debug('search: GET {}, content = {}', uri, query)
      def request = new Request("GET", uri)
      request.entity = entity
      def response = getRestClient()?.performRequest(request)

      def content = EntityUtils.toString((HttpEntity) response.entity)
      //println "content = $content"
      //println "JSON = ${groovy.json.JsonOutput.prettyPrint(content)}"
      def json = Holders.objectMapper.readValue(content, Map)
      //println "json = ${json}"

      if (log.debugEnabled) {
        log.debug('search: result = {}', LogUtils.limitedLengthString(json.toString(), 2000))
      }
      log.info('search: Elapsed time {}ms', System.currentTimeMillis() - start)

      def searchResult = new SearchResult(json)
      searchResult.query = query
      searchResult.from = from
      searchResult.size = size
      return searchResult
    } catch (ConnectException ignored) {
      def searchResult = new SearchResult()
      searchResult.query = query
      searchResult.from = from
      searchResult.size = size
      return searchResult
    }
  }

  /**
   * Deletes all Indices in the search engine.  This will lose all data there.
   * Can log performance as level INFO log messages.
   *
   * @return Status of the removal.
   */
  @Override
  Map deleteAllIndices() {
    def start = System.currentTimeMillis()
    def uri = '/_all'
    log.debug('deleteAllIndices: DELETE {}', uri)
    def response = getRestClient()?.performRequest(new Request("DELETE", uri))

    def content = EntityUtils.toString((HttpEntity) response.entity)
    //println "response = $content"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(content)}"
    def json = Holders.objectMapper.readValue(content, Map)
    //println "json = ${json}"

    log.debug('deleteAllIndices: result = {}', content)
    log.info('deleteAllIndices: Elapsed time {}ms', System.currentTimeMillis() - start)
    return json
  }

  /**
   * The delimiter used between lines in the bulk request.
   */
  static final String BULK_REQUEST_DELIMITER = '\r\n'

  /**
   * Builds the bulk index request content for a list of domain objects.
   * @param list The list.
   * @param archiveReferenceList The list of archive references associated with the list of objects.  (Optional).
   *        This will modify the index used to be the archive index and add the element '_archiveReference' to the
   *        document indexed.  The list size must match this reference list (if given).
   * @return The JSON content in the correct format for a bulk index request.
   */
  static String buildBulkIndexContent(List list, List<String> archiveReferenceList = null) {
    ArgumentUtils.checkMissing(list, 'list')
    def indexSuffix = ''
    if (archiveReferenceList) {
      assert archiveReferenceList.size() == list.size()
      indexSuffix = SearchHelper.ARCHIVE_INDEX_SUFFIX
    }
    def sb = new StringBuilder()

    def i = 0
    for (object in list) {
      def clazz = object.getClass()
      def indexName = SearchHelper.instance.getIndexNameForDomain(clazz)
      sb.append("""{"index":{"_index":"$indexName$indexSuffix","_type":"doc","_id":"${object.uuid}"}}""")
      sb.append(BULK_REQUEST_DELIMITER)
      sb.append(formatForIndex(object)).append(BULK_REQUEST_DELIMITER)
      if (archiveReferenceList) {
        // If we are trying to index an archived object, add _archiveReference to the content at the end, just before the last
        // bracket.
        def offset = sb.length() - 1 - BULK_REQUEST_DELIMITER.length()
        sb.insert(offset, """ ,"_archiveReference":"${archiveReferenceList[i]}" """)
      }
      i++
    }

    return sb.toString()
  }

  /**
   * Formats the given object as JSON for the search engine index requests.
   * @param object The object to format.
   * @return The JSON.
   */
  static String formatForIndex(Object object) {
    def settings = SearchHelper.instance.getSearchDomainSettings(object.getClass())
    def filter = new SearchableJacksonFilter(settings?.exclude)

    FilterProvider filters = new SimpleFilterProvider().addFilter("searchableFilter", (PropertyFilter) filter)
    def writer = Holders.objectMapper.writer(filters)
    return writer.writeValueAsString(object)
  }

  /**
   * Builds the URI needed to send the PUT request to index the object.
   * @param object The object to index.
   * @return The URI for this object's PUT request.
   */
  static String buildURIForIndexRequest(Object object) {
    ArgumentUtils.checkMissing(object, 'object')
    ArgumentUtils.checkMissing(object.uuid, 'object.uuid')
    def clazz = object.getClass()
    def indexName = SearchHelper.instance.getIndexNameForDomain(clazz)
    return "/$indexName/_doc/$object.uuid"
  }

  /**
   * Builds the URI needed to make a search request on a domainClass.
   * @param domainClass The domainClass to search on.
   * @return The URI for a domain search request.
   */
  static String buildURIForSearchRequest(Class domainClass) {
    ArgumentUtils.checkMissing(domainClass, 'domainClass')
    def indexName = SearchHelper.instance.getIndexNameForDomain(domainClass)
    return "/$indexName/_search"
  }

}
