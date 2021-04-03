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
        maxLength: 'Max Length',
        required: 'required',
        save: 'Save',
        searchStatusGreen: 'Green',
        sequence: 'Sequence',
        title: 'Title',
        valueClassName: 'Value Class',

        _notInOtherLanguages: 'Not',  // Do not add to the other languages.

      }
    }
  }
}