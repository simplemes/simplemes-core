/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import org.apache.http.HttpHost

/**
 *
 */
/**
 * This interface defines the API provided by the search interface to the external search engine (Elastic Search in this case).
 * This allows for easier use of the search engine and
 * supports unit testing with an interface that can be easily mocked.
 * This helper provides synchronous access to the search engine actions such as indexObject, indexObjects and search.
 * <p/>
 * This helper class can be created once when the application starts up and should never need refreshing.
 */
interface SearchEngineClientInterface {

  /**
   * Returns the status of the search engine's cluster.
   * @return The status.
   */
  SearchStatus getStatus()

  /**
   * Indexes a single object, waiting for completion.  This can be used directly, but is typically called by the request
   * queue logic.
   *
   * @param object The domain object to index.  Must be saved.
   * @return Status of the index creation.  A Map.
   */
  Map indexObject(Object object)

  /**
   * Removes a single object from the index.
   *
   * @param object The domain object to remove from the index.  Must have a record id..
   * @return Status of the index creation.  A Map.
   */
  Map removeObjectFromIndex(Object object)

  /**
   * Indexes a list of objects, waiting for completion.  This can be used directly, but is typically called by the request
   * queue logic.
   * Can log performance as level INFO and values as DEBUG log messages.
   *
   * @param objects The domain objects to index.  Must be saved.
   * @param archiveReferenceList The list of archive references associated with the list of objects.  (Optional).
   *        This will modify the index used to be the archive index and add the element '_archiveReference' to the
   *        document indexed.
   * @return Status of the index creation.  A Map with the response from the engine.
   */
  Map bulkIndex(List objects, List<String> archiveReferenceList)

  /**
   * Deletes all Indices in the search engine.  This will lose all data there.
   * Can log performance as level INFO log messages.
   *
   * @return Status of the removal.
   */
  Map deleteAllIndices()

  /**
   * Performs a global search using the given query string.
   * @param query The query string.  If it starts with &#123; then the string is used as the body for the GET request.
   * @param params Optional query parameters.  Supported elements: from and size.
   * @param The search results.
   */
  SearchResult globalSearch(String query, Map params)

  /**
   * Performs a standard domain search with the query string.  
   * @param domainClass The domain class to search.
   * @param query The query string.
   * @param params Optional query parameters.  Supported elements: from and size
   * @return The search result, containing the list of values found.
   */
  SearchResult domainSearch(Class domainClass, String query, Map params)

  /**
   * Sets the external search hosts to be used.
   * @param hosts The list of hosts.
   */
  void setHosts(List<HttpHost> hosts)

}
