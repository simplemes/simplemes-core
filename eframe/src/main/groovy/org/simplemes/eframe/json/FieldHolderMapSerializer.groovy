/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.simplemes.eframe.custom.FieldHolderMapInterface

/**
 * A serializer for writing a The FieldHolderMap to JSON output.
 */
class FieldHolderMapSerializer extends StdSerializer<FieldHolderMapInterface> {

  FieldHolderMapSerializer(Class<FieldHolderMapInterface> t) {
    super(t)
  }

  @Override
  void serialize(FieldHolderMapInterface value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeStartObject()
    value.each { k, v ->
      gen.writeObjectField((String) k, v)
    }
    gen.writeEndObject()
  }

}
