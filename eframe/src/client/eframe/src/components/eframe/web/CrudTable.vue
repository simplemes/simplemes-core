<template>
  <StandardHeader/>
  <div class="app-container">
    <div class="p-grid">
      <div class="p-col">
        <div class="box">
          <div class="p-row p-m-2">
            <DataTable :value="records" :lazy="true" :paginator="true" :rows="pageSize" ref="crudTable"
                       :totalRecords="totalRecords" :loading="loading" @page="onPage($event)" @sort="onSort($event)"
                       data-testid="CrudTable"
                       stateStorage="local" :stateKey="storageKey" :rowHover="false">
              <template #header>
                <div class="p-d-flex p-jc-between">
                  <Button type="button" icon="pi pi-plus" label="Add" class="p-button-outlined"
                          @click="addRecord"/>
                  <span class="p-input-icon-left p-input-icon-right">
                    <i class="pi pi-search"/>
                    <InputText v-model="requestParams.filter" :placeholder="$t('label.search')" @change="searchChanged"
                               ref="filter"/>
                    <i class="pi pi-times" @click="clearFilter"/>
                </span>
                </div>
              </template>
              <Column v-for="col of columns" :field="col.field" :header="col.header" :key="col.field"
                      :sortable="col.sort"></Column>
              <Column :exportable="false">
                <template #body="slotProps">
                  <Button icon="pi pi-pencil" class="p-button-rounded p-button-outlined p-mr-2"
                          @click="editRecord(slotProps.data)"/>
                  <Button icon="pi pi-ellipsis-h" class="p-button-rounded p-button-outlined p-button-success "
                          @click="optionsMenu(slotProps.data)"/>
                </template>
              </Column>
            </DataTable>
          </div>
        </div>
      </div>
    </div>
  </div>
  <CrudDialog :domainClassName="domainClassName" :service="service" ref="crudDialog" @updatedRecord="onUpdatedRecord"/>
</template>

<script>

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'

import PageHolder from './PageHolder'
import StandardHeader from './StandardHeader'
import CrudDialog from "@/components/eframe/web/CrudDialog"
import DomainService from "@/components/eframe/domain/DomainService"


export default {
  name: 'CrudTable',
  components: {
    StandardHeader, DataTable, Column, InputText, Button, CrudDialog
  },
  props: {
    columns: Array,
    service: Object,
    domainClassName: {
      type: String,
      required: true
    },
    storageKey: {
      type: String,
      required: true
    },
  }, // ['columns', 'service', 'storageKey'],

  data() {
    return {
      text: null,
      loading: false,
      totalRecords: 0,
      pageSize: 10,
      records: [],
      requestParams: {},
      fields: {},
    }
  },
  created() {
    window.$page = new PageHolder(this, DomainService)
  },
  methods: {
    addRecord() {
      this.$refs.crudDialog.openDialog(DomainService._emptyDomain(this.$data.fields))
    },
    clearFilter() {
      this.requestParams.filter = ''
      this.updateData()
    },
    optionsMenu(row) {
      console.log("options row: " + JSON.stringify(row));
    },
    editRecord(row) {
      var clonedRow = JSON.parse(JSON.stringify(row));
      this.$refs.crudDialog.openDialog(clonedRow)
    },
    loadData() {
      this.updateData()
    },
    searchChanged() {
      this.updateData()
    },
    updateData() {
      this.loading = true
      const params = this.requestParams
      let options = {count: params.rows, start: params.first, search: params.filter}
      if (params.sortField) {
        let order = 'asc'
        let key = 'sort[' + params.sortField + ']'
        if (params.sortOrder < 0) {
          order = 'desc'
        }
        options[key] = order
      }
      this.service.list(options, (data) => {
        this.records = data.data
        this.totalRecords = data.total_count
        this.loading = false
      }, (error) => {
        console.log("error: " + error);
        this.loading = false
      });
    },
    onPage(event) {
      this.requestParams = event
      this.updateData()
    },
    onSort(event) {
      this.requestParams = event
      this.updateData()
    },
    onUpdatedRecord() {
      //console.log("event: "+JSON.stringify(event));
      this.updateData()
    },
  },
  mounted() {
    let params = {
      first: 0,
      rows: 10,
      sortField: null,
      sortOrder: null,
      filter: ''
    }
    let s = localStorage.getItem(this.storageKey)
    if (s) {
      params = JSON.parse(s)
    }

    this.requestParams = params
    this.loading = true
    this.loadData()
    this.$refs.filter.$el.focus()

    // Load the fields needed for the add/edit dialogs.
    DomainService.getDisplayFields(this.domainClassName, (data) => {
      this.fields = data
    });
  },
}


</script>

