/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * The general base class for show definition pages tested in the framework.
 * This defines the elements common to all Show pages (Toolbar buttons) and tab panels.
 * <p>
 * <b>Note:</b> These assume the default ID is used for the efShow marker.
 */
class AbstractShowPage extends AbstractPage {

  /**
   * The first argument used in the 'to' call, if it is a domain object.  Can be used in the at checking.
   * This is used in the method {@link #checkTitle}, but it can't be used in the 'at' checker.
   * <p>
   *  Example 'to' page usage with a domain object.
   * <pre>
   *   to OrderShowPage, order
   * </pre>
   *
   */
  def domainObject

  /**
   * Checks the show page's title using the given lookup key for the domain class.
   * <p>
   * An example title check can be:
   * <pre>
   *   static at = &#123;
   *                 checkTitle('order.label')
   *               &#125;
   * </pre>
   *
   * @param domainLookupKey The key to lookup the label for domain object (e.g. 'order.label').
   * @return
   */
  boolean checkTitle(String domainLookupKey) {
    if (domainObject) {
      return title == GlobalUtils.lookup('show.title', null,
                                         TypeUtils.toShortString(domainObject),
                                         lookup(domainLookupKey),
                                         Holders.configuration.appName)
    } else {
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
      if (DomainUtils.instance.isDomainEntity(args[i].getClass())) {
        domainObject = args[i]
        args[i] = args[i].uuid
      }
    }
    return super.convertToPath(args)
  }

  /**
   * The page content available for this page.  See above.
   */
  static content = {
    listButton { $('div', view_id: "showList").find('a') }
    createButton { $('div', view_id: "showCreate").find('a') }
    editButton { $('div', view_id: "showEdit").find('a') }

    moreMenuButton { $('a.webix_list_item', webix_l_id: "showMoreMenu") }
    deleteButton { $('a.webix_list_item', webix_l_id: "showDelete") }

    // The tabbed panels for most pages
    mainPanel { $('div.webix_item_tab', button_id: "mainBody") }
    detailsPanel { $('div.webix_item_tab', button_id: "detailsBody") }
  }

}
