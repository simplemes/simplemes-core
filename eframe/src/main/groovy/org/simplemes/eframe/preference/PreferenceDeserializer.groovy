package org.simplemes.eframe.preference


import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.simplemes.eframe.json.TypeableJSONInterface
import org.simplemes.eframe.misc.TypeUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Serializes a Preference object to JSON for storage.  Supports any arbitrary POGO as a setting in the settings list.
 */
class PreferenceDeserializer extends StdDeserializer<Preference> {

  /**
   * The empty constructor.
   */
  PreferenceDeserializer() {
    this(null)
  }

  /**
   * Constructor for a single preference.
   * @param p
   */
  PreferenceDeserializer(Class<Preference> p) {
    super(p)
  }

  /**
   * Method to deserialize a Preference from JSON.
   *
   * @param jsonParser Parsed used for reading JSON content
   * @param context Context that can be used to access information about
   *   this deserialization activity.
   *
   * @return The deserialized value
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  @Override
  Preference deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException, JsonProcessingException {
    // Deserialize the top-level fields first.
    JsonNode node = jsonParser.getCodec().readTree(jsonParser)
    Preference pref = new Preference()
    pref.element = node.element?.textValue()
    pref.name = node.name?.textValue()

    // Now, loop on the list of elements (class name, followed by the JSON for that value).
    for (Iterator<JsonNode> iter = node.settings?.elements(); iter.hasNext();) {
      JsonNode classNode = iter.next()
      def className = classNode?.textValue()

      if (!iter.hasNext()) {
        throw new IllegalArgumentException("Input JSON for Preference has odd number of elements in the array.  No value found after $className")
      }

      JsonNode valueNode = iter.next()

      def clazz = TypeUtils.loadClass(className)
      if (!TypeableJSONInterface.isAssignableFrom(clazz)) {
        throw new IllegalArgumentException("Class ${clazz.name} is not allowed in a Preference.  Only TypeableJSONInterface class allowed.")
      }

      def subParser = valueNode.traverse()
      subParser.setCodec(jsonParser.getCodec())
      def setting = subParser.readValueAs(clazz)
      pref.settings << setting
    }
    return pref
  }
}
