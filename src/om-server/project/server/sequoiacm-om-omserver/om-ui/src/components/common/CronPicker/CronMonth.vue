<template>
  <div>
    <div>
      <div>
        每月
        <el-select id="input_cronPicker_days" v-model="days" multiple size="mini" style="width: 200px" @change="emitChange()">
          <el-option
            v-for="item in dayOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
        日，运行一次
      </div>
    </div>
  </div>
</template>

<script>

export default {
  name: 'CronMonth',
  data() {
    return {
      days: [1],
    }
  },
  computed: {
    dayOptions() {
      return Array.from(Array(31), (_, i) => i + 1)
    },
    cronExp() {
      if (this.days.length === 0) {
        return `0 0 0 * * ?`
      }
      return `0 0 0 ${this.days.join(',')} * ?`
    }
  },
  methods: {
    init(value) {
      const tempArr = value.split(' ')
      if(tempArr[3] === '*'){
        this.days = []
      }else{
        const dayArr = tempArr[3].split(',')
        this.days = dayArr.filter(v => v !== '').map(v => Number(v))
      }
    },
    emitChange() {
      this.$emit('change', this.cronExp)
    }
  }
}
</script>

<style scoped>

</style>
