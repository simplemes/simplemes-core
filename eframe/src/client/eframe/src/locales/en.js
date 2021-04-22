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
        defaultFlexType: 'Default Flex Type',
        cancel: 'Cancel',
        category: 'Category',
        fieldFormat: 'Format',
        fieldName: 'Field',
        fieldLabel: 'Label',
        fields: 'Fields',
        flexType: 'Flex Type',
        historyTracking: 'Tracking',
        historyTrackingNone: 'None',
        historyTrackingValues: 'Values',
        historyTrackingAll: 'All',
        maxLength: 'Max Length',
        required: 'Required',
        save: 'Save',
        search: 'Search',
        searchStatusGreen: 'Green',
        sequence: 'Sequence',
        title: 'Title',
        valueClassName: 'Class',

        _notInOtherLanguages: 'Not',  // Used for testing. Do not add to the other languages.
      },
      title: {
        add: 'Add',
        edit: 'Edit',
      },
    }
  }
}