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
   * Permanent fix will need help with JDBC changes covered in issue:
   * https://github.com/micronaut-projects/micronaut-data/issues/299
   *
   */
  static boolean workAroundOptimistic = true

  /**
   * Workaround for micronaut-data cannot detect failed update issue.  Probably will be permanent.
   * Permanent fix will need help with JDBC changes covered in issue:
   * https://github.com/micronaut-projects/micronaut-data/issues/299
   * <p>
   * Note this is tightly linked with the fix workAroundOptimistic.
   * Also, the EFrameJdbcRepositoryOperationsJava class will reference this workAround299 flag through
   * reflection.  Not through a direct reference.
   *
   */
  @SuppressWarnings('unused')
  static public boolean workAround299 = true

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
   * Still broken after fix from 2.1.3.
   *
   */
  static boolean workAround672 = true

  /**
   * Temporary testing work around for the Toolkit sorting using markSorting.  This approach requires two clicks
   * to change sort order from ascending.  Perhaps the code snippet might help to use a different mechanism for
   * initial sorting:
   * https://snippet.webix.com/cgr5o3os
   *
   * This work around does not directly affect the production run-time.  It only helps some tests.
   */
  static boolean workAroundToolkit1 = true

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
