/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

import geb.Module

/**
 * Defines the GEB page elements for a toolkit-based grid used for inline grids.
 * <p/>
 * <b>Usage:</b>
 * <pre>
 * // The page definition.
 * static content = &#123;
 *   orders &#123; module(new GridModule(field: 'orders')) &#125;
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
class GridModule extends Module {
  def field
  static content = {
    label { $('div.webix_el_label', view_id: "${field}Label").text() }
    headers { $("div.webix_dtable", view_id: field).find('div.webix_hcell') }
    sortAsc(required: false) {
      $("div.webix_dtable", view_id: field).find('div.webix_hcell').has('div.webix_ss_sort_asc')
    }
    sortDesc(required: false) {
      $("div.webix_dtable", view_id: field).find('div.webix_hcell').has('div.webix_ss_sort_desc')
    }
    rows { col -> $("div.webix_dtable", view_id: field).find('div.webix_column', column: "$col").find('div.webix_cell') }
    cell { row, col -> $("div.webix_dtable", view_id: field).find('div.webix_column', column: "$col").find('div.webix_cell', row) }

    addRowButton { $("div.webix_el_button", view_id: "${field}Add").find('button') }
    removeRowButton { $("div.webix_el_button", view_id: "${field}Remove").find('button') }

    pagerButtons { $("div.webix_pager", view_id: "${field}Pager").find('button') }
  }

  /**
   * Determines if the given row is marked as a selected row.
   * @param row The row index (0= first).
   * @return True if the row is flagged as selected.
   */
  boolean isSelected(int row) {
    return cell(row, 0).classes().contains('webix_row_select')
  }

}
