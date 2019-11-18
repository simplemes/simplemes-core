package org.simplemes.eframe.data


/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines an open-ended data type that can be configured by the end-user without extensive GUI or domain changes.
 * This is used to indicate that a given domain property is a configurable type and can be used by the framework in
 * the definition GUIs.
 * <p/>
 * This will typically be extended by a specific field type interface.
 * <p/>
 * See <a href="http://docs.simplemes.org/latest/single.html#configurableTypes">Configurable Types</a> for details.
 *
 */
interface ConfigurableTypeInterface {
  /**
   * Gets the localized display name for this type of configurable object.  This is typically displayed in drop-down lists
   * and other display locations.
   * @param locale The locale to display the name from (if null, defaults to the request locale).
   * @return The display name.
   */
  String toStringLocalized(Locale locale)

  /**
   * Builds the fields needed to configure this type.  This will be used to build the GUI field HTML elements.
   * @param configurableTypeFieldName The name of the Configurable Type field that the caller wants the fields for.
   *                                  This is used to specify the prefix for the values in the custom field holder.
   * @return The list of fields needed to configure this type.  These fields should match the names and types of the
   *         data fields.
   */
  List<FieldDefinitionInterface> determineInputFields(String configurableTypeFieldName)

}
