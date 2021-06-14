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
        bigDecimalFieldFormat: 'Number',
        booleanFieldFormat: 'Boolean',
        cancel: 'Cancel',
        category: 'Category',
        childListFieldFormat: 'Child List',
        customChildListFieldFormat: 'Custom Child List',
        dateFieldFormat: 'Date/Time',
        dateOnlyFieldFormat: 'Date',
        dateTime: 'Date/Time',
        defaultFlexType: 'Default Flex Type',
        delete: 'Delete',
        details: 'Details',
        disabledStatus: 'disabledStatus',
        domainReferenceFieldFormat: 'Domain Reference',
        enabled: 'enabled',
        enabledStatus: 'enabledStatus',
        encodedTypeFieldFormat: 'Encoded Type',
        enumFieldFormat: 'Enumeration',
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
        fieldLabel: 'Label',
        fieldName: 'Field',
        fields: 'Fields',
        format: 'Format',
        historyTracking: 'Tracking',
        historyTrackingAll: 'All',
        historyTrackingNone: 'None',
        historyTrackingValues: 'Values',
        integerFieldFormat: 'Integer',
        key: 'Key',
        longFieldFormat: 'Long',
        main: 'Main',
        maxLength: 'Max',
        name: 'Name',
        notes: 'Notes',
        qty: 'Qty',
        required: 'Required',
        save: 'Save',
        search: 'Search',
        searchStatusGreen: 'Green',
        sequence: 'Sequence',
        stringFieldFormat: 'String',
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