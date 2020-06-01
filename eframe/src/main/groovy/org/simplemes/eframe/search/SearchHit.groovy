/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.domain.annotation.DomainEntityInterface
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * A single search hit from the search engine.
 */
@Slf4j
@ToString(includePackage = false, includeNames = true)
class SearchHit {

  /**
   * The object ID.
   */
  UUID uuid

  /**
   * The class name of the domain.
   */
  String className

  /**
   * The object (lazy-loaded).
   */
  Object object

  /**
   * The object's domain class (lazy-loaded).
   */
  Class domainClass

  /**
   * Indicates the class was bad, so we can display a meaningful message.
   */
  boolean badClass = false

  /**
   * Empty constructor.
   */
  SearchHit() {
  }

  /**
   * Convenience constructor that pulls the values from the search result JSON Map from the search engine.
   * @param jsonMap The parsed JSON (as  map) from the search engine.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  SearchHit(Map jsonMap) {
    uuid = UUID.fromString((String) jsonMap._id)
    def object = jsonMap._source
    def key = object.keySet()[0]
    def domainObject = object[key]
    className = domainObject['class']
  }

  /**
   * Convenience constructor for unit tests to create a hit for an existing domain object.
   * @param domainObject The domain object.
   */
  SearchHit(Object domainObject) {
    object = domainObject
    uuid = domainObject.uuid
    className = domainObject.getClass().name
  }

  /**
   * This will read the actual domain object from the DB.
   * @return The object.
   */
  Class<DomainEntityInterface> getDomainClass() {
    try {
      domainClass = domainClass ?: TypeUtils.loadClass(className)
    } catch (ClassNotFoundException ignored) {
      log.warn('Search result for class "{}" is not a valid class.', className)
      badClass = true
    }
    return domainClass
  }

  /**
   * This will read the actual domain object from the DB.
   * @return The object.
   */
  Object getObject() {
    object = object ?: getDomainClass()?.findByUuid(uuid)
    return object
  }

  /**
   * Builds the link HREF for this search hit.
   * @return The link.  Can be null for missing records.
   */
  String getLink() {
    def o = getObject()
    if (o) {
      def root = DomainUtils.instance.getURIRoot(o.getClass())
      return "/${root}/show/${uuid}"
    } else {
      return null
    }
  }

  /**
   * Builds the display value for this hit.
   * @return The display value.
   */
  String getDisplayValue() {
    def o = getObject()
    if (o) {
      return "${TypeUtils.toShortString(o)} - ${o.getClass().simpleName}"
    } else {
      if (badClass) {
        //searchUnknownClass.message=Invalid class {0} for search result.
        return GlobalUtils.lookup('searchUnknownClass.message', className)
      } else {
        //searchMissingRecord.message=Search result ({1}:{0}) not found in database.
        return GlobalUtils.lookup('searchMissingRecord.message', domainClass?.simpleName, uuid)
      }
    }
  }

}
