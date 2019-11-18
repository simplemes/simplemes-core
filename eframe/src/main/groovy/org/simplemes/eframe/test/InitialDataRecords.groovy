package org.simplemes.eframe.test

import groovy.transform.ToString

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A holder that records the initial data records that are normally loaded by all initial data loaders.
 * This is primarily used by tests that need the initial data records and that should leave the records
 * alone during cleanup.
 * <p>
 * The loaders will register their data every time the load method is called, even if the data is already loaded.
 */
@ToString(includeNames = true, includePackage = false)
class InitialDataRecords {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static InitialDataRecords instance = new InitialDataRecords()

  /**
   * A list of allowed left-over records.  These will not be deleted automatically on exit.
   */
  Map<String, List<String>> records = [:]

  /**
   * Register any possible new records.  These will be added to the list, if not already in the list.
   * @param newRecords The records to add.  The key for the map is the domain class's simpleName and the value array
   *        contains the TypeUtils.toShortString() for the record.
   * @param clazz The domain/loader class being loaded.   This is used to duplicate the records for all parent classes.
   *              This is done since the parent class's list() method finds the child records by default.
   */
  void register(Map<String, List<String>> newRecords, Class clazz) {
    newRecords.each { String k, List<String> newList ->
      def list = records[k]
      if (list == null) {
        // A new domain for the main map
        list = []
        records[k] = list
      }
      for (o in newList) {
        if (!list.contains(o)) {
          list << o
        }
      }
      // Now, attempt to add the same list for any parent classes
      if (clazz.superclass != Object) {
        def parentClazz = clazz.superclass
        def map = [:]
        map[parentClazz.simpleName] = newList
        register((Map) map, (Class) parentClazz)
      }
    }
  }


}
