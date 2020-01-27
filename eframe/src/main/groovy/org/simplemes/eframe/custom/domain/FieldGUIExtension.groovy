/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import groovy.transform.EqualsAndHashCode
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Transient
import io.micronaut.data.model.DataType
import org.simplemes.eframe.custom.gui.FieldAdjustmentInterface
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.json.TypeableMapper
import org.simplemes.eframe.misc.FieldSizes

import javax.persistence.Column

//import grails.gorm.annotation.Entity

/**
 * This defines the field extensions added to the GUI for a given domain class.
 * This mainly adjusts the order of fields on the standard definition GUIs and lists.
 * This can also add custom fields to those GUIs.
 */
// TODO: Replace with non-hibernate alternative
//@ExtensibleFields()
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ["domainName"])
class FieldGUIExtension {

  /**
   * The domain these GUI extensions are defined for.
   */
  // TODO: DDL Add unique constraint on domainName
  @Column(length = FieldSizes.MAX_CLASS_NAME_LENGTH, nullable = false)
  String domainName

  /**
   * A non-persisted List of adjustments (<b>transient</b>).
   * Always set to an empty list on construction.
   */
  @Transient
  List<FieldAdjustmentInterface> adjustments = []

  /**
   * The JSON form of the adjustments.  This is the value persisted to the database.
   */
  @Column(nullable = true)
  @MappedProperty(type = DataType.STRING, definition = 'TEXT')
  String adjustmentsText

  /**
   * If true, then the JSON of the adjustments has been parsed (after retrieval).  (<b>transient</b>).
   * This is used to make sure the JSON is only parsed once, after retrieval.
   */
  @Transient
  boolean textHasBeenParsed = false

  @SuppressWarnings("unused")
  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE') Date dateCreated

  @SuppressWarnings("unused")
  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE') Date dateUpdated

  Integer version = 0

  @Id @AutoPopulated UUID uuid


  /**
   * Get the Adjustments.  Converts from the persisted JSON format if this is the first call to getAdjustments() after a load.
   * @return The adjustments.
   */
  List<FieldAdjustmentInterface> getAdjustments() {
    // This is done here instead of afterLoad() because of issues with caching and unit testing.
    if (adjustmentsText && !textHasBeenParsed) {
      // Need to parse the JSON into the preferences.
      //println "get JSON = ${groovy.json.JsonOutput.prettyPrint(adjustmentsText)}"
      // Work around the empty list problem in Jackson.  Will fail parsing if the 'settings' are not present.
      if (adjustmentsText?.contains('{')) {
        def reader = new StringReader(adjustmentsText)
        adjustments = TypeableMapper.instance.read(reader)
      } else {
        adjustments = null
      }
      textHasBeenParsed = true
    }
    return adjustments
  }

  /**
   * Persist the transient adjustment List into the JSON for storage in the DB.
   */
  protected void persistAdjustmentsInText() {
    if (adjustments) {
      def writer = new StringWriter()
      TypeableMapper.instance.writeList(writer, adjustments)
      adjustmentsText = writer.toString()
      //println "persist JSON = ${groovy.json.JsonOutput.prettyPrint(adjustmentsText)}"
    } else {
      adjustmentsText = null
    }
    textHasBeenParsed = true
  }

  /**
   * Sets the Adjustments.
   */
  void setAdjustments(List<FieldAdjustmentInterface> adj) {
    adjustments = adj
    // Go ahead and persist now since this is a low-usage method.
    // Originally would persist on save(beforeValidate) but that fails to run on hot-reload for development.
    persistAdjustmentsInText()
  }

  /**
   * Sets the adjustments.   Called by hibernate upon read.
   */
  @SuppressWarnings("unused")
  void setAdjustmentsText(String xml) {
    adjustmentsText = xml
    textHasBeenParsed = false
  }

  /**
   * Removes all references to the given field for the given domain.  If no adjustments are left,
   * then the whole FieldGUIExtension record is deleted too.
   * @param domainName The domain to check for references.
   * @param fieldName The field name to remove.
   */
  static void removeReferencesToField(String domainName, String fieldName) {
    //noinspection UnnecessaryQualifiedReference
    def list = FieldGUIExtension.findByDomainName(domainName)
    // Fully qualified call to findBy() is needed in new session
    for (fieldGUIExtension in list) {
      def removeList = []
      for (adj in fieldGUIExtension.adjustments) {
        // Remove the field from the adjustment.
        if (adj.removeField(fieldName)) {
          // and remember this to remove the adjustment from the list, later.
          removeList << adj
        }
      }
      // Now, remove any adjustments inside of another loop to avoid concurrent update issues.
      for (adj in removeList) {
        fieldGUIExtension.adjustments.remove((Object) adj)
      }
      if (fieldGUIExtension.adjustments.size()) {
        fieldGUIExtension.persistAdjustmentsInText() // Make sure the new list is in JSON
        //println "save fieldGUIExtension = $fieldGUIExtension, JSON = ${fieldGUIExtension.adjustmentsText}"
        fieldGUIExtension.save()
      } else {
        //println "deleting the whole FGE = $fieldGUIExtension"
        fieldGUIExtension.delete()
        //FieldExtensionHelper.deleteCustomChild(fieldGUIExtension)
      }
    }
  }

  /**
   *  Build a human-readable version of this object.
   * @return The human-readable string.
   */
  @Override
  String toString() {
    StringBuilder sb = new StringBuilder("FieldGUIExtension{")
    sb.append("domainName='").append(domainName).append('\'')
    sb.append(", adjustments=").append(getAdjustments())
    sb.append('}')
    return sb.toString()
  }
}
