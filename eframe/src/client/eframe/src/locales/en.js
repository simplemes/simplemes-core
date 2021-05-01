/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

/**
 * Provides the getMessages() function to define the strings for a given locale.
 */
export default {
  getLocale() {
    return 'en'
  },
  getMessages() {
    return {
      label: {
        cancel: 'Cancel',
        category: 'Category',
        defaultFlexType: 'Default Flex Type',
        delete: 'Delete',
        details: 'Details',
        fieldFormat: 'Format',
        fieldFormatBoolean: 'Boolean',
        fieldFormatChildList: 'Child List',
        fieldFormatConfigurableType: 'Config Type',
        fieldFormatDate: 'Date',
        fieldFormatDateTime: 'Date Time',
        fieldFormatDomainReference: 'Domain Reference',
        fieldFormatEnumeration: 'Enumeration',
        fieldFormatInteger: 'Integer',
        fieldFormatListOfDomainReferences: 'Reference List',
        fieldFormatNumber: 'Number',
        fieldFormatString: 'String',
        fieldName: 'Field',
        fieldLabel: 'Label',
        fields: 'Fields',
        flexType: 'Flex Type',
        historyTracking: 'Tracking',
        historyTrackingNone: 'None',
        historyTrackingValues: 'Values',
        historyTrackingAll: 'All',
        main: 'Main',
        maxLength: 'Max',
        required: 'Required',
        save: 'Save',
        search: 'Search',
        searchStatusGreen: 'Green',
        sequence: 'Sequence',
        title: 'Title',
        valueClassName: 'Class',

        _notInOtherLanguages: 'Not',  // Used for testing. Do not add to the other languages.
      },
      message: {
        deleted: '{record} Deleted',
        deleteConfirm: 'Are you sure you want to delete the selected record ({record})?',
        saved: '{record} Saved',
      },
      title: {
        add: 'Add',
        deleted: 'Deleted',
        edit: 'Edit',
        error: 'Error',
        saved: 'Saved',
      },
      tooltip: {
        addRow: 'Add Row',
        removeRow: 'Remove Row',
      },
    }
  }
}