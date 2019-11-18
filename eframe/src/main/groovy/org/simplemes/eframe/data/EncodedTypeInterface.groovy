package org.simplemes.eframe.data

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

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
@SuppressWarnings("GroovyDocCheck")
interface EncodedTypeInterface {
  /**
   * Returns the encoded ID of this value.
   * @return The encoded value.
   */
  String getId()

}