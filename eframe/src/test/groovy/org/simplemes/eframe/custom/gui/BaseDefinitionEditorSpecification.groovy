package org.simplemes.eframe.custom.gui

import geb.Page
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.test.BaseGUISpecification

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Base class for common Definition Editor functions.  Not exposed to other modules.
 * Provides common features that are used by the group of definition editor tests.
 */
class BaseDefinitionEditorSpecification extends BaseGUISpecification {
  /**
   * Finds the list element in the add/remove panel for a given element name.
   * This typically only works with the eframe_definition.js list elements used to configure the definition GUI.
   *
   * @param elementName The name of the element in the list.
   * @return A GEB element.
   */
  def findListElement(String elementName) {
    return $("#${elementName}ListItem")
  }

  /**
   * Finds the parent list in the add/remove panel for a given element name.
   * This typically only works with the eframe_definition.js list elements used to configure the definition GUI.
   *
   * @param elementName The name of the element in the list.
   * @return A GEB element for the list itself.
   */
  def findParentListForElement(String elementName) {
    return $("#${elementName}ListItem")?.parent()?.parent()?.parent()
  }

  /**
   * Clicks on the given list element.
   *
   * @param elementName The name of the element in the list.
   */
  void selectListElement(String elementName) {
    $("#${elementName}ListItem").click()
  }

  /**
   * Drags the list element from the source to the given destination.
   *
   * @param sourceName The element name to drag to the destination.
   * @param destinationName The element name to drag to source to.
   * @param checkPosition If true, then checks that the X/Y position is correct after the move.
   */
  void dragListElement(String sourceName, String destinationName, boolean checkPosition = true) {
    def source = findListElement(sourceName)
    def destination = findListElement(destinationName)
    def (offsetX, offsetY) = calculateOffset(source, destination)
    //def item = $('#listitem2availableFields')
    interact {
      // Drag the width element to the display list.
      dragAndDropBy(source, offsetX, offsetY)
    }
    standardGUISleep(2)
    // Make sure the drop worked.
    if (checkPosition) {
      source = findListElement(sourceName)
      destination = findListElement(destinationName)
      assert source.x == destination.x
      // We can't verify the exact vertical direction since the drop from one list to another
      // drops the item above the destination.
      // Drops in within a list (re-order) drops it below the destination.
      // All we can do is make sure they are close together.  This is a guess on the
      // height of the item.
      assert Math.abs(source.y - destination.y) < 50
    }
  }
  /**
   * Convenience method to build a custom panel definition.
   * @param options Contains: domainClass, panel, afterFieldName or a list of these elements.
   * @return The FieldGUIExtension created.
   */
  FieldGUIExtension buildCustomPanel(Map options) {
    def fg = null
    FieldGUIExtension.withTransaction {
      fg = new FieldGUIExtension(domainName: options.domainClass?.name)
      def adj = []
      if (options.panel) {
        adj << new FieldInsertAdjustment(fieldName: options.panel, afterFieldName: options.afterFieldName)
      }
      if (options.list) {
        for (a in options.list) {
          adj << new FieldInsertAdjustment(fieldName: a.panel, afterFieldName: a.afterFieldName)
        }
      }
      fg.adjustments = adj
      fg.save()
    }
    return fg
  }

  /**
   * Convenience method that navigates to the given page and opens the editor.
   * @param page The page to open the config editor for.
   */
  void openEditor(Class<Page> page) {
    login()
    to page

    configButton.click()
    waitFor { dialog0.exists }
  }


}
