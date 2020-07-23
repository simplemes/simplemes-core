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

  @Override
  void preAdjustQuery(String queryString, Class domainClass) {

  }

  @Override
  String postAdjustQuery(String response, String queryString, Class domainClass) {
    if (adjust) {
      return response + ".SearchAdjustQueryExtension"
    }
    return response
  }
}
