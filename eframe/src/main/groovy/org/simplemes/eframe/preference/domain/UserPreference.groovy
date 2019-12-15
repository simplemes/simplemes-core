package org.simplemes.eframe.preference.domain


//import grails.gorm.annotation.Entity
import groovy.transform.ToString
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.preference.Preference

/**
 * This domain class allows the persistence of various user preferences.  This class can also be used to save
 * global settings for all users.
 * Inside of this domain class, the actual preferences are stored in various POGOs and
 * saved in the <code>preferencesText</code> column (a CLOB or large text object).
 * <p/>
 * The Map preferences uses a key with the element name for the {@link org.simplemes.eframe.preference.Preference} object.
 * A {@link org.simplemes.eframe.preference.Preference} object contains a Map that holds a number of
 * POGO elements with the actual preferences.  For a list HTML element, this is stored in the Map with the key _'ListColumns'_.
 * This Map entry is a List of {@link org.simplemes.eframe.preference.ColumnPreference} objects, one for each column with a preference.
 * In the case of columns, this includes the display sequence, width and sorting details.
 * <p/>
 * See <a href="http://www.simplemes.org/doc/latest/guide/single.html#gui-state-persistence">GUI State Persistence</a> for a more detailed
 * explanation.
 */
//@Entity
@ToString(includePackage = false, includeNames = true)
class UserPreference {
  /**
   * The user this preference is for.
   */
  String userName

  /**
   * The Page (URI) these preferences are for.
   */
  String page

  /**
   * A non-persisted List of preferences (<b>transient</b>).
   */
  List<Preference> preferences = []

  /**
   * The JSON form of the preferences.  This is the value persisted to the database.
   */
  String preferencesText

  /**
   * If true, then the text of the preferences has been parsed (after retrieval).  (<b>transient</b>).
   * This is used to make sure the JSON text is only parsed once, after retrieval.
   */
  boolean textHasBeenParsed = false

  /**
   * The date this record was last updated.
   */
  Date lastUpdated

  /**
   * The date this record was created
   */
  Date dateCreated

  /**
   * Internal field constraints.
   */
  static constraints = {
    userName(maxSize: FieldSizes.MAX_CODE_LENGTH, nullable: false, blank: false)
    page(maxSize: FieldSizes.MAX_PATH_LENGTH, nullable: false)
    preferencesText(nullable: true)
  }

  /**
   * Internal Mapping of fields to columns.
   */
  static mapping = {
    preferencesText type: 'text'
    cache true
  }


  /**
   * Internal field transients.
   */
  @SuppressWarnings("unused")
  static transients = ['preferences', 'textHasBeenParsed']

  /**
   * Persist the transient preferences Map into the JSON for storage in the DB.
   */
  protected void persistPreferencesInText() {
    if (preferences) {
      def mapper = Holders.objectMapper
      preferencesText = mapper.writeValueAsString(preferences)
    } else {
      preferencesText = null
    }
    textHasBeenParsed = true
  }

  /**
   * Called before a save() happens.  Calls persistPreferencesInXML().
   */
  @SuppressWarnings("unused")
  def beforeValidate() {
    persistPreferencesInText()
  }

  /**
   * Get the Preferences.  Converts from the persisted XML format if this is the first call to getPreferences() after a load.
   * This is done here instead of afterLoad() because of issues with caching and unit testing.
   * @return The preferences.
   */
  def getPreferences() {
    if (preferencesText && !textHasBeenParsed) {
      //println "parsing..."
      // Need to parse the JSON into the preferences.
      def mapper = Holders.objectMapper
      //println "JSON = ${groovy.json.JsonOutput.prettyPrint(preferencesText)}"
      // Work around the empty list problem in Jackson.  Will fail parsing if the 'settings' are not present.
      if (preferencesText?.contains('"settings"')) {
        preferences = mapper.readValue(preferencesText, Preference[]) as List
      }
      textHasBeenParsed = true
    }
    return preferences
  }
  /**
   * Sets the Preferences.
   */
  void setPreferences(List<Preference> pref) {
    preferences = pref
    preferencesText = null
    textHasBeenParsed = false
  }

  /**
   * Sets the Preferences.
   */
  @SuppressWarnings("unused")
  void setPreferencesText(String xml) {
    preferencesText = xml
    textHasBeenParsed = false
  }

  /**
   *  Build a human-readable version of this object.
   * @return The human-readable string.
   */
  @Override
  String toString() {
    StringBuilder sb = new StringBuilder("UserPreference{")
    sb.append("id=").append(id)
    sb.append(", userName='").append(userName).append('\'')
    sb.append(", page='").append(page).append('\'')
    sb.append(", preferences=").append(preferences)
    sb.append(", preferencesText=").append(LogUtils.limitedLengthString(preferencesText, 200))
    sb.append(", textHasBeenParsed=").append(textHasBeenParsed)
    sb.append('}')
    return sb.toString()
  }
}
