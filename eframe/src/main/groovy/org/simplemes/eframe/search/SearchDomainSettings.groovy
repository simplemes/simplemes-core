/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Defines the search settings for single domain class.
 */
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
class SearchDomainSettings {

  /**
   * If true, then this domain is searchable by the external search engine.
   */
  boolean searchable = true

  /**
   * The fields to exclude from the search of this object.
   */
  List<String> exclude

  void setExclude(@DelegatesTo(SearchDomainSettings) List<String> exclude) {
    this.exclude = exclude
  }

  void setExclude(String excludeOne) {
    if (exclude == null) {
      exclude = []
    }
    exclude << excludeOne
  }
}
