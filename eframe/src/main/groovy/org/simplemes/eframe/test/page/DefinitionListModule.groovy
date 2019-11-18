package org.simplemes.eframe.test.page

import geb.Module

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB page elements for a toolkit-based grid used for definition lists.
 * <p/>
 * <b>Usage:</b>
 * <pre>
 * // The page definition.
 * static content = &#123;
 *   orders &#123; module(new DefinitionListModule(field: 'orders')) &#125;
 * &#125;
 * ...
 * assert orderGrid.rows.size() == allOrders.size()
 * for (int i = 0; i < allOrders.size(); i++) &#123;
 *   assert orders.cell(1,0).text() == allOrders[i].description
 *   assert orders.cell(0, getColumnIndex(Order,'order')).text() == 'M10045'
 *   assert orders.headers[7].text() == 'Due Date'
 *   assert orders.sortAsc.text() == lookup('order.label')  // or sortDesc
 * &#125;
 * </pre>
 *
 * <b>Note:</b> The extraction of many cells can be slow.
 *
 * <h3>content</h3>
 * The content elements available include:
 * <ul>
 *   <li><b>headers</b> - The column headers (List). </li>
 *   <li><b>sortAsc</b> - The column header marked as the ascending sort order. </li>
 *   <li><b>sortDesc</b> - The column header marked as the descending sort order. </li>
 *   <li><b>rows</b> - The rows for a single column (List). </li>
 *   <li><b>cell</b> - A single cell, arguments: row, col. </li>
 *   <li><b>pagerButtons</b> - The buttons in the pagers (List). </li>
 * </ul>
 *
 */
@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyUnusedDeclaration"])
class DefinitionListModule extends Module {
  /**
   * The field core name for the definition list.  By default, will use the "${field}${suffix}" to find the
   * div the list is defined in (<b>Required</b>).
   */
  def field

  /**
   * The field suffix for the definition list.  By default, this will use the "${field}${suffix}" to find the
   * div the list is defined in.
   * (<b>Default:</b> 'DefinitionList').
   */
  def suffix = 'DefinitionList'

  static content = {
    headers { $("div#${field}${suffix}").find('div.webix_hcell') }
    sortAsc(required: false) { $("div#${field}${suffix}").find('div.webix_hcell').has('div.webix_ss_sort_asc') }
    sortDesc(required: false) { $("div#${field}${suffix}").find('div.webix_hcell').has('div.webix_ss_sort_desc') }
    rows { col -> $("div#${field}${suffix}").find('div.webix_column', column: "$col").find('div.webix_cell') }
    cell { row, col -> $("div#${field}${suffix}").find('div.webix_column', column: "$col").find('div.webix_cell', row) }

    pagerButtons { $("div#${field}${suffix}").find('div.webix_pager').find('button') }
  }

}
