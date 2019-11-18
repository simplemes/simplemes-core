package org.simplemes.eframe.web.javascript


import org.simplemes.eframe.test.BaseJSSpecification
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of the eframe.js methods related to localization (lookup(), etc.).
 */
@IgnoreIf({ !sys['geb.env'] })
class EFrameJSLocalizationGUISpec extends BaseJSSpecification {

  def "verify that _addPreloadedMessages and lookup work together"() {
    given: 'a script to load and display a looked up label'
    def src = """
      ef._addPreloadedMessages([{"ok.label": "Ok","cancel.label": "Cancel"}]);
      displayResult(ef.lookup("ok.label"));
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the localized text is displayed'
    result.text() == 'Ok'
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
