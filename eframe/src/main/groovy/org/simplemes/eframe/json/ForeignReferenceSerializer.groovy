/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.simplemes.eframe.domain.annotation.DomainEntityInterface

/**
 * A serializer for writing foreign domain references with a simple format: just the key field(s) and an ID.
 */
class ForeignReferenceSerializer extends JsonSerializer<Object> {

  /**
   * The key fields for the domain reference.
   */
  List<String> keys

  /**
   * Builds the serializer, which dumps the given key fields.
   * @param keys The key fields.
   */
  ForeignReferenceSerializer(List<String> keys) {
    this.keys = keys
  }

  /**
   * Serializes the domain record reference using the short string (e.g. primary key field).
   * @param value
   * @param gen
   * @param provider
   */
  @Override
  void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException, JsonProcessingException {
    gen.writeStartObject()

    for (key in keys) {
      def object = value[key]
      if (object != null) {
        if (object instanceof DomainEntityInterface) {
          // Don't try to keep the nested object references (a reference in a reference).  Just use the UUID.
          gen.writeStringField(key, object.uuid.toString())
        } else {
          gen.writeStringField(key, object.toString())
        }
      }
    }
    gen.writeStringField('uuid', value.uuid.toString())
    gen.writeEndObject()
  }


}
