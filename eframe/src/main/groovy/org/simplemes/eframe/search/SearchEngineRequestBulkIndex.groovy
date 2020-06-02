/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.domain.SQLUtils
import org.simplemes.eframe.domain.annotation.DomainEntityHelper
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.LogUtils

import java.util.concurrent.atomic.AtomicInteger

/**
 * This encapsulates a bulk index request on a list of domain objects.  It primarily sends the index request to the
 * external search engine and does minimal processing and error checking.
 *
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>trace</b> - When a bulk index fails, the entire contents are logged (up to 20k chars). </li>
 * </ul>

 */
@Slf4j
@ToString(includePackage = false, includeNames = true)
class SearchEngineRequestBulkIndex implements SearchEngineRequestInterface {

  /**
   * The domain object IDs to index.  These are stored instead of the domains to reduce the memory used while waiting
   * to be executed.
   * Will log an INFO message if an object is deleted between the time ths list is built and when this\request is run.
   * This is probably not a serious error.
   */
  List domainIDs = []

  /**
   *
   */
  Class domainClass

  /**
   * The result from the bulk index action.
   */
  def result

  /**
   * The amount to sleep before running the index request.  Used only for testing and simulating
   * slow responses.
   */
  //static sleepTime = 0

  /**
   * The internal ID counter used to assign a unique bulkID to each request.
   */
  static AtomicInteger idCounter = new AtomicInteger(0)

  /**
   * The semi-unique ID used to identify this request.  Most log messages will have this in the content.
   */
  String bulkID = 'bulk-' + idCounter.incrementAndGet()

  /**
   * Constructor to request an index action on a list of domain objects.
   * @param domainObjects The objects to index.  All entries must be for the same domain (enforced).
   */
  SearchEngineRequestBulkIndex(List domainObjects) {
    ArgumentUtils.checkMissing(domainObjects, 'domainObjects')
    for (object in domainObjects) {
      domainIDs << object.uuid
      def dc = object.getClass()
      if (domainClass && dc != domainClass) {
        throw new IllegalArgumentException("List domainObjects contains mixed domain classes. ${bulkID} " +
                                             "Expected ${domainClass.name}, found ${dc.name} in list $domainObjects")
      }
      domainClass = dc
    }
  }

  /**
   * Returns the domain objects using their IDs stored in domainIDs.
   * @return The list of domain objects.  Logs an info if different from the original list.
   */
  List findRecords() {
    String tableName = DomainEntityHelper.instance.getTableName(domainClass)
    def list = SQLUtils.instance.executeQuery("SELECT * FROM $tableName WHERE uuid IN(?)", domainClass, domainIDs)

    // See if any records are missing (deleted since the list was built).
    if (list.size() != domainIDs.size()) {
      // Figure out which one(s) were removed.
      def missing = []
      for (id in domainIDs) {
        if (!list.any { it.id == id }) {
          // Not found.
          missing << id
        }
      }
      log.warn('getDomains: [{}] Missing domains ignored = {}', bulkID, missing)
    }
    return list
  }

  /**
   * This executes the bulk index action on the external search server.
   */
  @SuppressWarnings("UnnecessaryGetter")
  @Override
  void run() {
    try {
/*
      if (sleepTime) {
        int ms = sleepTime * (2.0 * new SecureRandom().nextDouble())
        log.warn('Sleeping {}ms to simulate slow search engine', ms)
        sleep(ms)
      }
*/
      domainClass.withTransaction {
        def list = findRecords()
        log.info('run: [{}] Starting call to bulkIndex()', bulkID)
        def res = SearchHelper.instance.bulkIndex(list)
        log.info('run: [{}] Done call to bulkIndex()', bulkID)
        // Make sure all objects were indexed
        def items = res.items

        if (items.size() != list.size()) {
          log.error('[{}] Index not created or updated for at least one entry. Found {} results.  Expected {}',
                    bulkID, items.size(), list.size())
        }

        def errorsFound = 0
        for (item in items) {
          def result = item.index.result
          if (!(result == 'created' || result == 'updated')) {
            // Some sort of error, so log it
            log.error('[{}] Index on {} not created or updated.  Response = {}', bulkID, domainClass.simpleName, item)
            errorsFound++
          }
        }
        // Notify the SearchHelper that we finished.
        SearchHelper.instance.finishedBulkRequest(errorsFound)
      }
    } catch (Throwable t) {
      if (log.traceEnabled) {
        domainClass.withNewSession {
          // Log the content if desired
          def s = SearchEngineClient.buildBulkIndexContent(findRecords())
          log.trace('run: [{}] Exception {}.  Content = {}', bulkID, t.toString(), LogUtils.limitedLengthString(s, 20000))
        }
      }
      // Notify the SearchHelper that we finished with an exception.
      SearchHelper.instance.finishedBulkRequest(1)
      throw t
    }
  }

}
