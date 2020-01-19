/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data

import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType

/**
 * Flags a basic type that is stored as an encoded value in the database.  Each concrete class
 * provides an ID that is stored in the database.  The base class also provides a valueOf() static
 * method to convert from the database (encoded) value to a real instance.
 *
 * <p>
 * <b>Note:</b> This works in conjunction with {@link ChoiceListInterface} and {@link ChoiceListItemInterface} to define
 *    the common type for drop-downs in GUIs.
 *    <p>
 * See <a href='http://docs.simplemes.org'>http://docs.simplemes.org</a>
 *    <p>
 */
@TypeDef(type = DataType.STRING)
@SuppressWarnings("GroovyDocCheck")
interface EncodedTypeInterface {
  /**
   * Returns the encoded ID of this value.
   * @return The encoded value.
   */
  String getId()

}