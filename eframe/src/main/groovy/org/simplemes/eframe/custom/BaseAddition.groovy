/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom
/**
 * Base class for most Addition classes.  Helps define the addition's elements to be added to the application.
 * Includes elements like field extensions, new encoded types, etc.
 *
 */
abstract class BaseAddition implements AdditionInterface {
  /**
   * Returns the name of this addition.
   * @return The name.
   */
  @Override
  String getName() {
    return addition.name
  }

  /**
   * Returns the list of encoded field types that data layer will support.
   * @return The list of classes.
   */
  @Override
  List<Class> getEncodedTypes() {
    return addition.encodedTypes
  }

  /**
   * Returns a list of classes that define non-domain classes that will perform initial data loading.
   * These classes need a static initialDataLoad() method.
   * @return The list of other classes
   */
  @Override
  List<Class> getInitialDataLoaders() {
    return addition.initialDataLoaders
  }

  /**
   * Returns the field extensions defined in this addition.
   *
   * @return The field additions.
   */
  @Override
  List<AdditionFieldConfiguration> getFields() {
    return addition.fields
  }

  /**
   * Returns the assets defined in this addition.
   *
   * @return The asset additions.
   */
  @Override
  List<AdditionAssetConfiguration> getAssets() {
    return addition.assets
  }
}
