package org.simplemes.eframe.data


import org.simplemes.eframe.misc.TypeUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides utility methods to access a list of choices that implement the ChoiceListInterface.
 */
class EncodedTypeListUtils {

  /**
   * A singleton to access this utility class methods/data.  Allow easy mocking in unit tests.
   */
  static EncodedTypeListUtils instance = new EncodedTypeListUtils()

  /**
   * Returns the list of valid values for the given ChoiceList base class.
   *
   * @return The list of valid values (instances).
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  List<ChoiceListItemInterface> getAllValues(Class baseClass) {
    // Make sure the class implements the ChoiceListInterface marker interface.
    if (!ChoiceListInterface.isAssignableFrom(baseClass)) {
      throw new IllegalArgumentException("baseClass '$baseClass.name' does not implement the ChoiceListInterface.")
    }
    // Get the core values first.
    def coreList = TypeUtils.getStaticProperty(baseClass, 'coreValues')

    def res = []
    for (clazz in coreList) {
      res << clazz.instance
    }

/*
    // Now, add any from the current additions
    for (addition in AdditionHelper.instance.additions) {
      // Consider DSL with clearer definition and after/before logic.
      def addedOptions = TypeUtils.getStaticProperty(addition.additionClass, 'addedOptions')

      if (addedOptions) {
        def addedList = addedOptions[baseClass]
        for (added in addedList) {
          res << added.instance
        }
      }
    }
*/


    return res
  }


}