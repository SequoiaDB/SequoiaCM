<template>
  <panel title="进程信息" >
    <div class="level" v-if="processInfo.pid">
      <div  class="level-item has-text-centered">
        <div>
          <p class="heading" v-text="'pid'" />
          <p v-text="processInfo.pid" />
        </div>
      </div>
      <div  class="level-item has-text-centered">
        <div>
          <p class="heading" v-text="'UPTIME'" />
          <p v-text="uptimeStr" />
        </div>
      </div>
      <div  class="level-item has-text-centered">
        <div>
          <p class="heading" v-text="'PROCESS CPU USAGE'" />
          <p v-text="(processInfo.process_cpu_usage*100).toFixed(2) + '%'" />
        </div>
      </div>
      <div  class="level-item has-text-centered">
        <div>
          <p class="heading" v-text="'SYSTEM CPU USAGE'" />
          <p v-text="(processInfo.system_cpu_usage*100).toFixed(2) + '%'" />
        </div>
      </div>
      <div  class="level-item has-text-centered">
        <div>
          <p class="heading" v-text="'CPUS'" />
          <p v-text="processInfo.cpus" />
        </div>
      </div>
    </div>
  </panel>
</template>


<script>
import Panel from '@/components/Panel'

import { getProcessInfo } from '@/api/monitor'
import moment from 'moment'

export default {
  props: {
    instanceId: String
  },
  components: {
    Panel
  },
  data() {
    return {
      processInfo: {},
      refresher: '',
      interval: 2000,
      uptime: 0,
      timeAdder: ''
    }
  },
  methods: {
    init() {
      getProcessInfo(this.instanceId).then(res => {
        this.processInfo = res.data
        this.uptime = this.processInfo.uptime
        this.startTimeAdder()
        this.setRefresh()
      })
    },
    setRefresh() {
      this.refresher = setTimeout(() => {
        getProcessInfo(this.instanceId).then(res => {
          this.processInfo = res.data
          this.setRefresh()
        })
      }, this.interval)
    },
    startTimeAdder() {
      this.timeAdder = setInterval(() => {
        this.uptime += 1000
      }, 1000)
    },
    closeTimeAdder() {
      clearInterval(this.timeAdder)
    }
  },
  computed: {
    uptimeStr(){
      const duration = moment.duration(this.uptime)
      return `${Math.floor(duration.asDays())}d ${duration.hours()}h ${duration.minutes()}m ${duration.seconds()}s`
    }
  },
  mounted() {
    this.init()
  },
  beforeDestroy() {
    clearTimeout(this.refresher)
    this.closeTimeAdder()
  }
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