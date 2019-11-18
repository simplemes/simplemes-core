package org.simplemes.eframe.data


import org.hibernate.EmptyInterceptor
import org.hibernate.type.Type
import org.simplemes.eframe.custom.ExtensibleFieldHelper

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This listener is used to process domain class persistence events from GORM.
 * This is configured in the application.yml setting:
 * <pre>
 *   session_factory:
 *     interceptor: org.simplemes.eframe.data.EFrameHibernatePersistenceInterceptor
 * </pre>
 *
 */
class EFrameHibernatePersistenceInterceptor extends EmptyInterceptor {

  @Override
  boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
    return super.onLoad(entity, id, state, propertyNames, types)
  }

  @Override
  boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
    onSave(entity)
    return super.onSave(entity, id, state, propertyNames, types)
  }


  /**
   * The actual handler for save events.
   * @param entity The domain object being saved.
   */
  void onSave(Object entity) {
    ExtensibleFieldHelper.instance.onObjectSave(entity)
  }


}

