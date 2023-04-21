<template>
  <div class="app-container">
    <panel title="对象数增量" class="panel-item">
      <div slot="title-right" class="time-selector">
        <el-date-picker
          v-model="objectCountDeltaQueryRange"
          type="daterange"
          align="right"
          unlink-panels
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :picker-options="pickerOptions">
        </el-date-picker>
        <el-button type="primary" size="small" icon="el-icon-search" class="btn-query" @click="handleQueryFileCountDeltaBtnClick">查询</el-button>
      </div>
      <object-count-delta-chart ref="objectCountDeltaChart"></object-count-delta-chart>
    </panel>
    <panel title="对象大小增量" class="panel-item">
      <div slot="title-right" class="time-selector">
        <el-date-picker
          v-model="objectSizeDeltaQueryRange"
          type="daterange"
          align="right"
          unlink-panels
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :picker-options="pickerOptions">
        </el-date-picker>
        <el-button type="primary" size="small" icon="el-icon-search" class="btn-query"  @click="handleQueryObjectSizeDeltaBtnClick">查询</el-button>
      </div>
      <object-size-delta-chart ref="objectSizeDeltaChart"></object-size-delta-chart>
    </panel>

  </div>
</template>

<script>
import ObjectCountDeltaChart from '@/views/bucket/components/ObjectCountDeltaChart'
import ObjectSizeDeltaChart from '@/views/bucket/components/ObjectSizeDeltaChart'
import Panel from '@/components/Panel'

import {queryBucketObjectDelta} from '@/api/bucket'

export default {
  components: {
    ObjectCountDeltaChart,
    ObjectSizeDeltaChart,
    Panel
  },
  data(){
    return {
      pickerOptions: {
        shortcuts: [{
          text: '最近一周',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
            picker.$emit('pick', [start, end]);
          }
        }, {
          text: '最近一个月',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
            picker.$emit('pick', [start, end]);
          }
        }, {
          text: '最近三个月',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 90);
            picker.$emit('pick', [start, end]);
          }
        }]
      },
      bucketName: this.$route.params.name,
      objectSizeDeltaQueryRange: '',
      objectSizeDeltaQueryCondition: {
        beginTime: null,
        endTIme: null
      },
      objectCountDeltaQueryRange: '',
      objectCountDeltaQueryCondition: {
        beginTime: null,
        endTIme: null
      },
      objectSizeDeltaChartData: null,
      objectCountDeltaChartData: null,
    }
  },
  methods: {
    init(){
      this.refreshChart()
    },

     // 刷新图表数据
    async refreshChart() {
      this.queryObjectCountDeltaChart()
      this.queryObjectSizeDeltaChart()
    },

      // 点击文件大小增量查询按钮
    handleQueryObjectSizeDeltaBtnClick() {
      this.objectSizeDeltaQueryCondition = {
        beginTime: this.objectSizeDeltaQueryRange ? this.objectSizeDeltaQueryRange[0].getTime() : null,
        endTime: this.objectSizeDeltaQueryRange ? this.$util.getMaxTimestampForDay(this.objectSizeDeltaQueryRange[1].getTime()) : null
      }
      this.queryObjectSizeDeltaChart()
    },

    // 点击文件数量增量查询按钮
    handleQueryFileCountDeltaBtnClick() {
      this.objectCountDeltaQueryCondition = {
        beginTime: this.objectCountDeltaQueryRange ? this.objectCountDeltaQueryRange[0].getTime() : null,
        endTime: this.objectCountDeltaQueryRange ? this.$util.getMaxTimestampForDay(this.objectCountDeltaQueryRange[1].getTime()) : null
      }
      this.queryObjectCountDeltaChart()
    },

    async queryObjectSizeDeltaChart() {
      let res = await queryBucketObjectDelta(this.bucketName, this.objectSizeDeltaQueryCondition.beginTime, this.objectSizeDeltaQueryCondition.endTime)
      this.$refs['objectSizeDeltaChart'].init(res.data['size_delta'])
    },

    async queryObjectCountDeltaChart() {
      let res = await queryBucketObjectDelta(this.bucketName, this.objectCountDeltaQueryCondition.beginTime, this.objectCountDeltaQueryCondition.endTime)
      this.$refs['objectCountDeltaChart'].init(res.data['count_delta'])
    }
  },

  created(){
    this.init()
  }

}
</script>
