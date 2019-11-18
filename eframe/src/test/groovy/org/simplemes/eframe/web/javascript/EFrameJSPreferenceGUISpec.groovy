package org.simplemes.eframe.web.javascript

import groovy.json.JsonSlurper
import org.simplemes.eframe.preference.DialogPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.test.BaseJSSpecification
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of the eframe.js methods related to user preferences (e.g. loadDialogPreferences).
 */
@IgnoreIf({ !sys['geb.env'] })
class EFrameJSPreferenceGUISpec extends BaseJSSpecification {

  def "verify that loadDialogPreferences finds the preferences for two dialogs"() {
    given: 'user preferences for the first dialog'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page '/javascriptTester'
        user 'admin'
        element 'aDialog'
      }
      preference.settings << new DialogPreference(width: 20.2, height: 21.2, left: 22.2, top: 23.2)
      preference.save()
    }

    and: 'the second dialog preferences are created in another txn'
    UserPreference.withTransaction {
      PreferenceHolder preference = PreferenceHolder.find {
        page '/javascriptTester'
        user 'admin'
        element 'anotherDialog'
      }
      preference.settings << new DialogPreference(width: 10.2, height: 11.2, left: 12.2, top: 13.2)
      preference.save()
    }

    when: 'the JS is executed for the admin user'
    login()
    execute('ef.loadDialogPreferences();')

    then: 'the loaded dialog preferences are found and stored in the client-side cache variable'
    waitFor {
      js.exec('return JSON.stringify(tk._getDialogPreferences());').contains('anotherDialog')
    }

    and: 'the preferences are valid'
    def text = js.exec('return JSON.stringify(tk._getDialogPreferences());')
    def json = new JsonSlurper().parseText((String) text)

    and: 'both dialogs are listed'
    json.aDialog.width == 20.2
    json.aDialog.height == 21.2
    json.aDialog.left == 22.2
    json.aDialog.top == 23.2

    json.anotherDialog.width == 10.2
    json.anotherDialog.height == 11.2
    json.anotherDialog.left == 12.2
    json.anotherDialog.top == 13.2

  }

  def "verify that lookup supports replaceable parameters"() {
    given: 'a script to load and display a looked up label'
    def src = """
      ef._addPreloadedMessages([{"title.label": "The title for {0}"}]);
      displayResult(ef.lookup("title.label","M1010"));
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the localized text is displayed'
    result.text() == 'The title for M1010'
  }

  def "verify that lookup with no preloaded message works gracefully"() {
    given: 'a script to load and display a looked up label'
    def src = """
      ef._addPreloadedMessages([{"title.label": "The title for {0}"}]);
      displayResult(ef.lookup("ok.label"));
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the localized text is displayed'
    result.text() == 'ok.label'
  }

}
