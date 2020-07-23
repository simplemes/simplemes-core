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
   * Works around use of deprecated constructor in the AssetPipelineService.
   * https://github.com/bertramdev/asset-pipeline/issues/265
   *
   */
  static boolean workAround265 = true

  /**
   * Works around Micronaut Data issue with updating foreign references (ManyToOne with a targetEntity).
   *
   * See issue https://github.com/micronaut-projects/micronaut-data/issues/671
   *
   */
  static boolean workAround671 = true

  /**
   * Works around Micronaut Data issue with the JsonCodec wrapping JSONB data in quotes and escaping them.
   *
   * See issue https://github.com/micronaut-projects/micronaut-data/issues/672
   *
   */
  static boolean workAround672 = true

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
