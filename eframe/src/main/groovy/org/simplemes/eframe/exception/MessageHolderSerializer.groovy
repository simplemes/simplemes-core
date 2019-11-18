package org.simplemes.eframe.exception

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This is the Jackson serializer used for a MessageHolder.
 * This is used for a specific, flexible format used for messages in the framework.
 */
class MessageHolderSerializer extends StdSerializer<MessageHolder> {

  /**
   * Empty Constructor.
   */
  MessageHolderSerializer() {
    this(null)
  }

  /**
   * Main constructor.
   * @param t
   */
  MessageHolderSerializer(Class<MessageHolder> t) {
    super(t)
  }

  /**
   * Serializes a MessageHolder.
   *
   * @param value
   * @param gen
   * @param provider
   * @throws IOException
   */
  @Override
  void serialize(MessageHolder value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeStartObject()

    gen.writeFieldName('message')
    gen.writeStartObject()
    gen.writeStringField("text", value.text)
    gen.writeStringField("level", value.levelText)
    gen.writeNumberField("code", value.code)

    // Now, dump the array of other messages
    gen.writeArrayFieldStart('otherMessages')

    for (msg in value.otherMessages) {
      gen.writeStartObject()
      gen.writeStringField("text", msg.text)
      gen.writeStringField("level", msg.levelText)
      gen.writeNumberField("code", msg.code)
      gen.writeEndObject()
    }
    gen.writeEndArray()

    gen.writeEndObject()

    gen.writeEndObject()
  }

}
