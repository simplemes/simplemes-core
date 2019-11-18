package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.simplemes.eframe.domain.DomainUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the Jackson deserializer that will serialize domain references with just the domain record's primary key field.
 *
 */
class JSONByKeyDeserializer extends StdDeserializer {
  @SuppressWarnings("unused")
  JSONByKeyDeserializer() {
    this(null)
  }

  JSONByKeyDeserializer(Class<?> vc) {
    super(vc)
  }

  /**
   * Converts an object key to a domain object.
   *
   * @param p Parsed used for reading JSON content
   * @param context Context that can be used to access information about
   *   this deserialization activity.
   *
   * @return Deserialized value
   */
  @Override
  Object deserialize(JsonParser p, DeserializationContext context) throws IOException, JsonProcessingException {
    def domainName = p.getCurrentName()
    def clazz = DomainUtils.instance.getDomain(domainName)
    if (!clazz) {
      throw new IllegalArgumentException("Could not find domain class for $domainName.  Do not use @JSONByKey on non-domain classes.")
    }
    def key = p.getText()
    def record = DomainUtils.instance.findDomainRecord(clazz, key)
    if (!record) {
      throw new IllegalArgumentException("@JSONByKey Could not find $domainName record for key $key")
    }
    return record
  }

}
