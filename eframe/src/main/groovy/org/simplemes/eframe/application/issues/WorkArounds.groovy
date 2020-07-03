/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application.issues

import java.lang.reflect.Modifier


/**
 * Various work-around flags to solve issues in libraries used.
 */
class WorkArounds {

  /**
   * Workaround for micronaut-data missing optimistic locking.  Probably will be permanent.
   * Permanent fix will need help with JDBC changes covered in enhancement:
   * https://github.com/micronaut-projects/micronaut-data/issues/299
   *
   */
  static boolean workAroundOptimistic = true

  /**
   * This make small changes to the SQLUtils to force UUIDs to strings for dynamic SQl queries.  This
   * is needed since setting the column type to UUID in Postgres prevents inserts with vanilla Micronaut Data.
   * No bug logged yet.  Affects prepareStatement.  Core MN-Data functions that fail with uuid column type: findById(), insert.
   * Seems to be an issue with the WHERE clause.  Re-evaluate after MN-Data 1.1.1 upgrade and/or conversion to test
   * using postgres.
   *
   */
  static boolean workAroundUuidString = true

  /**
   * Lists the current work arounds enabled.
   * @return
   */
  static List list() {
    def list = []
    def fields = this.declaredFields
    for (field in fields) {
      //println "$field.name"
      if (Modifier.isStatic(field.modifiers) && field.name.startsWith('workAround')) {
        field.setAccessible(true)
        def state = field.get(this)
        if (state) {
          list << "$field.name($state)"
        }
      }
    }
    return list
  }

}
