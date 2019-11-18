package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.simplemes.eframe.misc.NameUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A serializer for writing enum instances.  Writes the standard toString() value and a display value.
 */
class EnumSerializer extends JsonSerializer<Object> {

  String fieldName

  EnumSerializer(String fieldName) {
    this.fieldName = fieldName
  }

  /**
   * Serializes the enum using the toString() and adding a display value.
   * @param value
   * @param gen
   * @param provider
   */
  @Override
  void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException, JsonProcessingException {
    gen.writeString(value?.toString())
    gen.writeStringField(NameUtils.buildDisplayFieldNameForJSON(fieldName), value?.toStringLocalized())
  }


}
