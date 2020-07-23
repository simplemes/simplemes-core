/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

/**
 * Defines the API for the adjustQuery extension point.  This allows other modules to adjust the search engine
 * query to simplify use cases.
 */
@SuppressWarnings('unused')
interface AdjustQueryInterface {
  /**
   * ExtensionPoint pre method to adjust the query string to make the input more user friendly.
   * @param queryString The input query string from the user. Null not allowed.
   * @param domainClass The domain class for domain-specific searches.  Null allowed.
   * @return The adjusted query string.
   */
  void preAdjustQuery(String queryString, Class domainClass)

  /**
   * ExtensionPoint post method to adjust the query string to make the input more user friendly.
   * @param response The core response.
   * @param queryString The input query string from the user. Null not allowed.
   * @param domainClass The domain class for domain-specific searches.  Null allowed.
   * @return The adjusted query string.
   */
  String postAdjustQuery(String response, String queryString, Class domainClass)

}