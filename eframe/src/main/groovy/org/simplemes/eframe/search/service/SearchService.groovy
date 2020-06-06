/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search.service

import groovy.util.logging.Slf4j
import org.simplemes.eframe.search.SearchHelper
import org.simplemes.eframe.search.SearchResult
import org.simplemes.eframe.search.SearchStatus

import javax.inject.Singleton

/**
 * A service to handle search-related tasks.
 * This includes searching and indexing.
 */
@Slf4j
@Singleton
class SearchService {

  /**
   * Gets the search engine's status.
   * @return The result of the status request from the search engine.  Contains a field 'status' with values of red/yellow/green.
   */
  SearchStatus getStatus() {
    return SearchHelper.instance.status
  }

  /**
   * Clears the current statistics.
   */
  void clearStatistics() {
    SearchHelper.instance.clearStatistics()
  }

  /**
   * Performs a standard global search with the query string.
   * @param query The query string.
   * @param params Optional query parameters.  Supported elements: offset and max
   * @return The search result, containing the list of values found.
   */
  SearchResult globalSearch(String query, Map params = null) {
    return SearchHelper.instance.globalSearch(adjustQuery(query, null), params)
  }

  /**
   * Performs a standard domain search with the query string.  This class will use a key-based SQL search as a fallback
   * if the search engine is not available or the domain is not searchable.
   *
   * @param domainClass The domain class to search.
   * @param query The query string. This will be adjusted for Search Engine searches by the adjustQuery() method.
   * @param params Optional query parameters.  Supported elements: offset and max
   * @return The search result, containing the list of values found.
   */
  SearchResult domainSearch(Class domainClass, String query, Map params = null) {
    if (query && SearchHelper.instance.isDomainSearchable(domainClass)) {
      return SearchHelper.instance.domainSearch(domainClass, adjustQuery(query, domainClass), params)
    }
    return SearchHelper.instance.domainSearchInDB(domainClass, query, params)
  }

  /**
   * Rebuilds all search indices, with an option to delete existing indices.
   * Starts the bulk index request.  This queues up the index requests needed.
   *
   * @param deleteAllIndices If true, then this triggers a delete of all indices before the request is started.
   */
  void startBulkIndexRequest(Boolean deleteAllIndices = false) {
    SearchHelper.instance.startBulkIndexRequest(deleteAllIndices)
  }

  /**
   * Adjusts the query string to make the input more user friendly. This also serves as an extension point
   * for modules to make their own ease-of-use adjustments to the query string.  For example, this framework
   * adds the wildcard '*' to the end of the string to make it easier to to find partial matches.
   * @param queryString The input query string from the user.
   * @param domainClass The domain class for domain-specific searches.  Null allowed.
   * @return The adjusted query string.
   */
  @SuppressWarnings('unused')
  String adjustQuery(String queryString, Class domainClass) {
    if ((!queryString) || queryString.contains('*')) {
      return queryString
    }

    if (SearchHelper.isSimpleQueryString(queryString)) {
      return queryString + '*'
    } else {
      return queryString
    }
  }

}
