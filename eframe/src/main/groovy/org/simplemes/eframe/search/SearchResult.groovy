/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.ToString

/**
 * This class contains the results of a search.  This is a lightweight list of hits useful for display.
 */
@ToString(includeNames = true, includePackage = false)
class SearchResult {

  /**
   * The number of total hits available in the search engine.
   */
  int totalHits

  /**
   * The time to process the search.
   */
  long elapsedTime

  /**
   * The list of hits.
   */
  List<SearchHit> hits = []

  /**
   * The query that triggered this search.
   */
  Object query

  /**
   * The start of the result list.
   */
  int offset = 0

  /**
   * The max number of hits requested.
   */
  int max = 10

/**
 * Empty constructor.
 */
  SearchResult() {
  }

  /**
   * Convenience constructor that pulls the values from the search result JSON Map from the search engine.
   * @param jsonMap The parsed JSON (as map) from the search engine.
   */
  SearchResult(Map jsonMap) {
    totalHits = jsonMap.hits.total
    elapsedTime = jsonMap.took as long
    for (hit in jsonMap.hits.hits) {
      hits << buildHit(hit)
    }
  }

  /**
   * Convenience constructor that useful for tests.  Stores the fields from the map as-is into the object like
   * a normal Groovy map constructor.
   * <p>
   * <b>Note:</b> Do not use this in production.
   * @param map The normal groovy map constructor
   * @param hits The domain records to store in the list of hits.  Required for this constructor.
   */
  SearchResult(Map map, List hits) {
    // Support normal Groovy map constructor for easier unit test cases.
    map.each { key, value ->
      if (this.hasProperty((String) key)) {
        this[(String) key] = value
      }
    }

    for (hit in hits) {
      this.hits << new SearchHit(hit)
    }
  }

  /**
   * Builds the correct SearchHit object based on the contents of the hit.  The archive objects
   * are detected here and an ArchiveSearchHit is returned.
   * @param hit The JSON Map for the hit.
   * @return The correct SearchHit class or sub-class.
   */
  SearchHit buildHit(Map hit) {
    if (ArchiveSearchHit.isArchiveHit(hit)) {
      return new ArchiveSearchHit(hit)
    }
    return new SearchHit(hit)
  }

}
