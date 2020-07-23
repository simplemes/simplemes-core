/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample

import org.simplemes.eframe.search.AdjustQueryInterface

import javax.inject.Singleton

/**
 *
 */
@Singleton
class SearchAdjustQueryExtension implements AdjustQueryInterface {

  /**
   * Allows the value to be adjusted during a specific test.
   */
  static boolean adjust = false

  /**
   * ExtensionPoint pre method to adjust the query string to make the input more user friendly.
   * @param queryString The input query string from the user. Null not allowed.
   * @param domainClass The domain class for domain-specific searches.  Null allowed.
   * @return The adjusted query string.
   */
  @Override
  void preAdjustQuery(String queryString, Class domainClass) {

  }

  /**
   * ExtensionPoint post method to adjust the query string to make the input more user friendly.
   * @param response The core response.
   * @param queryString The input query string from the user. Null not allowed.
   * @param domainClass The domain class for domain-specific searches.  Null allowed.
   * @return The adjusted query string.
   */
  @Override
  String postAdjustQuery(String response, String queryString, Class domainClass) {
    if (adjust) {
      return response + ".SearchAdjustQueryExtension"
    }
    return response
  }
}
