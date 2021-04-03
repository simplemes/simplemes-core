/**
 * JS Service for access to domain object details (DomainController).
 */

export default {
  // Return the field definitions.
  // eslint-disable-next-line no-unused-vars
  getDisplayFields(domainClassName, successFunction, errorFunction) {
    // TODO: Replace with real query/caching.
    console.log("domainClassName: " + domainClassName);

    const dummy = {
      top: [
        {
          fieldName: 'flexType',
          fieldLabel: 'label.flexType',
          fieldFormat: 'S',
          fieldDefault: '',
          maxLength: 30,
        },
      ],
      bottom: [
        {
          fieldName: 'category',
          fieldLabel: 'label.category',
          fieldFormat: 'S',
          fieldDefault: '',
          maxLength: 20,
        },
        {
          fieldName: 'defaultFlexType',
          fieldLabel: 'label.defaultFlexType',
          fieldFormat: 'B',
        },
        {
          fieldName: 'title',
          fieldLabel: 'label.title',
          fieldFormat: 'S',
          fieldDefault: '',
          maxLength: 80,
        },
        {
          fieldName: 'fields',
          fieldLabel: 'label.fields',
          fieldFormat: 'C',
          fields: [
            {
              fieldName: 'sequence',
              fieldLabel: 'label.sequence',
              fieldFormat: 'I',
            },
            {
              fieldName: 'fieldName',
              fieldLabel: 'label.fieldName',
              fieldFormat: 'S',
              maxLength: 30,
            },
            {
              fieldName: 'fieldLabel',
              fieldLabel: 'label.fieldLabel',
              fieldFormat: 'S',
              maxLength: 80,
            },
            {
              fieldName: 'fieldFormat',
              fieldLabel: 'label.fieldFormat',  // TODO: Support Enum?
              fieldFormat: 'S',
              maxLength: 30,
            },
            {
              fieldName: 'maxLength',
              fieldLabel: 'label.maxLength',
              fieldFormat: 'I',
            },
            {
              fieldName: 'required',
              fieldLabel: 'label.required',
              fieldFormat: 'B',
            },
            {
              fieldName: 'historyTracking',
              fieldLabel: 'label.historyTracking',  // TODO: Support Enum?
              fieldFormat: 'S',
              maxLength: 30,
            },
            {
              fieldName: 'valueClassName',
              fieldLabel: 'label.valueClassName',
              fieldFormat: 'S',
              maxLength: 255,
            },

          ],
        },
      ],
      tabs: [
        {
          tab: 'MAIN',
          tabLabel: 'label.main',
          fields: [{
            fieldName: 'title',
            fieldLabel: 'label.title',
            fieldFormat: 'S',
            fieldDefault: '',
            maxLength: 20,
          }
          ]
        },
        {
          tab: 'DETAILS',
          tabLabel: 'Details',
          fields: [
            {
              fieldName: 'warehouse',
              fieldLabel: 'label.warehouse',
              fieldFormat: 'S',
              fieldDefault: '',
              maxLength: 20,
            }
          ]
        }
      ]
    }

    successFunction(dummy)

  }
}