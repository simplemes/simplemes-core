<template>
  <DataTable :value="records" ref="inlineGrid" class="p-datatable-sm"
             @sort="onSort($event)"
             data-testid="inlineGrid"
             stateStorage="local" :stateKey="storageKey" :rowHover="false">
    <template #header>
      <div class="p-d-flex p-jc-between">
        <Button type="button" icon="pi pi-plus  p-input-icon-right" class="p-button-outlined" @click="addRow"/>
        <Button type="button" icon="pi pi-minus p-input-icon-right" class="p-button-outlined" @click="addRow"/>
      </div>
    </template>
    <Column v-for="col of columns" :field="col.fieldName" :header="$t(col.fieldLabel)" :key="col.fieldName"
            :sortable="col.sort"></Column>
  </DataTable>
</template>

<script>

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'


export default {
  name: 'InlineGrid',
  components: {
    DataTable, Column, Button
  },
  props: {
    columns: Array,
    storageKey: {
      type: String,
      required: true
    },
  }, // ['columns', 'service', 'storageKey'],

  data() {
    return {
      records: [{sequence: 10}],
    }
  },
  created() {
  },
  methods: {
    addRow() {
      var row = {sequence: 10}
      for (let col of this.columns) {
        if (col.defaultValue) {
          theRecords = this.records
          let value = eval(col.defaultValue)
          if (value) {
            row[col.fieldName] = value
          }
        }
      }
      this.records[this.records.length] = row
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

