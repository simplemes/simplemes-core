/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json


import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.ISODate

/**
 * A deserializer for read a DateOnly field from JSON.
 */
class DateOnlyDeserializer extends StdDeserializer<DateOnly> {

  DateOnlyDeserializer(Class<DateOnly> t) {
    super(t)
  }

  /**
   * Deserialize into a DateOnly field.
   * @param p Parsed used for reading JSON content
   * @param context Context that can be used to access information about
   *   this deserialization activity.
   *
   * @return Deserialized value
   */

  @Override
  DateOnly deserialize(JsonParser p, DeserializationContext context) throws IOException, JsonProcessingException {
    return ISODate.parseDateOnly(p.text)
  }
}
