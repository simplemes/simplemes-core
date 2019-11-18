package org.simplemes.eframe.preference

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Serializes a Preference object to JSON for storage.  Supports any arbitrary POGO as a setting in the settings list.
 */
class PreferenceSerializer extends StdSerializer<Preference> {

  /**
   * The empty constructor.
   */
  @SuppressWarnings("unused")
  PreferenceSerializer() {
    this(null)
  }

  /**
   * Constructor for a single preference.
   * @param p
   */
  PreferenceSerializer(Class<Preference> p) {
    super(p)
  }

  /**
   * Serializes a Preference object to a JSON object (text).
   * @param preference The preference to write.
   * @param gen The Jackson generator.
   * @param provider The provider.
   */
  @Override
  void serialize(Preference preference, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeStartObject()
    gen.writeStringField("element", preference.element)
    if (preference.name) {
      gen.writeStringField("name", preference.name)
    }
    if (preference.settings) {
      gen.writeArrayFieldStart("settings")
      // Write the class name, followed by the object itself.
      for (setting in preference.settings) {
        gen.writeString(setting.class.name)
        gen.writeObject(setting)
      }
      gen.writeEndArray()
    }
    gen.writeEndObject()
  }
}
