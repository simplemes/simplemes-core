/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import io.micronaut.transaction.annotation.TransactionalEventListener
import org.simplemes.eframe.domain.DomainSaveTransactionEvent

import javax.inject.Singleton

/**
 * Listens for the transaction commit events on domain objects.
 * Mainly notifies the search logic to (maybe) index or re-index the object.
 */
@Singleton
class SearchTransactionEventListener {

  @SuppressWarnings('unused')
  @TransactionalEventListener
  void onNewSaveEvent(DomainSaveTransactionEvent event) {
    SearchHelper.getInstance().handlePersistenceChange(event.domainObject)
  }

}
