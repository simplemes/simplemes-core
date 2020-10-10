/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.javascript


import org.simplemes.eframe.test.BaseJSSpecification
import org.simplemes.eframe.test.UnitTestUtils
import spock.lang.IgnoreIf

/**
 * Tests of the eframe.js displayMessage function.
 */
@IgnoreIf({ !sys['geb.env'] })
class EFrameJSDisplayMessageGUISpec extends BaseJSSpecification {

  /**
   * Convenience method to check the class for a displayed message.
   * @return The class.
   */
  String getMessageClass() {
    List list = $('div#messages').find('div').classes()
    list = list - 'message'
    return list[0]
  }

  def "verify that displayMessage works with simple string input"() {
    given: 'a script to load and display a looked up label'
    def src = """
      ef.clearMessages();
      ef.displayMessage('the info message');
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the info message was displayed correctly'
    messages.text().contains('the info message')

    and: 'the message is displayed with the right class'
    messageClass == 'info-message'
    messages.info
  }

  def "verify that displayMessage works with single map input - all types"() {
    given: 'a script to load and display a looked up label'
    def src = """
      ef.clearMessages();
      ef.displayMessage({$name: 'the $name message'});
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the info message was displayed correctly'
    messages.text().contains("the $name message")

    and: 'the message is displayed with the right class'
    messageClass == className

    where:
    name      | className
    'info'    | 'info-message'
    'error'   | 'error-message'
    'warn'    | 'warning-message'
    'warning' | 'warning-message'
  }

  def "verify that displayMessage works with warning messages"() {
    given: 'a script to load and display a looked up label'
    def src = """
      ef.clearMessages();
      ef.displayMessage({warn: 'the warning message'});
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the info message was displayed correctly'
    messages.text.contains("the warning message")

    and: 'the message is displayed with the correct severity'
    messages.warn
    messages.warning

  }

  def "verify that displayMessage works array of strings"() {
    given: 'a script to load and display a looked up label'
    def src = """
      ef.clearMessages();
      ef.displayMessage(['ABC','DEF','GHI']);
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the info message was displayed correctly'
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['ABC', 'DEF', 'GHI'])
  }

  def "verify that displayMessage works array of objects - all types"() {
    given: 'a script to load and display a looked up label'
    def src = """
      ef.clearMessages();
      ef.displayMessage('the <script></script> <b>message</b>');
    """

    when: 'the JS is executed'
    execute(src)

    then: 'the info message was displayed correctly'
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['<script>', '</script>', '<b>', '</b>'])
  }

  /**
   * A simple script to open a dialog with a message area and ok handler.
   */
  private static final String DIALOG_SCRIPT = """
      holder.logger = 'original';
      ef._addPreloadedMessages([ // Simulate localized labels for the dialog
        {"ok.label": "Ok"},
        {"cancel.label": "Cancel"}
      ]);
      ef.displayTextFieldDialog({
        title: "addLogger.title", value: holder.logger, label: 'logger.label',
        textOk: function (value) { holder.logger = value; },
        left: 100,
        messageArea: true
      });
      ef.displayTextFieldDialog({
        title: "nested title", value: holder.logger, label: 'nested label',
        textOk: function (value) { holder.logger = value; },
        left: 500,
        messageArea: true
      });
      ef.displayMessage({error: 'the error'});
    """

  def "verify that displayMessage works with a toolkit view in a nested dialog"() {
    when: 'the JS is executed'
    execute(DIALOG_SCRIPT)

    then: 'the dialog is displayed'
    waitFor { dialog1.exists }

    and: 'the error is displayed in the right dialog'
    $('div', view_id: 'dialog1').$('div', view_id: 'dialogMessages').find('.message').text() == 'the error'
  }
}
