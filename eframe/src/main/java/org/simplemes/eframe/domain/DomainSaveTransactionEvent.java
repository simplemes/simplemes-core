/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain;/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
 */

import org.simplemes.eframe.domain.annotation.DomainEntityInterface;

/**
 * Event published when a single domain record is saved.
 */
public class DomainSaveTransactionEvent {

  /**
   * The object that was saved.
   */
  DomainEntityInterface domainObject;

  /**
   * Constructor.
   *
   * @param domainObject The object that was saved.
   */
  public DomainSaveTransactionEvent(DomainEntityInterface domainObject) {
    this.domainObject = domainObject;
  }

  public DomainEntityInterface getDomainObject() {
    return domainObject;
  }

  @Override
  public String toString() {
    return "DomainSaveTransactionEvent{" +
        "class=" + domainObject.getClass() +
        "uuid=" + domainObject.getUuid() +
        "toString()=" + domainObject +
        '}';
  }
}
