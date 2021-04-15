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
                          @click="openAddDialog"/>
                  <span class="p-input-icon-left p-input-icon-right">
                    <i class="pi pi-search"/>
                    <InputText v-model="requestParams.filter" placeholder="Search" @change="searchChanged"
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
  <Dialog v-model:visible="addDialogVisible" :breakpoints="{'960px': '95vw', '640px': '100vw'}" :style="{width: '80vw'}"
          header="Add" :modal="true" :maximizable="true">
    <div class="p-fluid p-formgrid p-grid p-ai-center">
      <StandardField v-for="field in fields.top" :key="field.fieldName" :field="field"/>
      <div class="p-col-12"></div>
      <StandardField v-for="field in fields.bottom" :key="field.fieldName" :field="field"/>
    </div>

    <template #footer>
      <Button icon="pi pi-times" :label="$t('label.cancel')" class="p-button-text" @click="closeAddDialog"/>
      <Button icon="pi pi-check" :label="$t('label.save')" class="p-button-text"/>
    </template>
  </Dialog>
</template>

<script>

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import PageHolder from './PageHolder'
import StandardHeader from './StandardHeader'
import DomainService from "@/components/eframe/domain/DomainService"
import StandardField from "@/components/eframe/domain/StandardField"


export default {
  name: 'CrudTable',
  components: {
    StandardHeader, StandardField, DataTable, Column, InputText, Button, Dialog,
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
      addDialogVisible: false,
      fields: {},
    }
  },
  created() {
    window.$page = new PageHolder(this)
  },
  methods: {
    clearFilter() {
      this.requestParams.filter = ''
      this.updateData()
    },
    closeAddDialog() {
      this.addDialogVisible = false
    },
    openAddDialog() {
      this.addDialogVisible = true
    },
    optionsMenu(row) {
      console.log("options row: " + JSON.stringify(row));
    },
    editRecord(row) {
      console.log("row: " + JSON.stringify(row));
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
      //console.log("top: " + JSON.stringify(data.top));


    });


  },
}


</script>

