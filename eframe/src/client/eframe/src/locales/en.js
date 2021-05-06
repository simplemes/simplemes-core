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
        booleanFieldFormat: 'Doolean',
        cancel: 'Cancel',
        category: 'Category',
        childListFieldFormat: 'Child List',
        customChildListFieldFormat: 'Custom Child List',
        dateFieldFormat: 'Date/Time',
        dateOnlyFieldFormat: 'Date',
        defaultFlexType: 'Default Flex Type',
        delete: 'Delete',
        details: 'Details',
        disabledStatus: 'disabledStatus',
        domainReferenceFieldFormat: 'Domain Reference',
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
        flexType: 'Flex Type',
        historyTracking: 'Tracking',
        historyTrackingAll: 'All',
        historyTrackingNone: 'None',
        historyTrackingValues: 'Values',
        integerFieldFormat: 'Integer',
        longFieldFormat: 'Long',
        main: 'Main',
        maxLength: 'Max',
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
      reportTimeIntervalEnum: {
        CUSTOM_RANGE: 'Custom Range',
        LAST_24_HOURS: 'Last 24 Hours',
        LAST_7_DAYS: 'Last 7 Days',
        LAST_30_DAYS: 'Last 30 Days',
        LAST_6_MONTHS: 'Last 6 Months',
        LAST_MONTH: 'Last Month',
        LAST_YEAR: 'Last Year',
        THIS_MONTH: 'This Month',
        THIS_YEAR: 'This Year',
        TODAY: 'Today',
        YESTERDAY: 'Yesterday',
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