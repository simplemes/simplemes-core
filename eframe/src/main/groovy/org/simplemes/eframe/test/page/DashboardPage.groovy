/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

/**
 * Defines the GEB Page for the framework's standard dashboard page.
 * <p/>
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class DashboardPage extends AbstractPage {
  /**
   * Build the page.
   */
  DashboardPage() {
    // No need to wait for Ajax finish on load.
  }

  static url = "/dashboard"
  static at = { title.contains(lookup('dashboard.label')) }
  //static at = { true }

  /**
   * The page content available for this page.  See above.
   */
  static content = {
    panel { id -> $('div.webix_form', view_id: "Panel$id") }
    undoButton { $('#undoButton') }
    undoButtonEnabled { !$('#undoButton').classes().contains('undo-button-disabled') }

    // Editor Dialog elements.
    editorResizer { id -> $('div.webix_resizer', view_id: "EditorResizer$id") }
    editorPanel { panelName -> $('div.webix_form', view_id: "EditorPanel$panelName") }

    editorSaveButton { module(new ButtonModule(id: 'save')) }
    editorCancelButton { module(new ButtonModule(id: 'cancel')) }

    editorCloseSaveConfirmButton { module(new ButtonModule(id: 'dialog1-save')) }
    editorCloseNoSaveButton { module(new ButtonModule(id: 'dialog1-noSave')) }
    editorCloseCancelButton { module(new ButtonModule(id: 'dialog1-cancel')) }
  }

  /**
   * If true, then the page will wait on load until the Ajax queries are completed.
   * Override in your sub-class if you have Ajax loading mechanism.
   * This parent class sets it to false.
   * <p>
   * <b>Note:</b> This wait for Ajax completion requires the eframe_toolkit.js be loaded.
   */
  boolean getWaitForAjaxOnLoad() {
    return true
  }

}
