/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application

import groovy.transform.ToString
import io.micronaut.context.annotation.ConfigurationProperties
import org.simplemes.eframe.security.WorkAround333CookieLoginHandler

/**
 * Defines the main, global framework configuration values.  This is normally accessed from the Holders utility
 * class:
 * <pre>
 *   def max = Holders.configuration.maxRowLimit
 * </pre>
 *
 */
@ToString(includeNames = true, includePackage = false)
@ConfigurationProperties('eframe')
class EFrameConfiguration {

  /**
   * The maximum number of rows for reports.  Can be overridden in the application.yml property
   *
   */
  static final Integer REPORT_MAX_ROW_COUNT = 1000

  /**
   * The maximum number of rows for reports.  Can be overridden in the application.yml property
   *
   */
  static final Integer REPORT_ROW_COUNT = 100


  /**
   * The absolute maximum row limit for a single query in most GUIs (<b>Default:</b> 100).
   */
  Integer maxRowLimit = 100

  /**
   * The Application's short name (<b>Default:</b> EFrame).
   */
  String appName = 'EFrame'

  /**
   * Flags un-localized text with '-== ==-' for easy detection in GUI tests (<b>Default:</b> false).
   */
  boolean localizationTest = false

  /**
   * A delay (in ms) for testing purposes.  Applies to limit API calls such as standard BaseCrudController.list() method.
   */
  Integer testDelay = null
  // Use environment EFRAME_TEST_DELAY=1000

  /**
   * The time to allow the browser to cache some stable/static resources (in seconds).  This reduces the server-traffic
   * for relatively static resources.   (<b>Default:</b> 24 hours).
   * <h3>Stable Resources Affected</h3>
   * <ul>
   *   <li><b>TaskMenu</b> - The task menu. </li>
   * </ul>
   */
  Integer cacheStableResources = 24 * 3600

  /**
   * Archive-related configurations.
   */
  Archive archive = new Archive()

  /**
   * Report-related configurations.
   */
  Report report = new Report()

  /**
   * Search-related configurations.
   */
  Search search = new Search()

  /**
   * Security-related configurations.
   */
  Security security = new Security()

  @ToString(includeNames = true, includePackage = false)
  @ConfigurationProperties('archive')
  static class Archive {
    /**
     * The top folder for the archive file storage.  (<b>Default:</b> 'archives').
     */
    String topFolder = 'archives'

    /**
     * The archiver factory to use.  (<b>Default:</b> 'org.simplemes.eframe.archive.ArchiverFactory').
     */
    String archiver = 'org.simplemes.eframe.archive.FileArchiver'

    /**
     * The folder to store the archives in.  (<b>Default:</b> '#{year}-#{month}-#{day}').
     */
    String folderName = '#{year}-#{month}-#{day}'

    /**
     * The file to store the archive in.  (<b>Default:</b> '#{key}').
     */
    String fileName = '#{key}'

    /**
     * If true, then the Archive contents are re-read to make sure the objects were written correctly. (<b>Default</b>: true)
     */
    boolean verify = true

    /**
     * If true, then an ArchiveLog record is created for each archive created. (<b>Default</b>: true)
     */
    boolean log = true

    /**
     * The number days before 'old' objects are archived.  Each module decides which object(s) are archived with
     * this age horizon.
     */
    Integer ageDays = 180

  }

  @ToString(includeNames = true, includePackage = false)
  @ConfigurationProperties('report')
  static class Report {

    /**
     * The default row limit for reports using the third-party report engine (<b>Default:</b> 100).
     */
    Integer rowLimit = REPORT_ROW_COUNT

    /**
     * The absolute max row limit for reports using the third-party report engine (<b>Default:</b> 1000).
     *
     */
    // Unused since the row limit is not exposed to HTTP access/
    //Integer maxRowLimit = REPORT_MAX_ROW_COUNT

  }

  /**
   * The config properties used for the external search engine interface.
   */
  @ToString(includeNames = true, includePackage = false)
  @ConfigurationProperties('search')
  static class Search {

    /**
     * The default request thread pool size (<b>Default:</b> 4).
     */
    Integer threadInitSize = 4

    /**
     * The max limit on the request thread pool size (<b>Default:</b> 10).
     */
    Integer threadMaxSize = 10

    /**
     * The number of index requests to pack into a single request to the search engine(<b>Default:</b> 50).
     * This is used only for the bulk indexing.
     */
    Integer bulkBatchSize = 50

    /**
     * The search engine server (host) that will provide the search functions.
     */
    List hosts = []

  }

  /**
   * The config properties used for the framework-specific security settings.
   */
  @ToString(includeNames = true, includePackage = false)
  @ConfigurationProperties('security')
  static class Security {

    /**
     * The default max age for the workaround JWT Refresh Cookie (seconds).  Set in EFrameConfiguration.jwtRefreshMaxAge.
     * This should be set in the micronaut-security configuration when issue 333 is fixed.
     */
    Long jwtRefreshMaxAge = WorkAround333CookieLoginHandler.DEFAULT_MAX_AGE

  }

}
