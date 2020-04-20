/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.domain.annotation.DomainEntityInterface

/**
 * Defines the Jackson deserializer that will serialize domain references with just the domain record's ID.
 *
 */
class JSONByIDDeserializer extends StdDeserializer {
  @SuppressWarnings("unused")
  JSONByIDDeserializer() {
    this(null)
  }

  JSONByIDDeserializer(Class<?> vc) {
    super(vc)
  }

  /**
   * Converts an object ID to a domain object.
   *
   * @param p Parsed used for reading JSON content
   * @param context Context that can be used to access information about
   *   this deserialization activity.
   *
   * @return Deserialized value
   */
  @Override
  Object deserialize(JsonParser p, DeserializationContext context) throws IOException, JsonProcessingException {
    def fieldName = p.getCurrentName()
    def clazz = findTypeFromParser(p) ?: DomainUtils.instance.getDomain(fieldName)
    if (!clazz) {
      throw new IllegalArgumentException("Could not find domain class for $fieldName.  Do not use @JSONByID on non-domain fields.")
    }
    if (!DomainEntityInterface.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException("Class for field '$fieldName' is not a domain class ($clazz.name).  Do not use @JSONByID on non-domain fields.")
    }
    def s = p.getText()
    def record = clazz.findByUuid(UUID.fromString(s))
    if (!record) {
      throw new IllegalArgumentException("@JSONByID Could not find $fieldName record for uuid $s")
    }
    return record
  }

  /**
   * Determines the type from the parser (if possible).
   * @param p The parser.
   */
  Class findTypeFromParser(JsonParser p) {
    def currentValue = p.currentValue
    if (currentValue) {
      def clazz = currentValue.getClass()
      def theField = clazz.getDeclaredField(p.currentName)
      return theField.type
    }
    return null
  }

}
