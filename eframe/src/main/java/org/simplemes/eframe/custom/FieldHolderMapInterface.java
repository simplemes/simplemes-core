package org.simplemes.eframe.custom;

/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */


import java.util.Map;

/**
 * Defines the Map interface used to hide the ExtensibleFieldHolder extensions to the Map logic.
 * This includes serializing/deserializing to/from JSON, retaining data type info and tracking history of
 * values.  The main implementation is in the Groovy source tree.
 * <p>
 * This works with the {@link org.simplemes.eframe.data.annotation.ExtensibleFieldHolderTransformation}  and
 * {@link ExtensibleFieldHelper} classes to convert the Map to/from JSON upon use.
 * <p>
 * Extra elements are added to the map to retain the data type for values, the history and configuration data
 * in case the definitions change (e.g. FlexType changes).
 */
public interface FieldHolderMapInterface extends Map {
  /**
   * Returns true if the Map has changed since the last time the JSON was generated (toJSON()).
   *
   * @return True if dirty.
   */
  boolean isDirty();

  /**
   * Sets the dirty flag.
   *
   * @param dirty True if the Map has changed since the last time the JSON was generated (toJSON()).
   */
  void setDirty(boolean dirty);

  /**
   * Returns True if the Map has changed since the last time the JSON was generated (toJSON()).
   * Defaults to true to work around issues with JSON parser.
   *
   * @return True if dirty.
   */
  boolean isParsingFromJSON();

  /**
   * RSet to True if the Map has changed since the last time the JSON was generated (toJSON()).
   * Defaults to true to work around issues with JSON parser.
   *
   * @param dirty True if the Map has changed since the last time the JSON was generated (toJSON()).
   */
  void setParsingFromJSON(boolean dirty);

  /**
   * Serializes this Map to JSON.
   *
   * @return The JSON.
   */
  String toJSON();

  /**
   * Merges the given map into this map.  Preserves the _config element and may add to the history, if
   * configured.
   *
   * @param src     The source Map.
   * @param context The place that triggered this.  Usually a domain entity.  Used for errors.
   */
  void mergeMap(FieldHolderMapInterface src, Object context);


}
