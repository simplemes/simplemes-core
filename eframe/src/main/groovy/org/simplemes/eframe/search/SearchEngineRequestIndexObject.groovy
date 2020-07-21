/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
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
    int count = 0
    while (domainObject == null) {
      // Attempt to read.  Allows retry to wait for record to commit.
      if (Holders.environmentTest) {
        // Tests won't retry (see below), so we need to always wait for the records to commit.
        // This is ugly, but the alternative is to use the MN-Data TransactionalEventListener.
        sleep(100)
      }
      domainObject = domainClass.findByUuid(uuid)
      count++
      if (!domainObject) {
        if (Holders.environmentTest) {
          // We don't want to keep re-trying since that causes problems in some tests that waitForIdle on the thread pool.
          // A retry loop will cause tests to wait forever in some cases.
          log.debug("Index could not find record ${domainClass?.simpleName} $uuid.  Ignoring.")
          return
        }
        // Still no record, so wait a little while
        sleep(100)
        if (count > 10) {
          log.error("Index could not find record ${domainClass?.simpleName} $uuid after 10 tries.  Ignoring.")
          return
        }
      }
    }

    def res = SearchHelper.instance.indexObject(domainObject)
    if (!(res?.result == 'created' || res?.result == 'updated')) {
      // Some sort of error, so log it
      log.error('Index not created or updated.  Response = {}', res)
    }
  }
}
