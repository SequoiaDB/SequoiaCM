<template>
  <div class="chart" ref="chart" ></div>
</template>
<script>

import { Area } from '@antv/g2plot';

import {convertFileSize} from '@/utils/index'

export default {
  props: {
  },
  data() {
    return {
      chart: ''
    }
  },
  methods: {
    init(data) {
      let chartData = this.getChartData(data)
      if (this.chart) {
        this.chart.changeData(chartData)
        return
      }
      const chartConfig = {
        data: chartData,
        xField: 'time',
        yField: 'count',
        xAxis: {
          range: [0, 1],
          tickCount: this.tickCount
        },
        yAxis: {
          label: {
            formatter: (v) => convertFileSize(v),
          }
        },
        areaStyle: {
          fill: 'l(270) 0:#ffffff 0.5:#7ec2f3 1:#1890ff'
        },
        color:['#409EFF'],
        smooth: true,
        tooltip: {
          formatter: (datum) => {
            return {
              name: "新增文件大小",
              value: convertFileSize(datum.count)
            }
          }
        }
      }
      const area = new Area(this.$refs.chart, chartConfig)
      area.render()
      this.chart = area
    },
    getChartData(data) {
      let chartData = []
      for (const item of data) {
        chartData.push({
          time: this.$util.parseTime(item.record_time, '{m}/{d}'),
          count: item.data,
        })
      }
      return chartData
    }
  }
}
</script>

<style scoped>
</style>