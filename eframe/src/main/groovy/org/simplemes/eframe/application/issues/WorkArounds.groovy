/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application.issues

import java.lang.reflect.Modifier


/**
 * Various work-around flags to solve issues in libraries used.
 */
class WorkArounds {
  // TODO: Check for fixes when libraries are updated.

  /**
   * Workaround for micronaut-data issue 264.  Fails on update. Does not use UUID for foreign reference in update.
   * https://github.com/micronaut-projects/micronaut-data/issues/264
   *
   */
  static boolean workAround264 = false

  /**
   * Workaround for micronaut-data issue 192.  Join only finds the first child element.
   * https://github.com/micronaut-projects/micronaut-data/issues/192
   * Fixed on 1/13/2020 for 1.0.0.M6?
   *
   */
  static boolean workAround192 = false

  /**
   * Workaround for micronaut-data issue 323.  Fails on update, uses 'id' instead 'uuid' for the identifier.
   * https://github.com/micronaut-projects/micronaut-data/issues/323
   *
   */
  static boolean workAround323 = false

  /**
   * Workaround for micronaut-data missing optimistic locking.  Probably will be permanent.
   *
   */
  static boolean workAroundOptimistic = false

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
