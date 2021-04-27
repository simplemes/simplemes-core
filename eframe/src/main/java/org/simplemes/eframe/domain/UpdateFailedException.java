/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.domain;/*
 * Copyright Michael Houston 2021. All rights reserved.
 * Original Author: mph
 *
 */

/**
 * Exception thrown when a single-record update fails in the JDBC framework.
 * Probably updated by another user.
 */
public class UpdateFailedException extends RuntimeException {

  Object entity;

  /**
   * Entity constructor.
   *
   * @param entity The entity that failed to update.
   */
  public UpdateFailedException(Object entity) {
    super("Update failed (probably updated by another user).  Record: " + entity);
    this.entity = entity;
  }

  @Override
  public String toString() {
    return "Update failed (probably updated by another user).  Record: " + entity;
  }

  public Object getEntity() {
    return entity;
  }

}
