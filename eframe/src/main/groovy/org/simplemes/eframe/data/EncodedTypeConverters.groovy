/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data


import io.micronaut.context.annotation.Factory
import io.micronaut.core.convert.ConversionContext
import io.micronaut.core.convert.TypeConverter

import javax.inject.Singleton

/**
 * Convert to/from SQL String to encoded types.
 */
@Factory
class EncodedTypeConverters {

  @Singleton
  TypeConverter<EncodedTypeInterface, String> encodedToStringConverter() {
    return { EncodedTypeInterface encodedType, Class targetType, ConversionContext context ->
      Optional.of(encodedType.id)
    } as TypeConverter<EncodedTypeInterface, String>
  }

  @Singleton
  TypeConverter<String, EncodedTypeInterface> stringToEncodedConverter() {
    return { String id, Class targetType, ConversionContext context ->
      Optional.of(targetType.valueOf(id))
    } as TypeConverter<String, EncodedTypeInterface>
  }
}
