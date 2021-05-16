/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

/**
 * JS Service for access to domain object details (DomainController).
 */
import InMemoriam from 'in-memoriam'

const cache = new InMemoriam(10, 600000)

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
    if (cache.get(domainClassName)) {
      successFunction(cache.get(domainClassName))
      //console.log("cache: " + JSON.stringify(cache.stats));
      return
    }

    const url = '/domain/displayFields?domain=' + domainClassName;
    window.$page.vue.axios.get(url).then((response) => {
      let theFields = response.data
      // Make sure all the top-level elements are present (empty)
      if (!theFields.tabs) {
        theFields.tabs = []
      }
      if (!theFields.top) {
        theFields.top = []
      }
      if (!theFields.bottom) {
        theFields.bottom = []
      }
      this._localizeLabels(theFields, true)

      successFunction(theFields)
      cache.set(domainClassName, theFields)
    }).catch((error) => {
      window.$page.handleError(error, url)
    })

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

    // Fill in any default values.
    for (let field of allFields) {
      if (field.defaultValue) {
        let value = eval(field.defaultValue)
        if (value) {
          record[field.fieldName] = value
        }
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