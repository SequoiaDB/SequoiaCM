<template>
  <el-select v-model="selectedOptions" placeholder="分区类型">
    <el-option label="none" v-if="noneEnabled" value="none"></el-option>
    <el-option v-if="dayEnabled" label="day" value="day"></el-option>
    <el-option label="month" value="month"></el-option>
    <el-option label="year" value="year"></el-option>
    <el-option label="quarter" value="quarter"></el-option>
  </el-select>
</template>

<script>
export default {
  props: {
    value: {
      type: String
    },
    dayEnabled: {
      type: Boolean,
      default: false
    },
    noneEnabled: {
      type: Boolean,
      default: true
    }
  },
  data(){
    return {
      selectedOptions: this.value
    }
  },
  watch:{
    value(newVal){
      this.selectedOptions = newVal
    },
    selectedOptions (newVal, oldVal){
      this.$emit("input", this.selectedOptions )
    }
  }
}
</script>