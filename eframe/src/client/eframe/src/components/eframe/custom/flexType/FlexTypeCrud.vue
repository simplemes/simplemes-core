<template>
  <StandardHeader/>
  <div class="app-container">
    <div class="p-grid">
      <div class="p-col">
        <div class="box">
          <div class="p-row p-m-2">
            <DataTable :value="flexTypes" :lazy="true" :paginator="true" :rows="pageSize" ref="crudTable"
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

import GeneralPage from '../../web/GeneralPage';
import StandardHeader from '../../web/StandardHeader';
import FlexTypeService from './FlexTypeService'


export default {
  components: {
    StandardHeader, DataTable, Column, InputText, Button
  },
  data() {
    return {
      text: null,
      loading: false,
      columns: null,
      totalRecords: 0,
      flexTypes: [],
      pageSize: 10,
      pageStart: 0,
      filter: '',
    }
  },
  created() {
    //window.$topComponent = this
    window.$page = new GeneralPage(this)
    this.columns = [
      {field: 'flexType', header: this.$t('label.flexType')},
      {field: 'category', header: this.$t('label.category')},
      {field: 'title', header: this.$t('label.title')},
      {field: 'fieldSummary', header: this.$t('label.fields')},
    ];
  },
  methods: {
    clearFilter() {
      this.filter = '';
      this.updateData()
    },
    searchChanged() {
      this.updateData()
    },
    startButtonClicked() {
      FlexTypeService.list((data) => {
        console.log(data);
      });
      //this.$toast.add({severity: 'success', summary: 'Start', detail: 'clicked', life: 3000});
    },
    loadData() {
      this.loading = true;
      FlexTypeService.list({}, (data) => {
        this.flexTypes = data.data;
        this.totalRecords = data.total_count;
        this.loading = false;
      });
    },
    updateData() {
      this.loading = true;
      let options = {count: this.pageSize, start: this.pageStart, search: this.filter}
      FlexTypeService.list(options, (data) => {
        this.flexTypes = data.data;
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

