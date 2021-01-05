/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.data.format.CustomChildListFieldFormat

/**
 * A serializer for writing custom fields to a JSON output.
 * <p>
 *   <b>Note:</b> This serializer depends on the _complexCustomFields Map added to all extensible field domains.
 */
class ComplexCustomFieldSerializer extends JsonSerializer<Object> {
  /**
   * The domain class to serialize the custom fields from.
   */
  Class domainClass

  /**
   * Normal constructor.
   * @param domainClass
   */
  ComplexCustomFieldSerializer(Class domainClass) {
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
    def domainObject = value[ExtensibleFieldHolder.COMPLEX_THIS_NAME]

    def fieldDefinitions = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(domainClass)
    for (field in fieldDefinitions) {
      if (field.format == CustomChildListFieldFormat.instance) {
        def list = ExtensibleFieldHelper.instance.getFieldValue(domainObject, field.name)
        gen.writeArrayFieldStart(field.name)
        for (record in list) {
          gen.writeObject(record)
        }
        gen.writeEndArray()
      }
    }
  }

}
