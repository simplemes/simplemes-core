/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.misc.ArgumentUtils

/**
 * This encapsulates a request to remove a domain object from the search engine index.  It primarily sends the request to the
 * external search engine and does minimal processing and error checking.
 */
@Slf4j
@ToString(includePackage = false, includeNames = true)
class SearchEngineRequestObjectRemoval implements SearchEngineRequestInterface {

  /**
   * The domain object to remove from the search engine.
   */
  def domainObject

  /**
   * Constructor to request removal of object from the search engine index.
   * @param domainObject
   */
  SearchEngineRequestObjectRemoval(Object domainObject) {
    ArgumentUtils.checkMissing(domainObject, 'domainObject')
    this.domainObject = domainObject
  }

  /**
   * This executes the indexObject action on the external search server.
   */
  @Override
  void run() {
    def res = SearchHelper.instance.removeObjectFromIndex(domainObject)
    //println "res = $res"
    if (!(res?.result == 'deleted' || res?.result == 'not_found')) {
      // Some sort of error, so log it
      log.error('Object not removed from Index.  Response = {}', res)
    }
  }
}
