/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.ISODate

/**
 * A serializer for writing a DateOnly field to a JSON output.
 */
class DateOnlySerializer extends StdSerializer<DateOnly> {

  DateOnlySerializer(Class<DateOnly> t) {
    super(t)
  }

  @Override
  void serialize(DateOnly value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(ISODate.format((DateOnly) value))
  }

}
