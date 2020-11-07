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

  /**
   * The parent searchable domain for this class.  This parent is the domain that will be indexed, not the child domain.
   * This child class is flagged as non-seachable.
   */
  Class parent

  /**
   * Empty constructor.
   */
  SearchDomainSettings() {
  }

  /**
   * Constructor for a Map case.
   * @param options The options.
   */
  SearchDomainSettings(Map options) {
    options.each { k, v ->
      //noinspection GroovyAssignabilityCheck
      this[k] = v
    }
  }

  void setExclude(@DelegatesTo(SearchDomainSettings) List<String> exclude) {
    this.exclude = exclude
  }

  /**
   * Returns the searchable state.
   */
  boolean isSearchable() {
    if (parent) {
      return false
    }
    return searchable
  }

}
