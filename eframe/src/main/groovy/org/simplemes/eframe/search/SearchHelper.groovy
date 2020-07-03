/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.data.model.Pageable
import org.apache.http.HttpHost
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.ArchiverFactory
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.domain.SQLUtils
import org.simplemes.eframe.domain.annotation.DomainEntityHelper
import org.simplemes.eframe.domain.annotation.DomainEntityInterface
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TypeUtils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * This helper encapsulates the search interface.  This allows for easier use of the search engine and
 * supports unit testing with an interface that can be easily mocked.
 * This helper provides a background indexing mechanism to reduce the affect on the application's
 * domain record updates.
 * <p/>
 * This helper class can be created once when the application starts up and should never need refreshing.
 *
 * <p> <p>
 * <b>Note:</b> This is an internal helper class.  Application and module code should use the
 * {@link org.simplemes.eframe.search.service.SearchService} instead.  This class is subject to change.
 *
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>info</b> - Performance timing. </li>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs for each normal (non-bulk) request. </li>
 *   <li><b>trace</b> - Detailed information on bulk actions. Ths includes the record IDs for the objects indexed.</li>
 * </ul>

 */
@CompileStatic
@Slf4j
class SearchHelper {

  /**
   * A static instance for this helper.
   */
  static SearchHelper instance = new SearchHelper()

  /**
   * The shared client used to communicate with the search engine.
   */
  SearchEngineClientInterface searchEngineClient

  /**
   * Keeps track of when the search feature is disabled (not configured).
   */
  boolean searchDisabled = false

  /**
   * The number of requests processed.  Updated by the search engine thread pool.
   * This is an atomic integer for high volume scenarios.
   */
  AtomicInteger finishedRequestCount = new AtomicInteger(0)

  /**
   * The number of failures triggered by the requests.  Updated by the search engine thread pool.
   * This is an atomic integer for high volume scenarios.
   */
  AtomicInteger failureCount = new AtomicInteger(0)

  /**
   * The status of the bulk request - in progress state.
   */
  static final String BULK_INDEX_STATUS_IN_PROGRESS = 'inProgress'

  /**
   * The status of the bulk request - completed successfully.
   */
  static final String BULK_INDEX_STATUS_COMPLETE = 'completed'

  /**
   * The time the current/last bulk index request was started.
   */
  long bulkIndexStart

  /**
   * The time the current/last bulk index request was finished.
   */
  long bulkIndexEnd

  /**
   * The current status of the current/last bulk index request.
   */
  String bulkIndexStatus = ''

  /**
   * The number of bulk requests in the current/last bulk index request.
   */
  int bulkIndexRequestCount = 0

  /**
   * The number of bulk requests completed in the current/last bulk index request.
   */
  int bulkIndexFinishedCount = 0

  /**
   * The number of bulk requests that failed during the current/last bulk index request.
   */
  int bulkIndexErrorCount = 0

  /**
   * If true, then all search actions that have a fallback should use it.  Mostly domainSearch().
   */
  boolean fallback = false

  /**
   * The suffix to add to all indices created for archived elements.
   */
  public static final String ARCHIVE_INDEX_SUFFIX = '-arc'

  @SuppressWarnings("CouldBeElvis")
  SearchEngineClientInterface getSearchEngineClient() {
    if (!searchEngineClient) {
      def hosts = determineHosts()
      if (hosts) {
        // All others, attempt to connect
        searchEngineClient = new SearchEngineClient(hosts: hosts)
      } else {
        fallback = true
        searchEngineClient = new MockSearchEngineClient()
        if (!Holders.environmentTest) {
          log.warn('getSearchEngineClient: Using Mock Engine - No eframe.search.hosts entry in application.yml')
        }
      }
    }
    return searchEngineClient
  }

  /**
   * Determines the search hosts to be contacted.  Configured in the <code>application.yml</code> file.
   * @return The list of hosts.
   */
  @CompileDynamic
  List<HttpHost> determineHosts() {
    def configuredHosts = Holders.configuration.search.hosts
    log.debug('determineHosts: configuredHosts = {}', configuredHosts)

    def hosts = []
    for (cfg in configuredHosts) {
      def hostName = cfg.host
      if (hostName) {
        def port = cfg.port ?: 9200
        def protocol = cfg.protocol ?: 'http'
        hosts << new HttpHost((String) hostName, port, protocol)
      } else {
        log.warn('determineHosts: host not found in application configuration {}', (Object) cfg)
      }
    }

    log.debug('determineHosts: Returning {}', hosts)
    if (!hosts && !searchDisabled) {
      log.info('determineHosts: No host found in application configuration. External search engine disabled. ')
      searchDisabled = true
    }
    return hosts
  }

  /**
   * Returns the status of the search engine's cluster.  Also contains info on any active or the previous bulk
   * index request.  This layer adds the bulk request status details.
   * @return The status.
   */
  SearchStatus getStatus() {
    def searchStatus = getSearchEngineClient().status
    //println "searchStatus1 = $searchStatus"
    addBulkIndexStatus(searchStatus)
    searchStatus.configured = !searchDisabled
    //println "searchStatus2 = $searchStatus"
    return searchStatus
  }

  /**
   * Performs a standard global search with the query string.
   * <p>
   * <b>Note:</b> This is an internal API method.  Application and module code should use the
   * {@link org.simplemes.eframe.search.service.SearchService} instead.
   *
   * @param query The query string.
   * @param params Optional query parameters.  Supported elements: from and size
   * @return The search result, containing the list of values found.
   */
  SearchResult globalSearch(String query, Map params = null) {
    return getSearchEngineClient().globalSearch(query, params)
  }

  /**
   * Performs a standard domain search with the query string using the external search engine.
   * <p>
   * <b>Note:</b> This is an internal API method.  Application and module code should use the
   * {@link org.simplemes.eframe.search.service.SearchService} instead.
   *
   * @param domainClass The domain class to search.
   * @param query The query string.
   * @param params Optional query parameters.  Supported elements: from and size
   * @return The search result, containing the list of values found.
   */
  SearchResult domainSearch(Class domainClass, String query, Map params = null) {
    return getSearchEngineClient().domainSearch(domainClass, query, params)
  }

  /**
   * Determines if the given domain is searchable and if the search engine is configured for this application.
   * @param domainClass The domain class to search.
   * @return The search result, containing the list of values found.
   */
  boolean isDomainSearchable(Class domainClass) {
    ArgumentUtils.checkMissing(domainClass, 'domainClass')
    // Figure out if the domain supports search first
    if (isSearchable(domainClass)) {
      // Uses the global fallback in the case where the search engine is not configured.
      return !fallback
    }

    return false
  }

  /**
   * Performs a domain search via SQL with the query string.  This uses a key-based SQL search.
   *
   * <h3>params</h3>
   * The <code>params</code> parameter can contain these options:
   * <ul>
   *   <li><b>max/offset/sort/order</b> - The standard paging/sorting parameters from the controller.</li>
   *   <li><b>options</b> - See below. </li>
   * </ul>
   *
   * <h3>options</h3>
   * The <code>options</code> parameter can contain these options:
   * <ul>
   *   <li><b>postProcessor</b> - A closure that is execute for each record found.  Useful for adding/removing elements from the list for each row. </li>
   *   <li><b>additionalProperties</b> - A list of property names (strings) to include in the formatted output.  This allows you to display transients in the list output. </li>
   * </ul>
   *
   * @param domainClass The domain class to search.
   * @param search The query string.  Optional.  If not provided, then finds all records.
   * @param params Optional query parameters.  Supported elements: offset/max and sorting options as supported by the ControllerUtils.
   *               Also supports an options element for other ControllerUtils features (see Options above).
   * @return The search result, containing the list of values found.
   */
  @CompileDynamic
  SearchResult domainSearchInDB(Class domainClass, String search, Map params = null) {
    def start = System.currentTimeMillis()
    def searchResult = new SearchResult()
    searchResult.query = search
    def options = params?.options

    // Figure out the primary key field for the search (if needed)
    def searchKey = ''
    if (search) {
      def keys = DomainUtils.instance.getKeyFields(domainClass)
      if (keys.size() > 0) {
        searchKey = DomainEntityHelper.instance.getColumnName(domainClass, keys[0])
      } else {
        throw new IllegalArgumentException("Domain class ${domainClass} has no key field defined.  Search filter is not supported.")
      }
    }

    // Figure out the criteria for paging/sorting
    def (int from, int max) = ControllerUtils.instance.calculateFromAndSizeForList(params)
    def (String sortField, String sortDir) = ControllerUtils.instance.calculateSortingForList(params)

    String tableName = DomainEntityHelper.instance.getTableName(domainClass)
    String where = search ? " WHERE $searchKey ILIKE ? " : ''

    String orderBy = ''
    if (sortField) {
      sortDir = sortDir ?: 'asc'
      sortField = DomainEntityHelper.instance.getColumnName(domainClass, sortField)
      orderBy = "ORDER BY $sortField $sortDir"
    }
    String sql = "SELECT * FROM $tableName $where $orderBy"

    int nArgs = 1 + (search ? 1 : 0)
    Object[] args = new Object[nArgs]
    args[0] = Pageable.from(from, max)
    if (search) {
      args[1] = "%${search}%".toString()
    }


    def list = SQLUtils.instance.executeQuery(sql, domainClass, args)

    if (params?.options?.postProcessor) {
      list.each { options?.postProcessor(it) }
    }

    // Find the total row count (depends on search criteria).
    def total
    if (search) {
      // Build a count() query to find how many match.
      def countList
      String countSql = "SELECT COUNT(*) as count FROM $tableName $where "
      countList = SQLUtils.instance.executeQuery(countSql, Map, "%${search}%".toString())
      total = countList[0].count
    } else {
      total = domainClass.count()
    }
    searchResult.totalHits = total as int
    for (hit in list) {
      searchResult.hits << new SearchHit(hit)
    }

    def elapsed = System.currentTimeMillis() - start
    log.info('domainSearchInDB: Elapsed time {}ms, found {} records of {}.  Domain = {}', elapsed, list.size(), total, domainClass)

    return searchResult
  }

  /**
   * Indexes a single object, waiting for completion.  This can be used directly, but is typically called by the request
   * queue logic.
   *
   * @param object The domain object to index.  Must be saved.
   * @return Status of the index creation.  A Map.
   */
  Map indexObject(Object object) {
    return getSearchEngineClient().indexObject(object)
  }

  /**
   * Removes a single object from the index.
   *
   * @param object The domain object to remove from the index.  Must have a record id..
   * @return Status of the index creation.  A Map.
   */
  Map removeObjectFromIndex(Object object) {
    return getSearchEngineClient().removeObjectFromIndex(object)
  }

  /**
   * Indexes a list of objects, waiting for completion.  This can be used directly, but is typically called by the request
   * queue logic.
   * Can log performance as level INFO and values as DEBUG log messages.
   *
   * @param objects The domain object to index.  Must be saved.
   * @param archiveReferenceList The list of archive references associated with the list of objects.  (Optional).
   *        This will modify the index used to be the archive index and add the element '_archiveReference' to the
   *        document indexed.
   * @return Status of the index creation.  A Map.
   */
  Map bulkIndex(List objects, List<String> archiveReferenceList = null) {
    return getSearchEngineClient().bulkIndex(objects, archiveReferenceList)
  }

  /**
   * Starts the bulk index request.  This queues up the index requests needed.
   * The index delete is run synchronously, then the index request are queued after that.
   * <p>
   * <b>Note:</b> This method requires an active transaction.
   *
   * @param deleteAllIndices If true, then this triggers a delete of all indices before the request is started.
   */
  void startBulkIndexRequest(Boolean deleteAllIndices = false) {
    // Reset all current counters
    bulkIndexRequestCount = 0
    bulkIndexErrorCount = 0
    bulkIndexFinishedCount = 0
    bulkIndexStatus = BULK_INDEX_STATUS_IN_PROGRESS
    def start = System.currentTimeMillis()
    if (deleteAllIndices) {
      getSearchEngineClient().deleteAllIndices()
    }

    bulkIndexStart = start
    bulkIndexEnd = 0

    buildAndSubmitBulkIndexRequests()
    buildAndSubmitBulkArchiveIndexRequests()

    // This is just the time to submit the requests.
    log.info('startBulkIndexRequest: Elapsed time for startup (not total bulk index time): {}ms', System.currentTimeMillis() - start)
  }

  /**
   * Builds the list of bulk index requests and submits them to the queue to be processed.
   * This method logs a debug message with each domain and the number of requests queued for it.
   *
   */
  @CompileDynamic
  void buildAndSubmitBulkIndexRequests() {
    // Find all of the domains that are searchable
    def searchableDomainClasses = []
    for (domainClass in (DomainUtils.instance.allDomains)) {
      if (isSearchable(domainClass)) {
        searchableDomainClasses << domainClass
      }
    }

    def batchSize = Holders.configuration.search.bulkBatchSize ?: 50
    log.debug('buildAndSubmitBulkIndexRequests: building requests with max of {} documents for domains {}.', batchSize, searchableDomainClasses)

    // Keep track of the domains we found, to avoid processing sub-classes twice.
    //Set domainsProcessed = [] as Set

    for (Class clazz in searchableDomainClasses) {
      def total = clazz.count()
      def batchCount = NumberUtils.divideRoundingUp((long) total, (int) batchSize)

      def offset = 0
      for (int i = 0; i < batchCount; i++) {
        // Grab the next batch of records
        //def list = clazz.listOrderById([offset: offset, max: batchSize, order: "asc"])

        String tableName = DomainEntityHelper.instance.getTableName(clazz)
        String sql = "SELECT * FROM $tableName order by uuid"
        def list = SQLUtils.instance.executeQuery(sql, clazz, Pageable.from(i, batchSize))

        def request = new SearchEngineRequestBulkIndex(list)
        SearchEnginePoolExecutor.addRequest(request)
        bulkIndexRequestCount++
        log.debug('build..Requests: Created request for {} objects on {}', list.size(), clazz.simpleName)
        if (log.traceEnabled) {
          log.trace('build..Requests: IDs to be indexed for {} =  {}', clazz.simpleName, list*.uuid)
        }
        offset += batchSize
      }
    }
  }

  /**
   * Builds the list of bulk index requests on archive files and submits them to the queue to be processed.
   * This method logs a debug message with each archive file found.
   *
   */
  @CompileDynamic
  void buildAndSubmitBulkArchiveIndexRequests() {
    def fileRefs = ArchiverFactory.instance.archiver.findAllArchives()

    def total = fileRefs.size()
    def batchSize = Holders.configuration.search.bulkBatchSize ?: 50
    def batchCount = NumberUtils.divideRoundingUp(total, batchSize)

    def offset = 0
    for (int i = 0; i < batchCount; i++) {
      def end = offset + batchSize - 1
      end = Math.min(end, total - 1)
      def batch = fileRefs[offset..end]

      def request = new SearchEngineRequestBulkArchiveIndex(batch)
      SearchEnginePoolExecutor.addRequest(request)
      bulkIndexRequestCount++
      log.debug('build.Archive.Requests: Created request for {} archived objects', batch.size())
      if (log.traceEnabled) {
        log.trace('build.Archive.Requests: References to be indexed for {}', batch)
      }

      //def fileRef = fileRefs[offset+i]
      offset += batchSize
    }
  }

  /**
   * Adds the bulk index request status to the given status.
   * @param searchStatus The status to add to.
   */
  void addBulkIndexStatus(SearchStatus searchStatus) {
    def queue = SearchEnginePoolExecutor.pool?.queue
    if (queue != null) {
      //println "queue = ${queue.size()}"
      def count = 0
      for (request in queue) {
        //println "request = $request"
        if (request instanceof SearchEngineRequestBulkIndex) {
          count++
        }
      }
      searchStatus.pendingBulkRequests = count
    }
    searchStatus.finishedRequestCount = finishedRequestCount.get()
    searchStatus.failedRequests = failureCount.get()
    searchStatus.totalBulkRequests = bulkIndexRequestCount
    searchStatus.bulkIndexStatus = bulkIndexStatus
    searchStatus.bulkIndexErrorCount = bulkIndexErrorCount
    searchStatus.bulkIndexStart = bulkIndexStart
    searchStatus.bulkIndexEnd = bulkIndexEnd
    searchStatus.finishedBulkRequests = bulkIndexFinishedCount
  }

  /**
   * This is called when a bulk request is finished.
   * @param errorsFound The number of errors or exceptions found.
   */
  void finishedBulkRequest(int errorsFound) {
    bulkIndexErrorCount += errorsFound
    bulkIndexFinishedCount++
    if (bulkIndexFinishedCount >= bulkIndexRequestCount) {
      bulkIndexStatus = BULK_INDEX_STATUS_COMPLETE
      bulkIndexEnd = System.currentTimeMillis()
    }
    log.info('finishedBulkRequest: bulkIndexFinishedCount {}, {}, status {} at {}', bulkIndexFinishedCount, bulkIndexRequestCount,
             bulkIndexStatus, new Date(bulkIndexEnd))
  }

  /**
   * Notifies the helper that a request failed.
   */
  void requestFailed() {
    failureCount.incrementAndGet()
  }

  /**
   * Notifies the helper that a request was added.
   */
  void finishedRequest() {
    finishedRequestCount.incrementAndGet()
  }

  /**
   * Returns the current request count.
   * @return
   */
  int getFinishedRequestCount() {
    return finishedRequestCount.get()
  }

  /**
   * Returns the current failure count.
   * @return
   */
  int getFailureCount() {
    return failureCount.get()
  }

  /**
   * Resets the request counters.
   */
  void resetCounts() {
    finishedRequestCount.set(0)
    failureCount.set(0)
  }

  /**
   * Clears the current statistics.
   */
  void clearStatistics() {
    resetCounts()
    bulkIndexStatus = ''
    bulkIndexFinishedCount = 0
    bulkIndexErrorCount = 0
    bulkIndexEnd = 0
    bulkIndexStart = 0
    bulkIndexRequestCount = 0
  }

  /**
   * The current cached domain settings.
   */
  Map domainSettingsCache = new ConcurrentHashMap()

  /**
   * Returns the effective search settings for the given class.
   * @param domainClass The domain class.
   * @return The settings.
   */
  SearchDomainSettings getSearchDomainSettings(Class domainClass) {
    // Use the cached settings, if any.
    def settings = domainSettingsCache[domainClass]
    if (settings) {
      return settings as SearchDomainSettings
    }

    // Check for simple case: searchable = true/false.
    def o = TypeUtils.getStaticProperty(domainClass, "searchable")
    if (o instanceof Boolean) {
      settings = new SearchDomainSettings(searchable: (Boolean) o)
    } else if (o instanceof Closure) {
      settings = new SearchDomainSettings(o)
    }

    // Default is not searchable.
    settings = settings ?: new SearchDomainSettings(searchable: false)

    domainSettingsCache[domainClass] = settings

    return settings as SearchDomainSettings
  }

  /**
   * Determines if the given class is searchable.
   */
  boolean isSearchable(Class domainClass) {
    def settings = getSearchDomainSettings(domainClass)
    return settings.searchable
  }

  /**
   * Handles the persistence update/create for domains.  Updates the search engine indices for those changes.
   * @param object The domain object.
   */
  @CompileDynamic
  void handlePersistenceChange(DomainEntityInterface object) {
    requestBackgroundIndexObject(object)
  }

  /**
   * Handles the persistence delete for domains.  Removes the object index.
   * @param object The domain object.
   */
  @CompileDynamic
  void handlePersistenceDelete(DomainEntityInterface object) {
    requestBackgroundObjectRemoval(object)
  }

  /**
   * Get the domain class associated with the given index name.
   * Uses a naming scheme to find the domain class.
   * @param indexName The index name.
   * @return The domain class for the index.
   */
  Class getDomainClassForIndex(String indexName) {
    if (!indexName) {
      return null
    }
    if (indexName.endsWith(SearchHelper.ARCHIVE_INDEX_SUFFIX)) {
      indexName = indexName[0..(indexName.length() - 4)]
    }
    def domainSimpleName = NameUtils.convertFromHyphenatedName(indexName)

    return DomainUtils.instance.getAllDomains().find { it.simpleName.toLowerCase() == domainSimpleName.toLowerCase() }

  }

  /**
   * Get the index name for the given domain class.
   * Uses a naming scheme to build the index name.
   * @param indexName The index name.
   * @return The domain class for the index.
   */
  String getIndexNameForDomain(Class domainClass) {
    if (!domainClass) {
      return null
    }

    return NameUtils.convertToHyphenatedName(domainClass.simpleName)
  }

  /**
   * Will create and submit an indexObject request to the search engine.
   * @param object The object to index.  Only searchable domain objects will be indexed.
   */
  static void requestBackgroundIndexObject(Object object) {
    ArgumentUtils.checkMissing(object, 'object')
    def clazz = object.getClass()
    if (!SearchHelper.instance.isSearchable(clazz)) {
      return
    }
    SearchEnginePoolExecutor.addRequest(new SearchEngineRequestIndexObject(object))
  }

  /**
   * Will create and submit a deleteObject request to the search engine.
   * @param object The object remove from the index.  Only searchable domain objects will be removed from the index.
   */
  static void requestBackgroundObjectRemoval(Object object) {
    ArgumentUtils.checkMissing(object, 'object')
    def clazz = object.getClass()
    if (!SearchHelper.instance.isSearchable(clazz)) {
      return
    }
    SearchEnginePoolExecutor.addRequest(new SearchEngineRequestObjectRemoval(object))
  }

  /**
   * Determines if this query string is simple.  This means it does no contain logic or quotes.
   * @param queryString The string to check.
   * @return True if simple.
   */
  static boolean isSimpleQueryString(String queryString) {
    // Checks for ()'" and the strings ' or ', ' and '
    return !(queryString =~ /'|"| [oO][rR] | [aA][nN][dD] |\(|\)/)
  }

}
