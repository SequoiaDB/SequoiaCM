<template>
  <div class="chart" ref="chart"></div>
</template>

<script>
import { Line } from '@antv/g2plot';

export default {
  props: {
  },
  data() {
    return {
      chart: '',
    }
  },
  methods: {
    init(uploadTraffic, downloadTraffic) {
      let chartData = this.getChartData(uploadTraffic, downloadTraffic)
      if (this.chart) {
        this.chart.changeData(chartData)
        return
      }
      const chartConfig = {
        data: chartData,
        xField: 'time',
        yField: 'value',
        seriesField: "type",
        padding: 'auto',
        xAxis: {
          range: [0, 1]
        },
        yAxis: {
          label: {
            formatter: (v) => v + '次',
          }
        },
        area: {
          style: {
            fillOpacity: 0.15
          }
        },
        smooth: true,
        color:['#409EFF',  '#67C23A'],
        tooltip: {
          formatter: (datum) => {
            return {
              name: datum.type + '请求数',
              value: datum.value + '次'
            }
          }
        }
      }
      const chart = new Line(this.$refs.chart, chartConfig)
      chart.render()
      this.chart = chart
    },
    getChartData(uploadTraffic, downloadTraffic) {
      let chartData = []
      for (const item of uploadTraffic) {
        chartData.push( 
          {
            time: this.$util.parseTime(item.record_time, '{m}/{d}'),
            value: item.data,
            type: "上传"
          }
        )
      }
      for (const item of downloadTraffic) {
        chartData.push( 
          {
            time: this.$util.parseTime(item.record_time, '{m}/{d}'),
            value: item.data,
            type: "下载" 
          }
        )
      }
      return chartData
    },
  }
}
</script>

<style scoped>
</style>