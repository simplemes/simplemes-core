<!--
  - Copyright (c) Michael Houston 2021. All rights reserved.
  -->

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
                       stateStorage="local" :stateKey="computedStateKey" :rowHover="false">
              <template #header>
                <div class="p-d-flex p-jc-between">
                  <Button type="button" icon="pi pi-plus" class="p-button-outlined" @click="addRecord"
                          :label="$t('label.add')" :title="$t('tooltip.addCrud')" id="addRecord"/>
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
                          @click="optionsMenu(slotProps.data,$event)" aria-haspopup="true"
                          aria-controls="overlay_menu"/>
                </template>
              </Column>
            </DataTable>
          </div>
        </div>
      </div>
    </div>
  </div>
  <Menu id="rowMenu" ref="rowMenu" :model="rowMenuItems" :popup="true"/>
  <CrudDialog :domainClassName="domainClassName" :service="service" ref="crudDialog" @updatedRecord="onUpdatedRecord"/>
  <Dialog v-model:visible="confirmDeleteDialogVisible" :style="{width: '450px'}" header="Confirm" :modal="true">
    <div class="confirmation-content">
      <i class="pi pi-exclamation-triangle p-mr-3" style="font-size: 2rem"/>
      <span>{{ $t('message.deleteConfirm', {record: this.service.buildLabel(this.rowMenuRecord, true)}) }}</span>
    </div>
    <template #footer>
      <Button :label="$t('label.cancel')" icon="pi pi-times" class="p-button-text"
              @click="confirmDeleteDialogVisible = false"/>
      <Button :label="$t('label.delete')" icon="pi pi-check" class="p-button-text" @click="deleteSelectedRow"/>
    </template>
  </Dialog>
</template>

<script>

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Menu from 'primevue/menu'
import Dialog from 'primevue/dialog'

import PageHolder from './PageHolder'
import StandardHeader from './StandardHeader'
import CrudDialog from "./CrudDialog"
import DomainService from "../domain/DomainService"


export default {
  name: 'CrudTable',
  components: {
    StandardHeader, DataTable, Column, InputText, Button, CrudDialog, Menu, Dialog
  },
  props: {
    columns: Array,
    service: Object,
    domainClassName: {
      type: String,
      required: true
    },
  },

  data() {
    return {
      text: null,
      loading: false,
      totalRecords: 0,
      pageSize: 10,
      records: [],
      requestParams: {},
      confirmDeleteDialogVisible: false,
      rowMenuRecord: {},  // The record for the row menu.
      rowMenuVisible: false,
      rowMenuItems: [         // TODO: Support added menu acrions. Using a computed method to combine.
        {
          label: this.$t('label.delete'),
          icon: 'pi pi-times',
          command: () => {
            this.confirmDeleteDialogVisible = true
          }
        }
      ],
    }
  },
  computed: {
    computedStateKey() {
      // Provides a state key name for the list's local storage.  Uses domainClass.
      return this.domainClassName + 'List'
    }
  },
  created() {
    window.$page = new PageHolder(this, DomainService)
  },
  methods: {
    addRecord() {
      // Load the fields needed for the add dialog.
      DomainService.getDisplayFields(this.domainClassName, (fields) => {
        this.$refs.crudDialog.openDialog(DomainService._emptyDomain(fields))
      });
    },
    clearFilter() {
      this.requestParams.filter = ''
      this.updateData()
    },
    optionsMenu(row, event) {
      this.rowMenuRecord = row
      this.$refs.rowMenu.toggle(event)
    },
    deleteSelectedRow() {
      this.service.delete(this.rowMenuRecord, () => {
        this.confirmDeleteDialogVisible = false
        this.updateData()
        const s = this.$t('message.deleted', {record: this.service.buildLabel(this.rowMenuRecord, true)})
        this.$toast.add({severity: 'success', summary: this.$t('title.deleted'), detail: s, life: 5000})
      })

    },
    editRecord(row) {
      var clonedRow = JSON.parse(JSON.stringify(row))
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
      options.domainClassName = this.domainClassName
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
    let s = localStorage.getItem(this.computedStateKey)
    if (s) {
      params = JSON.parse(s)
    }

    this.requestParams = params
    this.loading = true
    this.loadData()
    this.$refs.filter.$el.focus()
  },
}


</script>

