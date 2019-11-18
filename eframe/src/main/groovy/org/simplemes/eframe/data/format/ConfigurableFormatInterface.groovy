package org.simplemes.eframe.data.format

import org.simplemes.eframe.data.FieldDefinitionInterface

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines a custom field format works with a Configurable Type field.
 * This is mainly used to return the list of fields currently selected for the Configurable Type value.
 */
interface ConfigurableFormatInterface {
  /**
   * Gets list of fields currently selected for the Configurable Type value.
   * These are typically treated as normal fields in the GUI and API, but are stored in the
   * custom field holder.
   *
   * @param object The domain object the field is stored in.
   * @param fieldName The name of the configurable field.  Used to find the current value and the fields.
   * @return The list.
   */
  List<FieldDefinitionInterface> getCurrentFields(Object object, String fieldName)

}
