/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.archive.ArchiverFactory
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.LogUtils

/**
 * This encapsulates a bulk index request on a list of archive references.  It primarily sends the index request to the
 * external search engine and does minimal processing and error checking.
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>trace</b> - When a bulk index fails, the entire contents are logged (up to 20k chars). </li>
 * </ul>

 */
@Slf4j
@ToString(includePackage = false, includeNames = true)
class SearchEngineRequestBulkArchiveIndex implements SearchEngineRequestInterface {

  /**
   * The suffix to add to all indices created for archived elements.
   */
  public static final String INDEX_SUFFIX = '-arc'

  /**
   * The archive file references to index.  These are stored instead of the domains to reduce the memory used while waiting
   * to be executed.
   */
  List archiveRefs = []

  /**
   * The result from the bulk index action.
   */
  def result

  /**
   * Constructor to request an index action on a list of archive object references.
   * @param archiveRefs The archive references for the objects to index.
   */
  SearchEngineRequestBulkArchiveIndex(List<String> archiveRefs) {
    ArgumentUtils.checkMissing(archiveRefs, 'archiveRefs')
    this.archiveRefs = archiveRefs
  }

  /**
   * Returns the domain objects using their IDs stored in domainIDs.
   * @return The list of domain objects.  Logs an info if not found or other error.
   */
  List getDomains() {
    def list = []
    for (ref in archiveRefs) {
      def archiver = ArchiverFactory.instance.archiver
      println "ref = $ref"
      def objects = archiver.unarchive(ref, false)
      for (object in objects) {
        if (SearchHelper.instance.isSearchable(object.getClass())) {
          list << object
        }
      }
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
      def list = getDomains()
      def res = SearchHelper.instance.bulkIndex(list, archiveRefs)
      // Make sure all objects were indexed
      def items = res.items

      if (items.size() != list.size()) {
        log.error('Index not created or updated for at least one entry. Found {} results codes.  Expected {}.',
                  items.size(), list.size())
      }

      def errorsFound = 0
      for (item in items) {
        def result = item.index.result
        if (!(result == 'created' || result == 'updated')) {
          // Some sort of error, so log it
          log.error('Index on {} not created or updated.  Response = {}', list, item)
          errorsFound++
        }
      }
      // Notify the SearchHelper that we finished.
      SearchHelper.instance.finishedBulkRequest(errorsFound)
    } catch (Throwable t) {
      if (log.traceEnabled) {
        // Log the content if desired
        def s = SearchEngineClient.buildBulkIndexContent(getDomains())
        log.trace('run: Exception {}.  Content = {}', t.toString(), LogUtils.limitedLengthString(s, 20000))
      }
      // Notify the SearchHelper that we finished with an exception.
      SearchHelper.instance.finishedBulkRequest(1)
      throw t
    }
  }

}
