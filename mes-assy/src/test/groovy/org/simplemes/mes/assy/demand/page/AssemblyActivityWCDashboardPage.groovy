package org.simplemes.mes.assy.demand.page


import org.simplemes.eframe.test.page.GridModule
import org.simplemes.eframe.test.page.TextFieldModule
import org.simplemes.mes.demand.page.WorkCenterSelectionDashboardPage

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The page definition for a dashboard with the assembly activity and a standard work center selection page.
 * The assembly activity is in the second (B) panel.
 */
@SuppressWarnings("unused")
class AssemblyActivityWCDashboardPage extends WorkCenterSelectionDashboardPage {

  static content = {
    componentList { module(new GridModule(field: 'componentListB')) }
    addButton { index -> $('button#add', (Integer) index) }
    removeButton { index -> $('button#remove', (Integer) index) }

    // Elements on the add component dialog (dialog0)
    addQtyField { module(new TextFieldModule(field: 'qty')) }
    addField1 { module(new TextFieldModule(field: 'assemblyData_FIELD1')) }
    addField2 { module(new TextFieldModule(field: 'assemblyData_FIELD2')) }
    addCancelButton { $('div', view_id: 'dialog0-cancel') }
    addAssembleButton { $('div', view_id: 'dialog0-assemble') }

    // Elements on the remove component dialog (dialog0)
    removeCheckBox { index -> $("input#removeComp${index}") }  // Index starts at 1.
    removeCheckBoxText { index -> $("li#removeCompText${index}").text() }   // Index starts at 1.
    removeCancelButton { $('div', view_id: 'dialog0-cancel') }
    removeRemoveButton { $('div', view_id: 'dialog0-remove') }

  }

}
