<template>
  <DataTable :value="records" ref="inlineGrid" class="p-datatable-sm"
             @sort="onSort($event)"
             data-testid="inlineGrid"
             stateStorage="local" :stateKey="storageKey" :rowHover="false">
    <template #header>
      <div class="p-d-flex p-jc-between">
        <Button type="button" icon="pi pi-plus  p-input-icon-right" class="p-button-outlined" @click="addRow"/>
        <Button type="button" icon="pi pi-minus p-input-icon-right" class="p-button-outlined" @click="openAddDialog"/>
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
      this.records[this.records.length] = {sequence: 10}
      console.log("recs: " + JSON.stringify(this.records));
    },
  },
  mounted() {

  },
}


</script>

