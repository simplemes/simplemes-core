package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.ExtensibleFieldHelper

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A serializer for writing custom fields to a JSON output.
 */
class CustomFieldSerializer extends JsonSerializer<Object> {

  /**
   * The domain class to serialize the custom fields from.
   */
  Class domainClass

  /**
   * Normal constructor.
   * @param domainClass
   */
  CustomFieldSerializer(Class domainClass) {
    this.domainClass = domainClass
  }

  /**
   * Serializes the domain record reference using the short string (e.g. primary key field).
   * @param value
   * @param gen
   * @param provider
   */
  @Override
  void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException, JsonProcessingException {
    // We will use a null value since we don't want the raw holder data in the JSON output.
    gen.writeNull()

    if (!value) {
      return
    }

    def fieldDefinitions = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(domainClass)
    for (field in fieldDefinitions) {
      if (field.custom) {
        def map = Holders.objectMapper.readValue((String) value, Map)
        def s = map[field.name]
        gen.writeObjectField(field.name, s)
      }
    }
  }

}
