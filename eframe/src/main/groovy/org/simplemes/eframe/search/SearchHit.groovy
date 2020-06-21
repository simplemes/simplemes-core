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
   * The name of the index for the search hit.
   */
  String indexName

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
    indexName = jsonMap._index
    domainClass = SearchHelper.instance.getDomainClassForIndex(indexName)
    className = domainClass?.name
    // If the class name can't be reverse-engineered from the index name, the we may need to use the _meta
    // setting on the index itself.  See https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-meta-field.html
  }

  /**
   * Convenience constructor for unit tests to create a hit for an existing domain object.
   * @param domainObject The domain object.
   */
  SearchHit(Object domainObject) {
    object = domainObject
    uuid = domainObject.uuid
    domainClass = domainObject.getClass()
    className = domainObject.getClass().name
  }

  /**
   * This will read the actual domain object from the DB.
   * @return The object.
   */
  Class<DomainEntityInterface> getDomainClass() {
    if (domainClass) {
      return domainClass
    }
    log.warn('Search result for index "{}" is not a valid domain class index.', indexName)
    badClass = true
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
    if (domainClass && getObject()) {
      def root = DomainUtils.instance.getURIRoot(domainClass)
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
      return "${TypeUtils.toShortString(o, true)} - ${o.getClass().simpleName}"
    } else {
      if (badClass) {
        //searchUnknownClass.message=Invalid class {0} for search result.
        return GlobalUtils.lookup('searchUnknownClass.message', null, className ?: indexName)
      } else {
        //searchMissingRecord.message=Search result ({1}:{0}) not found in database.
        return GlobalUtils.lookup('searchMissingRecord.message', null, domainClass?.simpleName, uuid)
      }
    }
  }

}
