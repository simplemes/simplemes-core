/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

/**
 * Defines the API for the adjustQuery extension point.  This allows other modules to adjust the search engine
 * query to simplify use cases.
 */
interface AdjustQueryInterface {
  void preAdjustQuery(String queryString, Class domainClass)

  String postAdjustQuery(String response, String queryString, Class domainClass)

}