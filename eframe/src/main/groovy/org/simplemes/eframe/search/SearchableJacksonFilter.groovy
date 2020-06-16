/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.PropertyWriter
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter

/**
 * Defines a filter that looks at the searchable static field for any excluded fields and
 * removes them from the JSON for the search engine.
 */
class SearchableJacksonFilter extends SimpleBeanPropertyFilter {

  /**
   * The list of excluded fields.
   */
  List<String> excludes

  /**
   * The list of standard fields that are excluded.
   */
  static List<String> stdExcludes = ['dateCreated', 'dateUpdated', 'uuid', 'version']

  /**
   * If true, then exclude all fields that start with underscore.
   */
  static boolean excludeUnderScores = true

  /**
   * Constructor that supports a list of fields to exclude from the serialized JSON.
   * @param excludes The list of excluded fields.
   */
  SearchableJacksonFilter(List<String> excludes) {
    this.excludes = excludes
  }

  @Override
  void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
    //println "writer.name = $writer.name"
    if (isAllowed(writer.name)) {
      super.serializeAsField(pojo, jgen, provider, writer)
    }
  }

  /**
   * Determines if the field is allowed (e.g. not excluded).
   * @param fieldName The field.
   * @return True if the field is allowed.
   */
  boolean isAllowed(String fieldName) {
    if (excludes?.contains(fieldName)) {
      return false
    }
    if (stdExcludes.contains(fieldName)) {
      return false
    }
    if (excludeUnderScores && fieldName[0] == '_') {
      return false
    }

    return true
  }
}
