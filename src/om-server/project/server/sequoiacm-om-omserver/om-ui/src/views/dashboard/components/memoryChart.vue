<template>
  <div class="chart" ref="chart" ></div>
</template>

<script>
import { Line } from '@antv/g2plot';

import { getHeapInfo } from '@/api/monitor'

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
      maxHeapSize: '',
      interval: 2000,
    }
  },
  methods: {
    init() {
      this.initChartData()
      getHeapInfo(this.instanceId).then(res => {
        this.pushChartData(res.data)
        let unit = "M"
        let maxHeapSize = Math.floor(res.data.max / 1024)
        if (maxHeapSize > 1024) {
          maxHeapSize = (maxHeapSize / 1024).toFixed(2)
          unit = "G"
        }
        const chartConfig = {
          data: this.chartData,
          xField: 'time',
          yField: 'value',
          seriesField: "type",
          xAxis: {
            range: [0, 1],
            tickCount: this.tickCount
          },
          yAxis: {
            label: {
              formatter: (v) => v + "M",
            }
          },
          area: {
            style: {
              fillOpacity: 0.15
            }
          },
          legend: {
            title: {
              text: 'MAX HEAP SIZE: ' + maxHeapSize + unit,
              spacing: 8
            }
          },
          smooth: true,
          color:['#409EFF',  '#67C23A'],
          tooltip: {
            formatter: (datum) => {
              return {
                name: datum.type,
                value: datum.value + 'M'
              }
            }
          }
        }
        const chart = new Line(this.$refs.chart, chartConfig)
        chart.render()
        this.chart = chart
        this.setRefresh()
      })
    },
    setRefresh() {
      this.refresher = setTimeout(() => {
        getHeapInfo(this.instanceId).then(res => {
          this.pushChartData(res.data)
          this.chart.changeData(this.chartData)
          this.setRefresh()
        })
      }, this.interval)
    },
    pushChartData(data) {
      this.chartData.splice(0,2)
      let time = this.$util.parseTime(new Date, '{h}:{i}:{s}')
      this.chartData.push( 
        {
          time: time,
          value: Math.floor(data.size / 1024),
          type: "size"
        },
        {
          time: time,
          value: Math.floor(data.used / 1024),
          type: "used"
        },
      )
    },
    initChartData() {
      this.chartData = []
      for(let i=0; i<this.tickCount; i++) {
        this.chartData.push( 
          {
            time: '',
            value: 0,
            type: "size"
          },
          {
            time: '',
            value: 0,
            type: "used"
          }
        )
      }
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