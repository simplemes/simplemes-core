/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.javascript

import geb.navigator.Navigator
import org.openqa.selenium.Keys
import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.test.BaseJSSpecification
import org.simplemes.eframe.test.UnitTestUtils
import spock.lang.IgnoreIf

/**
 * Tests of the eframe_toolkit.js methods related to dialogs.
 */
@IgnoreIf({ !sys['geb.env'] })
class ToolkitJSDialogGUISpec extends BaseJSSpecification {

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

  /**
   * Sets the dialog's input text field with the given value.
   * @param value The value.
   */
  void setTextFieldValue(String value) {
    $('div', view_id: 'dialogTextField').find('input').value(value)
  }


  /**
   * Verifies that the given element is centered in the browser window and centered.
   * Uses and approximate comparison since browsers are not super precise.
   * @param element The element.
   * @param widthPercent The width (percent) expected.
   * @param heightPercent The height (percent) expected.
   * @return true
   */
  boolean assertCenteredAndSized(Navigator element, String widthPercent, String heightPercent) {
    int w = element.width
    int h = element.height
    int x = element.x
    int y = element.y

    int screenWidth = (int) js.exec('return window.innerWidth')
    int screenHeight = (int) js.exec('return window.innerHeight')

    // find center
    int xCenter = x + (int) (w / 2)
    int yCenter = y + (int) (h / 2)

    // Check the size
    UnitTestUtils.assertClose(w, percentInPixels(screenWidth, widthPercent), 'width')
    UnitTestUtils.assertClose(h, percentInPixels(screenHeight, heightPercent), 'height')

    // Check the position
    UnitTestUtils.assertClose(xCenter, (int) (screenWidth / 2), 'xCenter')
    UnitTestUtils.assertClose(yCenter, (int) (screenHeight / 2), 'yCenter')

    return true
  }

  /**
   * Verifies that the given element is sized and positioned as expected.
   * Uses and approximate comparison since browsers are not super precise.
   * @param element The element.
   * @param widthPercent The width (percent) expected.
   * @param heightPercent The height (percent) expected.
   * @param leftPercent The left position (percent) expected.
   * @param topPercent The top position (percent) expected.
   * @return true
   */
  boolean assertSizeAndPosition(Navigator element, String widthPercent, String heightPercent,
                                String leftPercent, String topPercent) {
    int w = element.width
    int h = element.height
    int x = element.x
    int y = element.y

    int screenWidth = (int) js.exec('return window.innerWidth')
    int screenHeight = (int) js.exec('return window.innerHeight')

    // Check the size
    UnitTestUtils.assertClose(w, percentInPixels(screenWidth, widthPercent), 'width')
    UnitTestUtils.assertClose(h, percentInPixels(screenHeight, heightPercent), 'height')

    // Check the position
    UnitTestUtils.assertClose(x, percentInPixels(screenWidth, leftPercent), 'x')
    UnitTestUtils.assertClose(y, percentInPixels(screenHeight, topPercent), 'y')

    return true
  }

  /**
   * Calculate the value in pixels, based on the total and the given percent specifier.
   * @param total The total to get the percent of.
   * @param percent The percent (e.g. "30%").
   */
  int percentInPixels(int total, String percent) {
    def p = new BigDecimal(percent - '%')
    return (int) total * (p * 0.01)
  }

  /**
   * Convenience method to build a simple dialog with a specific number of buttons.
   * @param buttons The buttons to use.  Null and empty list allowed.
   * @param handleButton The button to generate a handler for.  Will stick the handler arguments into the holder for testing.
   * @param extraScript Addition script code to add as a parameter to the displayDialog() function call.
   * @return The script.
   */
  String buildDialogWithButtons(List<String> buttons, String handleButton, String extraScript = '') {
    def buttonScript = ''
    if (buttons != null) {
      StringBuilder buttonsWithQuotes = new StringBuilder()
      for (button in buttons) {
        if (buttonsWithQuotes) {
          buttonsWithQuotes << ","
        }
        buttonsWithQuotes << "'$button'"
      }
      buttonScript = ",buttons: [$buttonsWithQuotes]"
    }

    def handler = ''
    if (handleButton) {
      handler = """
        ,$handleButton: function (dialogID, button) { 
          holder.button = button; 
          holder.dialogID = dialogID; 
        }
      """
    }
    def src = """
      holder.dialogID = '';
      holder.button = '';
      ef.displayDialog({
        title: "Title", body: 'the body text' 
        $buttonScript
        $handler
        $extraScript
      });
    """
    //println "src = $src"
    return src
  }

  def "verify that _displayTextFieldDialog displays the dialog and handles ok button"() {
    when: 'the JS is executed'
    execute(TEXT_FIELD_DIALOG_SCRIPT)

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'the input field is changed'
    setTextFieldValue('new value')

    and: 'the Ok button is pressed'
    dialog0.okButton.click()

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the input field value has been returned'
    js.holder.logger == 'new value'
  }

  def "verify that _displayTextFieldDialog displays the dialog with localized text"() {
    when: 'the JS is executed'
    execute(TEXT_FIELD_DIALOG_SCRIPT)

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    and: 'the title is correct'
    dialog0.title == 'Add Other Logger'

    and: 'the Ok button text is correct'
    dialog0.okButton.text() == lookup('ok.label')

    and: 'the cancel button text is correct'
    dialog0.cancelButton.text() == 'Cancel'

    and: 'the field label is correct'
    $('div', view_id: 'dialogTextField').find('label').text() == 'Logger'
  }

  def "verify that _displayTextFieldDialog places default focus in the input field"() {
    when: 'the JS is executed'
    execute(TEXT_FIELD_DIALOG_SCRIPT)

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'the input field is changed'
    sendKey('XYZZY')

    and: 'the Ok button is pressed'
    dialog0.okButton.click()

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the input field value has been returned'
    js.holder.logger.contains('XYZZY')
  }

  def "verify that _displayTextFieldDialog supports the ENTER hot-key"() {
    when: 'the JS is executed'
    execute(TEXT_FIELD_DIALOG_SCRIPT)

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'the input field is changed'
    setTextFieldValue('new value')

    and: 'the Enter key is sent'
    sendKey(Keys.ENTER)

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the input field value has been returned'
    js.holder.logger == 'new value'
  }

  def "verify that _displayTextFieldDialog supports the escape hot-key"() {
    when: 'the JS is executed'
    execute(TEXT_FIELD_DIALOG_SCRIPT)

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'the ESC key is sent'
    sendKey(Keys.ESCAPE)

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }
  }

  def "verify that _displayTextFieldDialog handles the cancel button"() {
    when: 'the JS is executed'
    execute(TEXT_FIELD_DIALOG_SCRIPT)

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'the input field is changed'
    setTextFieldValue('new value')

    and: 'the Cancel button is clicked'
    dialog0.cancelButton.click()

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the result value is unchanged'
    js.holder.logger == 'original'
  }

  def "verify that _displayTextFieldDialog handles the window close icon"() {
    when: 'the JS is executed'
    execute(TEXT_FIELD_DIALOG_SCRIPT)

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'the input field is changed'
    setTextFieldValue('new value')

    and: 'the window close button is clicked'
    dialog0.closeButton.click()

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the result value is unchanged'
    js.holder.logger == 'original'
  }

  def "verify that _displayTextFieldDialog opens a centered dialog with right size"() {
    when: 'the JS is executed'
    execute(TEXT_FIELD_DIALOG_SCRIPT)

    and: 'the dialog is displayed'
    waitFor { dialog0.exists }

    then: 'the dialog is the right size'
    assertCenteredAndSized((Navigator) dialog0.view, "50%", "35%")

    cleanup:
    dialog0.closeButton.click()
  }

  def "verify that _displayDialog can be moved and the size is remembered upon page refresh"() {
    when: 'the JS is executed for a given user'
    login()
    execute(TEXT_FIELD_DIALOG_SCRIPT)

    and: 'the dialog is displayed'
    waitFor { dialog0.exists }
    int left1 = dialog0.view.x
    int top1 = dialog0.view.y

    and: 'the dialog is moved'
    interact {
      dragAndDropBy(dialog0.header, -50, 50)
    }

    then: 'the dialog is moved'
    int left2 = dialog0.view.x
    int top2 = dialog0.view.y
    UnitTestUtils.assertClose(left2, left1 - 50, 'Dialog left position after move', 5)
    UnitTestUtils.assertClose(top2, top1 + 50, 'Dialog top position after move', 5)

    when: 'the user preferences written to the DB'
    waitFor() { nonZeroRecordCount(UserPreference) }

    and: 'the page is reloaded and the dialog preferences are loaded'
    execute('eframe.loadDialogPreferences();')
    waitForCompletion()

    and: 'the dialog is re-displayed'
    js.exec(TEXT_FIELD_DIALOG_SCRIPT)

    then: 'the dialog is in the right place'
    int left3 = dialog0.view.x
    int top3 = dialog0.view.y
    UnitTestUtils.assertClose(left3, left2, 'Dialog left position after refresh', 9)
    UnitTestUtils.assertClose(top3, top2, 'Dialog top position after refresh', 9)

    cleanup:
    dialog0.closeButton.click()
  }

  def "verify that _displayDialog can be resized and the size is remembered upon page refresh"() {
    when: 'the JS is executed for a given user'
    login()
    execute(TEXT_FIELD_DIALOG_SCRIPT)

    and: 'the dialog is displayed'
    waitFor { dialog0.exists }
    int width1 = dialog0.view.width
    int height1 = dialog0.view.height

    and: 'the dialog is resized'
    interact {
      dragAndDropBy(dialog0.resizeHandle, 100, 50)
    }

    then: 'the dialog was resized'
    int width2 = dialog0.view.width
    int height2 = dialog0.view.height
    UnitTestUtils.assertClose(width2, width1 + 100, 'Dialog width after move', 5)
    UnitTestUtils.assertClose(height2, height1 + 50, 'Dialog height after move', 5)

    when: 'the user preferences written to the DB'
    waitFor() {
      def count = 0
      UserPreference.withTransaction {
        count = UserPreference.count()
      }
      count > 0
    }

    and: 'the page is reloaded and the dialog preferences are loaded'
    execute('eframe.loadDialogPreferences();')
    waitForCompletion()

    and: 'the dialog is re-displayed'
    js.exec(TEXT_FIELD_DIALOG_SCRIPT)

    then: 'the dialog is in the right place'
    int width3 = dialog0.view.width
    int height3 = dialog0.view.height
    UnitTestUtils.assertClose(width3, width2, 'Dialog width after refresh', 9)
    UnitTestUtils.assertClose(height3, height2, 'Dialog height after refresh', 9)

    cleanup:
    dialog0.closeButton.click()
  }

  def "verify that displayDialog displays supports custom buttons"() {
    when: 'the the dialog is displayed'
    execute(buildDialogWithButtons(['yes', 'no', 'cancel'], 'yes'))

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'the yes button is clicked'
    $('div', view_id: "dialog0-yes").click()

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the arguments passed to the button handler were correct'
    js.holder.button == 'yes'
    js.holder.dialogID == 'dialog0'
  }

  def "verify that displayDialog works with no buttons"() {
    given: 'the beforeClose function needed to verify that the right values were passed to the function'
    def extraScript = """
        ,beforeClose: function (dialogID,action) { 
          holder.dialogID = dialogID; 
          holder.action = action; 
        }
    """

    when: 'the the dialog is displayed'
    execute(buildDialogWithButtons([], null, extraScript))

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'the dialog is closed with the escape key'
    sendKey(Keys.ESCAPE)

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the beforeClose function was called'
    js.holder.dialogID == 'dialog0'
    js.holder.action == 'cancel'
  }

  def "verify that displayDialog displays defaults to a single Ok button"() {
    when: 'the the dialog is displayed'
    execute(buildDialogWithButtons(null, 'ok'))

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'the ok button is pressed'
    $('div', view_id: "dialog0-ok").click()

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'the arguments passed to the button handler were correct'
    js.holder.button == 'ok'
    js.holder.dialogID == 'dialog0'
  }

  def "verify that displayDialog displays defaults to a single Ok button that supports escape as hot-key"() {
    when: 'the the dialog is displayed'
    execute(buildDialogWithButtons(null, null))

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'escape is pressed'
    sendKey(Keys.ESCAPE)

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }
  }

  def "verify that displayDialog displays defaults with a single button has the default focus"() {
    when: 'the the dialog is displayed'
    execute(buildDialogWithButtons(null, 'ok'))

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'escape is pressed'
    sendKey(Keys.SPACE)

    then: 'the dialog is closed'
    waitFor { !dialog0.exists }

    and: 'ok button was triggered properly'
    js.holder.button == 'ok'
    js.holder.dialogID == 'dialog0'
  }

  def "verify that displayDialog displays beforeClosing function can prevent closing"() {
    given: 'the beforeClose function that prevent closing'
    def extraScript = """
        ,beforeClose: function (dialogID,action) { 
          return false; 
        }
    """
    when: 'the the dialog is displayed'
    execute(buildDialogWithButtons(null, null, extraScript))

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    when: 'escape is pressed'
    sendKey(Keys.ESCAPE)
    standardGUISleep()

    then: 'the dialog is not closed'
    dialog0.exists
  }

  def "verify that displayDialog can take custom size and position inputs"() {
    given: 'an displayDialog script'
    def src = """
      ef.displayDialog({
        title: "addLogger.title",body: 'abc',
        width: "90%",
        height: "50%",
        left: "5%",
        top: "9%"
      });
    """
    when: 'the the dialog is displayed'
    execute(src)

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    and: 'the dialog is the right size/position'
    assertSizeAndPosition((Navigator) dialog0.view, "90%", "50%", "5%", "9%")
  }

  def "verify that displayDialog gracefully detects missing required parameter body or bodyURL"() {
    given: 'an displayDialog script'
    def src = """
      ef.displayDialog({
        title: "addLogger.title"
      });
    """
    when: 'the the dialog is displayed'
    execute(src)

    then: 'the dialog is displayed'
    waitFor { messages.text() }

    and: 'the message is correct'
    //        ef._criticalError("eframe._displayDialog(): Missing required field body/bodyURL.  Options = "+options);
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), ['Missing', 'body', 'title', 'addLogger.title'])
  }

  def "verify that displayDialog supports dynamic page content from server"() {
    given: 'a page available from the server'
    def pageSrc = """
      \${params._variable}.display = {  
        rows: [  
          {id: 'sample', template: "Make changes as needed."}
        ]
      }
    """
    DashboardTestController.setMemoryPages('sample', pageSrc)

    when: 'the JS is executed'
    def jsSrc = "ef.displayDialog({bodyURL: '/test/dashboard/memory?page=sample', title: 'Sample Title' }); "
    login()
    execute(jsSrc)

    then: 'the dialog is displayed'
    waitFor { dialog0.exists }

    and: 'the body has the expected content'
    $('div.webix_view', view_id: 'sample').text() == 'Make changes as needed.'

    cleanup:
    sendKey(Keys.ESCAPE)
    waitFor { !dialog0.exists }
  }

}
