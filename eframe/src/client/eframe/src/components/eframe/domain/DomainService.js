/**
 * JS Service for access to domain object details (DomainController).
 */

export default {

  fieldFormats: {
    'STRING': 'S',
    'INT': 'I',
    'ENUM': 'E',
    'BOOLEAN': 'B',
    /*
BigDecimal - N
DateOnly - D
DateTime - T
☑ Boolean/boolean - B
⌧ Long/long - L
Domain Reference - R
List of Refs - Q
List of Children - C
List of Custom Children - K
Enumeration - E
EncodedType - Y
Configurable Type - G
     */
  },

  // Return the field definitions.
  // eslint-disable-next-line no-unused-vars
  getDisplayFields(domainClassName, successFunction, errorFunction) {
    // TODO: Replace with real query/caching.
    //console.log("getting domain fields: " + domainClassName);

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
              defaultValue: "_max('sequence')+10",
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
              fieldFormat: 'E',
              maxLength: 30,
              defaultValue: "'NONE'",
              validValues: [
                {value: 'NONE', label: 'None'},  // TODO: Localize on server, with synch of en.js to messages.properties?
                {value: 'VALUES', label: 'Values'},
                {value: 'ALL', label: 'All'},
              ]
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

  },
  // Return the field definitions- in a flattened array.
  // eslint-disable-next-line no-unused-vars
  _flattenFieldList(theFields) {
    var fields = []

    for (let field of theFields.top) {
      fields[fields.length] = field
    }
    for (let field of theFields.bottom) {
      fields[fields.length] = field
    }
    for (let tab of theFields.tabs) {
      for (let field of tab.fields) {
        fields[fields.length] = field
      }
    }

    return fields
  },
  // Creates an empty record of the domain being maintained.
  // eslint-disable-next-line no-unused-vars
  _emptyDomain(fields) {
    let record = {}

    //console.log("theComponent.fields: "+JSON.stringify(theComponent.$data.fields));
    let allFields = this._flattenFieldList(fields)

    // Force an empty array for the child record list for the inline grid support.
    for (let field of allFields) {
      if (field.fieldFormat == 'C') {
        record[field.fieldName] = []
      }
    }

    return record
  }


}