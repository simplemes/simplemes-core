/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.domain

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * This is triggered when an update fails due to optimistic lock failure (version mis-match or record deleted).
 */
class OptimisticLockException extends RuntimeException {

  UpdateFailedException cause

  OptimisticLockException(UpdateFailedException cause) {
    this.cause = cause
  }


  @Override
  String toString() {
    def entity = cause?.entity
    if (entity?.hasProperty('version')) {
      //error.210.message=Record updated by another user.  {0}: {1}, version: {2}.
      return GlobalUtils.lookup('error.210.message', null, cause?.entity?.getClass()?.simpleName,
                                TypeUtils.toShortString(cause?.entity), entity?.version)

    } else {
      //println "entity2 = $entity.class ${TypeUtils.toShortString(cause?.entity)}"
      //error.211.message=Record updated by another user.  {0}: {1}.
      return GlobalUtils.lookup('error.211.message', null, cause?.entity?.getClass()?.simpleName,
                                TypeUtils.toShortString(cause?.entity))

    }
  }
}
