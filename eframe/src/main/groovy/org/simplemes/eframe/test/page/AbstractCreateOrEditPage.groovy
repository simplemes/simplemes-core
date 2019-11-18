package org.simplemes.eframe.test.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The general base class for create or edit definition pages tested in the framework.
 * This defines the elements common to all create/edit pages (Toolbar buttons) and tab panels.
 * <p>
 * <b>Note:</b> These assume the default ID is used for the efCreate/efEdit markers.
 */
class AbstractCreateOrEditPage extends AbstractPage {
  /**
   * The first argument used in the 'to' call, if it is a domain object.
   * This is only supported with the edit pages (in the {@link #checkEditTitle} method).
   * <p>
   *  Example 'to' page usage with a domain object.
   * <pre>
   *   to OrderEditPage, order
   * </pre>
   *
   */
  def domainObject

  /**
   * Checks the create page's title using the given lookup key for the domain class.
   * <p>
   * An example title check can be:
   * <pre>
   *   static at = &#123;
   *                 checkCreateTitle('order.label')
   *               &#125;
   * </pre>
   *
   * @param domainLookupKey The key to lookup the label for domain object (e.g. 'order.label').
   * @return
   */
  boolean checkCreateTitle(String domainLookupKey) {
    // Check with an assertion for a clearer error message.
    assert title == GlobalUtils.lookup('create.title', null, lookup(domainLookupKey), Holders.configuration.appName)
    return title == GlobalUtils.lookup('create.title', null, lookup(domainLookupKey), Holders.configuration.appName)
  }

  /**
   * Checks the edit page's title using the given lookup key for the domain class.
   * Uses the <code>domainObject</code>, if set.
   * <p>
   * An example title check can be:
   * <pre>
   *   static at = &#123;
   *                 checkEditTitle('order.label')
   *               &#125;
   * </pre>
   *
   * @param domainLookupKey The key to lookup the label for domain object (e.g. 'order.label').
   * @return True if the title matches.
   */
  boolean checkEditTitle(String domainLookupKey) {
    if (domainObject) {
      // Check with an assertion for a clearer error message.
      assert title == GlobalUtils.lookup('edit.title', null,
                                         TypeUtils.toShortString(domainObject),
                                         lookup(domainLookupKey),
                                         Holders.configuration.appName)

      return title == GlobalUtils.lookup('edit.title', null,
                                         TypeUtils.toShortString(domainObject),
                                         lookup(domainLookupKey),
                                         Holders.configuration.appName)
    } else {
      // Check with an assertion for a clearer error message.
      assert title.contains(lookup(domainLookupKey))
      return title.contains(lookup(domainLookupKey))
    }
  }

  /**
   * Generates the path arguments from the arguments.  This sub-class takes a domain object
   * and stores it in the 'domainObject' field and the uses the object's id for the URL creation.
   * @param args The 'to' arguments.
   * @return The path.
   */
  @Override
  String convertToPath(Object[] args) {
    // Convert domain entry argument to an ID
    for (int i = 0; i < args.size(); i++) {
      if (DomainUtils.instance.isGormEntity(args[i].getClass())) {
        domainObject = args[i]
        args[i] = args[i].id
      }
    }
    return super.convertToPath(args)
  }


  /**
   * The page content available for this page.  See above.
   */
  static content = {
    listButtonOnCreate { $('div.webix_el_button', view_id: "createList").find('a') }
    listButtonOnEdit { $('div.webix_el_button', view_id: "editList").find('a') }
    createButton { $('div.webix_el_button', view_id: "createSave").find('button') }
    createBottomButton { $('div.webix_el_button', view_id: "createSaveBottom").find('button') }
    updateButton { $('div.webix_el_button', view_id: "editSave").find('button') }
    updateBottomButton { $('div.webix_el_button', view_id: "editSaveBottom").find('button') }

    // The tabbed panels for most pages
    mainPanel { $('div.webix_item_tab', button_id: "mainBody") }
    detailsPanel { $('div.webix_item_tab', button_id: "detailsBody") }

    // Edit Field Dialog contents
    fieldName { module(new TextFieldModule(field: 'fieldName')) }
    fieldLabel { module(new TextFieldModule(field: 'fieldLabel')) }
    fieldFormat { module(new ComboboxModule(field: 'fieldFormat')) }
    maxLength { module(new TextFieldModule(field: 'maxLength')) }
    valueClassName { module(new TextFieldModule(field: 'valueClassName')) }
    saveFieldButton { $('div.webix_el_button', view_id: "saveField").find('button') }
    cancelFieldButton { $('div.webix_el_button', view_id: "cancelField").find('button') }
  }

}
