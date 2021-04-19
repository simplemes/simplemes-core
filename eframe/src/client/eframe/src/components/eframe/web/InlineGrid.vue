<template>
  <DataTable :value="this.$attrs.records" ref="inlineGrid" class="p-datatable-sm editable-cells-table"
             @sort="onSort($event)"
             data-testid="inlineGrid"
             stateStorage="local" :stateKey="storageKey" :rowHover="false"
             editMode="cell" @cellEditInit="onCellEditInit">
    <template #header>
      <div class="p-d-flex p-jc-between">
        <Button type="button" icon="pi pi-plus  p-input-icon-right" class="p-button-outlined" @click="addRow"/>
        <Button type="button" icon="pi pi-minus p-input-icon-right" class="p-button-outlined" @click="addRow"/>
      </div>
    </template>
    <Column v-for="col of columns" :field="col.fieldName" :header="$t(col.fieldLabel)" :key="col.fieldName"
            :sortable="col.sort">
      <template #editor="slotProps">
        <InputText v-model="slotProps.data[slotProps.column.props.field]" id="cellEditorField"/>
      </template>
    </Column>
  </DataTable>
</template>

<script>

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'


export default {
  name: 'InlineGrid',
  components: {
    DataTable, Column, Button, InputText
  },
  props: {
    columns: Array,
    storageKey: {
      type: String,
      required: true
    },
  }, // ['columns', 'service', 'storageKey'],

  created() {
  },
  methods: {
    addRow() {
      var row = {sequence: 10}
      for (let col of this.columns) {
        if (col.defaultValue) {
          theRecords = this.$attrs.records
          let value = eval(col.defaultValue)
          if (value) {
            row[col.fieldName] = value
          }
        }
      }
      if (this.$attrs.records == undefined) {
        this.$attrs.records = []

      }
      this.$attrs.records[this.$attrs.records.length] = row
    },
    onCellEditInit() {
      //const fieldName = event.field
      setTimeout(function () {
        const element = document.getElementById('cellEditorField')
        if (element) {
          element.focus()
          element.select()
        }

      }, 100)

    },
  },
  mounted() {

  },
}

let theRecords;

// Method to calculate the max value of the a column in the current inline grid.
// eslint-disable-next-line no-unused-vars
function _max(fieldName) {
  let max = 0
  if (!theRecords) {
    return 0
  }
  for (let record of theRecords) {
    if (record[fieldName]) {
      if (record[fieldName] > max) {
        max = record[fieldName]
      }
    }
  }

  return max

}

</script>

