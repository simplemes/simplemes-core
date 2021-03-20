<template>
  <StandardHeader/>
  <div class="app-container">
    <div class="p-grid">
      <div class="p-col">
        <div class="box">
          <div class="p-row p-m-2">
            <DataTable :value="records" :lazy="true" :paginator="true" :rows="pageSize" ref="crudTable"
                       :totalRecords="totalRecords" :loading="loading" @page="onPage($event)">
              <template #header>
                <div class="p-d-flex p-jc-between">
                  <Button type="button" icon="pi pi-plus" label="Add" class="p-button-outlined"
                          @click="clearFilter"/>
                  <span class="p-input-icon-left p-input-icon-right">
                    <i class="pi pi-search"/>
                    <InputText v-model="filter" placeholder="Search" @change="searchChanged" ref="filter"/>
                    <i class="pi pi-times" @click="clearFilter"/>
                </span>
                </div>
              </template>
              <Column v-for="col of columns" :field="col.field" :header="col.header" :key="col.field"></Column>
            </DataTable>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import InputText from 'primevue/inputtext';
import Button from 'primevue/button';

import PageHolder from '@/components/eframe/web/PageHolder';
import StandardHeader from '@/components/eframe/web/StandardHeader';


export default {
  name: 'CrudTable',
  components: {
    StandardHeader, DataTable, Column, InputText, Button
  },
  props: ['columns', 'service'],
  data() {
    return {
      text: null,
      loading: false,
      totalRecords: 0,
      records: [],
      pageSize: 10,
      pageStart: 0,
      filter: '',
    }
  },
  created() {
    window.$page = new PageHolder(this)
  },
  methods: {
    clearFilter() {
      this.filter = '';
      this.updateData()
    },
    searchChanged() {
      this.updateData()
    },
    loadData() {
      this.loading = true;

      this.service.list({}, (data) => {
        this.records = data.data;
        this.totalRecords = data.total_count;
        this.loading = false;
      });
    },
    updateData() {
      this.loading = true;
      let options = {count: this.pageSize, start: this.pageStart, search: this.filter}
      this.service.list(options, (data) => {
        this.records = data.data;
        this.totalRecords = data.total_count;
        this.loading = false;
      });
    },
    onPage(event) {
      this.pageStart = event.first
      this.updateData()
    },
  },
  mounted() {
    this.loading = true;
    this.loadData();
    this.$refs.filter.$el.focus();
  },
}


</script>

