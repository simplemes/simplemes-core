/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format

import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.domain.annotation.DomainEntityInterface

/**
 * Defines a custom field format that the value needs to be loaded from the database as a list.
 * Mainly used by the custom child list format to load/save the values when needed.
 */
interface ListFieldLoaderInterface {
  /**
   * Gets the given field value from the given object (domain or POGO depending on sub-class).
   *
   * @param object The domain object the field is stored in.
   * @param fieldDefinition The field definition used to define this field (<b>Required</b>).
   * @return The list.
   */
  List readList(DomainEntityInterface object, FieldDefinitionInterface fieldDefinition)

  /**
   * Saves the list field values to the DB.  Relies on the GORM/hibernate save() mechanism and dirty
   * checking for the save.
   *
   * @param object The domain object the field is to be stored in.
   * @param list The field list.
   * @param fieldDefinition The field definition used to define this field (<b>Required</b>).
   */
  void saveList(DomainEntityInterface object, List list, FieldDefinitionInterface fieldDefinition)


}
