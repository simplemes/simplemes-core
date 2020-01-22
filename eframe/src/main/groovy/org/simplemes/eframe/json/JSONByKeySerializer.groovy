/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.simplemes.eframe.domain.DomainUtils

/**
 * Defines the Jackson serializer that will serialize domain references with just the domain record's primary key field.
 *
 */
class JSONByKeySerializer extends StdSerializer {
  @SuppressWarnings("unused")
  JSONByKeySerializer() {
    this(null)
  }

  JSONByKeySerializer(Class<?> vc) {
    super(vc)
  }

  /**
   * Generates a simple key value (integer) for the node.
   * @param value The domain object.
   * @param gen The generator
   * @param provider The provider.
   */
  @Override
  void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    //println "value = ${value.class}, gen = $gen, provider = $provider"
    def clazz = value.class
    if (!DomainUtils.instance.isDomainEntity(clazz)) {
      def domainName = value.class.simpleName
      throw new IllegalArgumentException("Field with class type $domainName is not a domain class.  Do not use @JSONByKey on non-domain classes.")
    }
    def keyName = DomainUtils.instance.getPrimaryKeyField(clazz)
    gen.writeString((String) value[keyName])
  }

}
