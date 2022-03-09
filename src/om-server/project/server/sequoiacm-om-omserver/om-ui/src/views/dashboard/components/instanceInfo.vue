<template>
  <panel title="节点信息" >
    <div class="level" v-if="instanceInfo.service_name">
      <div  class="level-item has-text-centered">
        <div>
          <p class="heading" v-text="'service'" />
          <p v-text="instanceInfo.service_name" />
        </div>
      </div>
      <div  class="level-item has-text-centered">
        <div>
          <p class="heading" v-text="'version'" />
          <p v-text="'v' + instanceInfo.metadata.version" />
        </div>
      </div>
      <div  class="level-item has-text-centered">
        <div>
          <p class="heading" v-text="'url'" />
          <p v-text="'http://' + instanceInfo.host_name + ':' + instanceInfo.port" />
        </div>
      </div>
      <div  class="level-item has-text-centered">
        <div>
          <p class="heading" v-text="'region/zone'" />
          <p v-text="instanceInfo.region + '/' + instanceInfo.zone" />
        </div>
      </div>
      <div  class="level-item has-text-centered">
        <div>
          <p class="heading" v-text="'managementPort'" />
          <p v-text="instanceInfo.management_port" />
        </div>
      </div>
    </div>
  </panel>
</template>


<script>
import Panel from '@/components/Panel'


export default {
  props: {
    instanceInfo: Object
  },
  components: {
    Panel
  },
  data() {
    return {
  
    }
  },
  methods: {}
    
}
</script>

<style scoped>

.level {
  align-items: center;
  justify-content: space-between;
  display: flex;
}


.level-item {
  align-items: center;
  display: flex;
  flex-basis: auto;
  flex-grow: 0;
  flex-shrink: 0;
  justify-content: center;
}
.has-text-centered {
  text-align: center!important;
}

.heading {
  display: block;
  font-size: 11px;
  letter-spacing: 1px;
  margin-bottom: 5px;
  text-transform: uppercase;
}

.level>.level-item:not(.is-narrow) {
  flex-grow: 1;
}

@media (max-width:1300px)  {
  .level {
    flex-flow: column;
  }
}
</style>