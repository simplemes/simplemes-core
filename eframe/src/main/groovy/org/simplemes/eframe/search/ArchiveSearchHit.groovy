/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.ToString
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils

/**
 * A single archive search hit from the search engine.
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true)
class ArchiveSearchHit extends SearchHit {

  /**
   * The archive reference for the object.
   */
  String archiveReference

  /**
   * Empty constructor.
   */
  ArchiveSearchHit() {
  }

  /**
   * Convenience constructor that pulls the values from the search result JSON Map from the search engine.
   * @param jsonMap The parsed JSON (as  map) from the search engine.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  ArchiveSearchHit(Map jsonMap) {
    super(jsonMap)
    def jsonObject = jsonMap._source
    archiveReference = jsonObject['_archiveReference']
  }

  /**
   * This returns the archive reference since the archive object is not in the DB.
   * @return The ref.
   */
  Object getObject() {
    return archiveReference
  }

  /**
   * Builds the link HREF for this search hit.
   * @return The link.
   */
  String getLink() {
    def name = DomainUtils.instance.getURIRoot(getDomainClass())
    return "/${name}/showArchive?ref=${archiveReference}"
  }

  /**
   * Builds the display value for this hit.
   * @return The display value.
   */
  String getDisplayValue() {
    // archivedObject.label=Archived {0} on file {1}
    return GlobalUtils.lookup('archivedObject.label', domainClass.simpleName, archiveReference)
  }

  /**
   * Determines if the given hit result is an archived object reference.
   * @param jsonMap The hit details from the search result.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  static isArchiveHit(Map jsonMap) {
    def jsonObject = jsonMap._source
    return jsonObject['_archiveReference'] != null
  }

}
