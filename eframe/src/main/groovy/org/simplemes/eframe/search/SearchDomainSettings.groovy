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
   * The parent searchable for this class.
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

  /**
   * Constructor for a closure case.
   * @param delegate The closure.
   */
  SearchDomainSettings(Closure delegate) {
    delegate.setDelegate(this)
    delegate.setResolveStrategy(Closure.DELEGATE_FIRST)
    delegate.call()
  }


  void setExclude(@DelegatesTo(SearchDomainSettings) List<String> exclude) {
    this.exclude = exclude
  }

}
