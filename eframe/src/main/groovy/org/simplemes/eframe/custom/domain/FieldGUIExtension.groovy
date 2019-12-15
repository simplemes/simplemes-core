package org.simplemes.eframe.custom.domain

//import grails.gorm.annotation.Entity
import org.simplemes.eframe.custom.gui.FieldAdjustmentInterface
import org.simplemes.eframe.json.TypeableMapper
import org.simplemes.eframe.misc.FieldSizes

/**
 * This defines the field extensions added to the GUI for a given domain class.
 * This mainly adjusts the order of fields on the standard definition GUIs and lists.
 * This can also add custom fields to those GUIs.
 */
//@Entity
class FieldGUIExtension {
  String domainName

  /**
   * A non-persisted List of adjustments (<b>transient</b>).
   * Always set to an empty list on construction.
   */
  List<FieldAdjustmentInterface> adjustments = []

  /**
   * The JSON form of the adjustments.  This is the value persisted to the database.
   */
  String adjustmentsText

  /**
   * If true, then the JSON of the adjustments has been parsed (after retrieval).  (<b>transient</b>).
   * This is used to make sure the JSON is only parsed once, after retrieval.
   */
  boolean textHasBeenParsed = false

  /**
   * The date this record was last updated.
   */
  Date lastUpdated

  /**
   * The date this record was created
   */
  @SuppressWarnings("unused")
  Date dateCreated

  /**
   * Internal Mapping of fields to columns.
   */
  @SuppressWarnings("unused")
  static mapping = {
    adjustmentsText type: 'text'
    cache true
  }

  /**
   * Internal constraints.
   */
  @SuppressWarnings("unused")
  static constraints = {
    domainName(maxSize: FieldSizes.MAX_CLASS_NAME_LENGTH, blank: false, unique: true)
    adjustmentsText(nullable: true)
  }

  /**
   * Internal field transients.
   */
  @SuppressWarnings("unused")
  static transients = ['adjustments', 'textHasBeenParsed']

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
    withNewSession {
      withTransaction {
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
            fieldGUIExtension.lastUpdated = new Date(new Date().time + 1)
            // Must change a simple field to force hibernate to update the record.
            fieldGUIExtension.persistAdjustmentsInText() // Make sure the new list is in JSON
            //println "save fieldGUIExtension = $fieldGUIExtension, JSON = ${fieldGUIExtension.adjustmentsText}"
            fieldGUIExtension.save(flush: false)
          } else {
            //println "deleting the whole FGE = $fieldGUIExtension"
            fieldGUIExtension.delete(flush: false)
            //FieldExtensionHelper.deleteCustomChild(fieldGUIExtension)
          }
        }
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
