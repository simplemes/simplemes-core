package org.simplemes.eframe.custom
/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

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
   * Returns the list of encoded field types that Hibernate will support.
   * @return The list of classes.
   */
  @Override
  List<Class> getEncodedTypes() {
    return addition.encodedTypes
  }

  /**
   * Returns a list of classes that define the top-level domain classes that help hibernate find the domain classes.
   * This is usually a dummy class at the highest package that will contain all of the domain classes.
   * Hibernate will search this package and all sub-packages for valid domain classes.
   * @return The list of top-level domain classes.
   */
  @Override
  List<Class> getDomainPackageClasses() {
    return addition.domainPackageClasses
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
}
