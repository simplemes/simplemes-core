package org.simplemes.eframe.domain.annotation;

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
 */

import java.util.UUID;

/**
 * A marker interface to indicate this is a domain entity that has some features added by the
 * {@link DomainEntity} annotation.  Also provides some methods added by the annotation.
 */
public interface DomainEntityInterface {

  /**
   * Returns the record UUID.
   *
   * @return The UUID.
   */
  UUID getUuid();

  /**
   * Sets the record UUID.
   *
   * @param uuid The UUID.
   */
  void setUuid(UUID uuid);

  /**
   * Saves the record.
   *
   * @return The record.
   */
  Object save();

  /**
   * Deletes the record.
   *
   * @return The record.
   */
  Object delete();

}
