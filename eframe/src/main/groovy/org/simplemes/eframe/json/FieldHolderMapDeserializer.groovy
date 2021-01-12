/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.FieldHolderMap
import org.simplemes.eframe.custom.FieldHolderMapInterface

/**
 * A deserializer for read a FieldHolderMap from JSON.
 * Supports preservation of existing values in fields with a {@link org.simplemes.eframe.data.annotation.ExtensibleFieldHolder}
 * field.
 */
@Slf4j
class FieldHolderMapDeserializer extends StdDeserializer<FieldHolderMapInterface> {

  FieldHolderMapDeserializer(Class<FieldHolderMapInterface> t) {
    super(t)
  }

  /**
   * Deserialize into a FieldHolderMap field.
   * @param p Parsed used for reading JSON content
   * @param context Context that can be used to access information about
   *   this deserialization activity.
   *
   * @return Deserialized value
   */
  @Override
  FieldHolderMapInterface deserialize(JsonParser p, DeserializationContext context) throws IOException, JsonProcessingException {
    def map = new FieldHolderMap(parsingFromJSON: true)
    while (p.nextToken() != JsonToken.END_OBJECT) {
      if (p.currentToken() == JsonToken.FIELD_NAME) {
        def fieldName = p.currentName
        p.nextToken()
        JsonToken t = p.getCurrentToken()
        // Handles native JSON types. Later get() calls may convert type based on _config setting.
        switch (t) {
          case JsonToken.VALUE_STRING:
            map[fieldName] = p.valueAsString
            break
          case JsonToken.VALUE_NUMBER_FLOAT:
            map[fieldName] = new BigDecimal(p.valueAsString)
            break
          case JsonToken.VALUE_NUMBER_INT:
            map[fieldName] = p.valueAsLong
            break
          case JsonToken.VALUE_TRUE:
            map[fieldName] = true
            break
          case JsonToken.VALUE_FALSE:
            map[fieldName] = false
            break
          case JsonToken.VALUE_NULL:
            map[fieldName] = null
            break
          case JsonToken.START_OBJECT:
            // Read any sub-maps as traditional maps.  Not as FieldHolderMap.
            // Mainly used for _config field.
            Map subMap = context.readValue(p, Map)
            map[fieldName] = subMap
            break
        }
      }
    }
    // Now we have finished parsing from JSON, so set the flag correctly.
    map.setParsingFromJSON(false)
    log.trace("deserialize(): Parsed to map: {}", map)
    return map
  }

  /**
   * Alternate deserialization method (compared to the most commonly
   * used, {@link #deserialize(JsonParser, DeserializationContext)}),
   * which takes in initialized value instance, to be
   * configured and/or populated by deserializer.
   * Method is not necessarily used (or supported) by all types
   * (it will not work for immutable types, for obvious reasons):
   * most commonly it is used for Collections and Maps.
   * It may be used both with "updating readers" (for POJOs) and
   * when Collections and Maps use "getter as setter".
   * <p>
   * Default implementation just throws
   * {@link UnsupportedOperationException}, to indicate that types
   * that do not explicitly add support do not necessarily support
   * update-existing-value operation (esp. immutable types)
   * @param p
   * @param context
   * @param intoValue
   */
  @Override
  FieldHolderMapInterface deserialize(JsonParser p, DeserializationContext context, FieldHolderMapInterface intoValue) throws IOException {
    if (intoValue != null) {
      def newMap = deserialize(p, context) as FieldHolderMapInterface
      intoValue.mergeMap(newMap, newMap)  // Uses the new map (from JSON) as context for possible error message.
      return intoValue as FieldHolderMapInterface
    }
    return deserialize(p, context) as FieldHolderMapInterface
  }

}
