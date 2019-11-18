package org.simplemes.eframe.test

import org.grails.datastore.gorm.events.ConfigurableApplicationEventPublisher
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEventListener
import org.grails.datastore.mapping.engine.event.EventType
import org.grails.orm.hibernate.HibernateDatastore
import org.simplemes.eframe.data.EFrameHibernatePersistenceInterceptor
import org.springframework.context.ApplicationEvent

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Sub-class of the GORM HibernateDatastore that can be used to configure the session factory for testing.
 * Provides some features from the application.yml setting that do not work in unit test mode.
 */
class EFrameHibernateDatastore extends HibernateDatastore {

  /**
   * Construct a Hibernate datastore scanning the given packages
   *
   * @param configuration The configuration
   * @param packagesToScan The packages to scan
   */
  EFrameHibernateDatastore(Map<String, Object> configuration, Package... packagesToScan) {
    super(DatastoreUtils.createPropertyResolver(configuration), packagesToScan)
  }

  @Override
  protected void registerEventListeners(ConfigurableApplicationEventPublisher eventPublisher) {
    super.registerEventListeners(eventPublisher)
    eventPublisher.addApplicationListener(new EframePersistenceListener(this))
  }

}


/**
 * This listener is used to process domain class persistence events from GORM.
 * This will call the interceptor handlers for the non-test mode cases.
 */
class EframePersistenceListener extends AbstractPersistenceEventListener {

  /**
   * A dummy interceptor that is used to link the Unit Test hibernate session to the EFrame logic for onSave
   * onDelete and other events.
   */
  EFrameHibernatePersistenceInterceptor interceptor

  /**
   * Main constructor.
   * @param dataStore
   */
  EframePersistenceListener(final Datastore dataStore) {
    super(dataStore)
    interceptor = new EFrameHibernatePersistenceInterceptor()
  }

  @Override
  protected void onPersistenceEvent(final AbstractPersistenceEvent event) {
    switch (event.eventType) {
      case EventType.PostInsert:
      case EventType.PostUpdate:
        interceptor.onSave(event.entityObject)
        break

    }

/*
    if (!(eventType == EventType.PostInsert || eventType == EventType.PostDelete
      || eventType == EventType.PostUpdate || eventType == EventType.Validation)) {
      // A quick test to avoid the expensive tests below.
      return
    }

    //println "$event.eventType = $event.entityObject"
*/
  }

  @Override
  boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
    return true
  }
}
