<template>
  <div class="chart" ref="chart" ></div>
</template>
<script>

import { Area } from '@antv/g2plot';

import { getConnectionInfo } from '@/api/monitor'


export default {
  props: {
    instanceId: String
  },
  data() {
    return {
      chartData: [],
      chart: '',
      refresher: '',
      interval: 2000,
      tickCount: 5,
    }
  },
  methods: {
    init() {
      this.initChartData()
      getConnectionInfo(this.instanceId).then( res => {
        this.pushChartData(res.data)
        const chartConfig = {
          data: this.chartData,
          xField: 'time',
          yField: 'connection_count',
          xAxis: {
            range: [0, 1],
            tickCount: this.tickCount
          },
          areaStyle: {
            fill: 'l(270) 0:#ffffff 0.5:#7ec2f3 1:#1890ff'
          },
          color:['#409EFF'],
          smooth: true
        }
        const area = new Area(this.$refs.chart, chartConfig)
        area.render()
        this.chart = area
        this.setRefresh()
      })
    },
    pushChartData(data) {
      this.chartData.splice(0,1)
      let time = this.$util.parseTime(new Date, '{h}:{i}:{s}')
      this.chartData.push( 
        {
          time: time,
          connection_count: data.connection_count
        }
      )
    },
    initChartData() {
      this.chartData = []
      for(let i=0; i<this.tickCount; i++) {
        this.chartData.push( 
          {
            time: '',
            connection_count: 0
          }
        )
      }
    },
    setRefresh() {
      this.refresher = setTimeout(() => {
        getConnectionInfo(this.instanceId).then( res=> {
          this.pushChartData(res.data)
          this.chart.changeData(this.chartData)
          this.setRefresh()
        })
      }, this.interval)
    }
  },
  mounted() {
      this.init()
  },
  beforeDestroy() {
    clearTimeout(this.refresher)
  },
 
}
</script>

<style scoped>
</style>