<template>
  <Toast/>
  <Toolbar>
    <template #left>
      <Button icon="pi pi-bars" class="p-mr-2"/>
    </template>

    <template #right>
      <Button icon="pi pi-cog" class="p-mr-2"/>
      <Button icon="pi pi-power-off" class="p-mr-2"/>
      <Button icon="pi pi-home" class="p-button-success" @click="handleHomeClick($event)"/>
    </template>
  </Toolbar>
</template>


<script>

import Button from 'primevue/button';
import Toast from 'primevue/toast';
import Toolbar from 'primevue/toolbar';

export default {
  name: "StandardHeader",
  components: {
    Button, Toast, Toolbar
  },
  methods: {
    handleHomeClick() {
      console.log('Home clicked');
      window.location = "/"
    },
  }


}
</script>

