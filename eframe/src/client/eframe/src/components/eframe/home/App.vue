<template>
  <div id="app">
    <div class="app-container">
      <DataTable :value="products" :resizableColumns="true" columnResizeMode="expand"
                 class="p-datatable-gridlines editable-cells-table" v-on:column-resize-end="resized" editMode="cell">
        <Column field="id" header="Code"></Column>
        <Column field="name" header="Name">
          <template #editor="slotProps">
            <InputText v-model="slotProps.data[slotProps.column.props.field]"/>
          </template>
        </Column>
        <Column field="category" header="Category"></Column>
        <Column field="quantity" header="Quantity"></Column>
      </DataTable>
    </div>

    <Toast/>
  </div>
  <a href="order">Order</a>
  <a href="orderDashboard">OrderDashboard</a>
</template>

<script>
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';

export default {
  data() {
    return {
      message: null,
      text: null,
      products: [
        {id: 0, name: 'Vegetables and ZZZ'},
        {id: 1, name: 'Cheese'},
        {id: 2, name: 'Whatever else humans are supposed to eat'}
      ]

    }
  },
  methods: {
    resized(event) {
      console.log("resized2");
      console.log(event);
      console.log(this);
      this.$toast.add({severity: 'info', summary: 'Hello '});
    },
    greet() {
      this.$toast.add({severity: 'info', summary: 'Hello ' + this.text});
      this.message = 'Hello ' + this.text;
    }
  },
  components: {
    DataTable, Column
  }
}
</script>

<style scoped>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin-top: 60px;
}

.app-container {
  text-align: center;
}

body #app .p-button {
  margin-left: .2em;
}

form {
  margin-top: 2em;
}
</style>