<template>
  <div class="chart" ref="chart" ></div>
</template>

<script>
import { Line } from '@antv/g2plot';

import { getThreadInfo } from '@/api/monitor'

export default {
  props: {
    instanceId: String
  },
  data() {
    return {
      tickCount: 5,
      chartData: [],
      chart: '',
      refresher: '',
      interval: 2000,
    }
  },
  methods: {
    init() {
      this.initChartData()
      getThreadInfo(this.instanceId).then(res => {
        this.pushChartData(res.data)
        const chartConfig = {
          data: this.chartData,
          xField: 'time',
          yField: 'value',
          seriesField: "type",
          xAxis: {
            range: [0, 1],
            tickCount: this.tickCount
          },
          area: {
            style: {
              fillOpacity: 0.15
            }
          },
          color:['#409EFF', '#909399', '#67C23A'],
          smooth: true
        }
        const chart = new Line(this.$refs.chart, chartConfig)
        chart.render()
        this.chart = chart
        this.setRefresh()
      })
    },
    pushChartData(data) {
      this.chartData.splice(0,3)
      let time = this.$util.parseTime(new Date, '{h}:{i}:{s}')
      this.chartData.push( 
          {
            time: time,
            value: data.all,
            type: "all"
          },
          {
            time: time,
            value: data.waiting,
            type: "waiting"
          },
          {
            time: time,
            value: data.runnable,
            type: "runnable"
          }
        )
    },
    initChartData() {
      this.chartData = []
      for(let i=0; i<this.tickCount; i++) {
        this.chartData.push( 
          {
            time: "",
            value: 0,
            type: "all"
          },
          {
            time: "",
            value: 0,
            type: "waiting"
          },
          {
            time: "",
            value: 0,
            type: "runnable"
          }
        )
      }
    },
    setRefresh() {
      this.refresher = setTimeout(() => {
        getThreadInfo(this.instanceId).then(res => {
          this.pushChartData(res.data)
          this.chart.changeData(this.chartData)
          this.setRefresh()
        })
      }, this.interval)
    },
  },
  mounted() {
    this.init()
  },
  beforeDestroy() {
    clearTimeout(this.refresher)
  }
 
}
</script>

<style scoped>
</style>