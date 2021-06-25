package org.simplemes.eframe.test.page

import geb.Module

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB page elements for a toolkit-based grid used for Crud page lists.
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
 *   <li><b>cell</b> - A single cell, arguments: row, col. </li>
 *   <li><b>sortAsc</b> - The column header marked as the ascending sort order. </li>
 *   <li><b>sortDesc</b> - The column header marked as the descending sort order. </li>
 *   <li><b>pagerButtons</b> - The buttons in the pagers (List). </li>
 * </ul>
 *
 */
@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyUnusedDeclaration"])
class CrudListModule extends Module {
  /**
   * The data-testid for the main CRUD list.
   */
  def testID = "CrudTable"

  static content = {
    headers { $("div.p-datatable", 'data-testid': testID).find('thead').find('span.p-column-title') }
    addRecordButton { $("button#addRecord") }

    rows { $("div.p-datatable", 'data-testid': testID).find('tbody').find('tr') }
    cell { row, col -> $("div.p-datatable", 'data-testid': testID).find('tr', row + 1).find('td', col) }
    pagerButtons { $("div.p-datatable", 'data-testid': testID).find('button.p-paginator-page') }
    editRowButton { row -> $("button#EditRow", row) }
    rowMenuButton { row -> $("button#RowMenuButton", row) }
    deleteRowButton(required: false) { $("div.p-menu").find('span.pi-times') }

    searchField { $("div.p-datatable", 'data-testid': testID).find('input') }
  }

}
