/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.misc.ArgumentUtils

/**
 * This encapsulates a index request on a domain object.  It primarily sends the request to the
 * external search engine and does minimal processing and error checking.
 */
@Slf4j
@ToString(includePackage = false, includeNames = true)
class SearchEngineRequestIndexObject implements SearchEngineRequestInterface {

  /**
   * The domain object to index.
   */
  def domainObject

  /**
   * The class for the domain object.
   */
  def domainClass

  /**
   * The ID of the record to process.
   */
  def uuid

  /**
   * Constructor to request an index action on a domain object.
   * @param domainObject
   */
  SearchEngineRequestIndexObject(Object domainObject) {
    ArgumentUtils.checkMissing(domainObject, 'domainObject')
    domainClass = domainObject.getClass()
    uuid = domainObject.uuid
  }

  /**
   * This executes the indexObject action on the external search server.
   */
  @Override
  void run() {
    domainObject = domainClass.findByUuid(uuid)

    def res = SearchHelper.instance.indexObject(domainObject)
    if (!(res?.result == 'created' || res?.result == 'updated')) {
      // Some sort of error, so log it
      log.error('Index not created or updated.  Response = {}', res)
    }
  }
}
