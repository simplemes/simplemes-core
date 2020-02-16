/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * Defines the Jackson serializer that will serialize domain references with just the domain record's ID.
 *
 */
class JSONByIDSerializer extends StdSerializer {
  @SuppressWarnings("unused")
  JSONByIDSerializer() {
    this(null)
  }

  JSONByIDSerializer(Class<?> vc) {
    super(vc)
  }

  /**
   * Generates a simple ID value (integer) for the node.
   * @param value The domain object.
   * @param gen The generator
   * @param provider The provider.
   */
  @Override
  void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    //println "value = ${value.class}, gen = $gen, provider = $provider"
    gen.writeString(value.uuid.toString())
  }

}
