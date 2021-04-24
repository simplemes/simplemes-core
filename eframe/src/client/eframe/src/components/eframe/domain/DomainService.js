/**
 * JS Service for access to domain object details (DomainController).
 */

export default {

  fieldFormats: {
    'STRING': 'S',
    'INT': 'I',
    'ENUM': 'E',
    'BOOLEAN': 'B',
    'CHILD_LIST': 'C',
    'NUMBER': 'N',
    'DATE': 'D',
    'DATE_TIME': 'T',
    'DOMAIN_REFERENCE': 'R',
    'REFERENCE_LIST': 'Q',
    'CONFIG_TYPE': 'G',
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
              fieldLabel: 'label.fieldFormat',
              fieldFormat: 'E',
              defaultValue: "'NONE'",
              validValues: [
                {value: 'S', label: 'label.fieldFormatString'},
                {value: 'I', label: 'label.fieldFormatInteger'},
                {value: 'N', label: 'label.fieldFormatNumber'},
                {value: 'D', label: 'label.fieldFormatDate'},
                {value: 'T', label: 'label.fieldFormatDateTime'},
                {value: 'B', label: 'label.fieldFormatBoolean'},
                {value: 'R', label: 'label.fieldFormatDomainReference'},
                {value: 'Q', label: 'label.fieldFormatListOfDomainReferences'},
                {value: 'C', label: 'label.fieldFormatChildList'},
                {value: 'E', label: 'label.fieldFormatEnumeration'},
                {value: 'G', label: 'label.fieldFormatConfigurableType'},
              ],
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
              fieldLabel: 'label.historyTracking',
              fieldFormat: 'E',
              defaultValue: "'NONE'",
              validValues: [
                {value: 'NONE', label: 'label.historyTrackingNone'},
                {value: 'VALUES', label: 'label.historyTrackingValues'},
                {value: 'ALL', label: 'label.historyTrackingAll'},
              ],
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

    this._localizeLabels(dummy, true)
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
  },
  // Localizes the labels found in the domain definitions.  Walks all fields.
  _localizeLabels(theFields, flatten) {
    var fields = theFields
    if (flatten) {
      fields = this._flattenFieldList(theFields)
    }

    for (let field of fields) {
      if (field.validValues) {
        for (let v of field.validValues) {
          if (v.label && v.label.indexOf('.') >= 0) {
            v.label = window.$page.vue.$t(v.label)
          }
        }
      }

      // Localize any fields in a child inline grid.
      if (field.fieldFormat === this.fieldFormats.CHILD_LIST) {
        this._localizeLabels(field.fields, false)
      }

    }
  },
}