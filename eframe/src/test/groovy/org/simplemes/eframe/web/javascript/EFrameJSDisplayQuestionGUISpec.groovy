package org.simplemes.eframe.web.javascript

import org.simplemes.eframe.test.BaseJSSpecification
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests of the eframe.js displayQuestionDialog function.
 */
@IgnoreIf({ !sys['geb.env'] })
class EFrameJSDisplayQuestionGUISpec extends BaseJSSpecification {

  /**
   * A simple script to open a text field dialog with a default value and ok handler.
   * This script calls the ef.displayTextFieldDialog() function to verify that the public entry-point
   * works.
   */
  private static final String TEXT_FIELD_DIALOG_SCRIPT = """
      holder.logger = 'original';
      ef._addPreloadedMessages([ // Simulate localized labels for the dialog
        {"ok.label": "Ok"},
        {"cancel.label": "Cancel"},
        {"addLogger.title": "Add Other Logger"},
        {"logger.label": "Logger"},
        {"_decimal_": "."}
      ]);
      ef.displayTextFieldDialog({
        title: "addLogger.title", value: holder.logger, label: 'logger.label',
        textOk: function (value) { holder.logger = value; }
      });
    """

  def "verify that displayQuestionDialog works for simple case - ok and cancel"() {
    given: 'a script to load and display a looked up label'
    def src = """
      holder.result = '';
      eframe.displayQuestionDialog({title: 'Delete',
        question: 'Ok to Delete?',
        ok: function() {holder.result='ok'}  
      });
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the dialog contains the text passed in'
    $('span#QuestionText').text().contains('Ok to Delete')

    when: 'the Ok button is pressed'
    dialog0.okButton.click()

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the Ok logic was executed'
    js.holder.result == 'ok'
  }

  def "verify that displayQuestionDialog works for custom button case"() {
    given: 'a script to load and display a looked up label'
    def src = """
      holder.result = '';
      eframe.displayQuestionDialog({title: 'Delete',
        question: 'Ok to Delete?',
        buttons: ['yes','no'],
        yes: function() {holder.result='yes'},  
        no: function() {holder.result='no'}  
      });
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the buttons are correct'
    $('div', view_id: "dialog0").find('div', view_id: "dialog0-yes").find('button').text() == 'yes.label'
    $('div', view_id: "dialog0").find('div', view_id: "dialog0-no").find('button').text() == 'no.label'

    when: 'the yes button is pressed'
    $('div', view_id: "dialog0").find('div', view_id: "dialog0-yes").find('button').click()

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the correct function was executed'
    js.holder.result == 'yes'

    when: 'the JS is executed again'
    execute(src)

    and: 'the no button is clicked'
    $('div', view_id: "dialog0").find('div', view_id: "dialog0-no").find('button').click()

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the correct function was executed'
    js.holder.result == 'no'
  }

  // test delete showGUI tests

}
