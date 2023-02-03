<template>
  <div>
    <!-- 新增/编辑 数据流对话框 -->
    <el-dialog
      :title=getTitleByOperation()
      class="edit-dialog"
      :visible.sync="editDialogVisible"
      width="700px">
      <el-form ref="form1" v-if="operation==='ws_add'" :rules="rules1" :model="form1" size="small" label-width="100px">
        <el-form-item label="数据流" prop="transition">
          <el-select
            v-model="form1.transition"
            size="small"
            placeholder="请选择数据流"
            filterable
            no-match-text=" "
            style="width:100%"
            @change="onTransitionChange">
            <el-option
              v-for="item in transitionList"
              :key="item"
              :label="item"
              :value="item">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="创建副本" prop="isCustom" v-if="operation==='ws_add' && form1.transition">
          <el-switch
            v-model="form1.isCustom"
            style="display: block; margin-top: 5px;"
            active-color="#13ce66"
            :active-value="true"
            :inactive-value="false">
          </el-switch>
        </el-form-item>
      </el-form>
      <el-form ref="form" :model="form" :rules="rules" size="small" label-width="100px" :disabled="operation==='ws_add' && !form1.isCustom" v-if="operation!=='ws_add' || form1.transition">
        <el-form-item label="数据流名称"  prop="name" v-if="operation!=='ws_add'">
          <el-input v-model="form.name" maxlength="30"  placeholder="请输入任务名称"></el-input>
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="起始阶段"  prop="source">
              <el-select
                id="select_transition_source"
                v-model="form.source"
                no-data-text="无数据"
                size="small"
                placeholder="请选择起始阶段"
                clearable filterable
                no-match-text=" "
                style="width:100%"
                @change="onStageTagChange">
                <el-option
                  v-for="item in residualStageTagList"
                  :key="item"
                  :label="item"
                  :value="item">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标阶段" prop="dest">
              <el-select
                id="select_transition_dest"
                v-model="form.dest"
                no-data-text="无数据"
                size="small"
                placeholder="请选择目标阶段"
                clearable filterable
                no-match-text=" "
                style="width:100%"
                @change="onStageTagChange">
                <el-option
                  v-for="item in residualStageTagList"
                  :key="item"
                  :label="item"
                  :value="item">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="流转配置"  prop="transitionConfig">
          <el-card class="box-card">
            <el-form-item label="设置迁移条件">
              <div style="margin-left: 200px;">
                符合以下
                <el-select
                  id="select_transition_triggers_mode"
                  v-model="form.transitionTriggers.mode"
                  size="mini"
                  style="width:75px">
                    <el-option
                      v-for="item in modeList"
                      :key="item"
                      :label="item"
                      :value="item">
                    </el-option>
                </el-select>
                条件
              </div>
            </el-form-item>
            <el-form-item >
              <el-form-item
                v-for="(trigger, index) in form.transitionTriggers.triggers"
                label=""
                :key="'规则'+trigger.id"
                prop="transitionTriggers.triggers">
                  <template>
                    <el-collapse>
                      <el-collapse-item>
                        <template slot="title">
                          规则 {{trigger.id}}<i class="header-icon el-icon-delete" @click="handleRemoveTriggers(trigger)" style="margin-left: 10px;"/>
                        </template>
                        <el-form-item label="设置迁移条件" label-width="120px">
                          <div style="margin-left: 180px;">
                            符合以下
                            <el-select
                              v-model="trigger.mode"
                              size="mini"
                              style="width:75px">
                                <el-option
                                  v-for="item in modeList"
                                  :key="item"
                                  :label="item"
                                  :value="item">
                                </el-option>
                            </el-select>
                            条件
                          </div>
                        </el-form-item>
                        <el-form-item label-width="130px" :prop="'transitionTriggers.triggers.' + index + '.create_time'" :rules="rules.trigger">
                          <template slot="label">
                            文件创建时间
                            <el-tooltip effect="dark" placement="top-start">
                              <div slot="content">文件的创建时间已超过指定天数</div>
                              <i class="el-icon-question"></i>
                            </el-tooltip>
                          </template>
                          <el-input v-number-only="{minValue:0}" maxlength="6" v-model="trigger.create_time" placeholder="单位: 天"></el-input>
                        </el-form-item>
                        <el-form-item label-width="130px" :prop="'transitionTriggers.triggers.' + index + '.last_access_time'" :rules="rules.trigger">
                          <template slot="label">
                            上次访问时间
                            <el-tooltip effect="dark" placement="top-start">
                              <div slot="content">最近一次通过起始阶段对应站点访问文件已超过指定天数</div>
                              <i class="el-icon-question"></i>
                            </el-tooltip>
                          </template>
                          <el-input v-number-only="{minValue:0}" maxlength="6" v-model="trigger.last_access_time" placeholder="单位: 天"></el-input>
                        </el-form-item>
                        <el-form-item label-width="130px" :prop="'transitionTriggers.triggers.' + index + '.build_time'" :rules="rules.trigger">
                          <template slot="label">
                            文件停留时间
                            <el-tooltip effect="dark" placement="top-start">
                              <div slot="content">文件上传至起始阶段对应站点的时间已超过指定天数</div>
                              <i class="el-icon-question"></i>
                            </el-tooltip>
                          </template>
                          <el-input v-number-only="{minValue:0}" maxlength="6" v-model="trigger.build_time" placeholder="单位: 天"></el-input>
                        </el-form-item>
                      </el-collapse-item>
                    </el-collapse>
                  </template>
              </el-form-item>
              <template>
                <el-button icon="el-icon-plus" size="mini" @click="addTransitionTrigger">添加</el-button>
              </template>
            </el-form-item>
            <el-form-item label="任务超时时长" label-width="120px" prop="transitionTriggers.max_exec_time" :rules="rules.max_exec_time">
              <el-input v-number-only="{minValue:0}" v-model.number="form.transitionTriggers.max_exec_time" maxlength="12" placeholder="单位：秒"></el-input>
            </el-form-item>
            <el-form-item label="cron" label-width="120px" prop="transitionTriggers.rule" :rules="rules.cron">
              <el-input v-model="form.transitionTriggers.rule" placeholder="请输入 Cron 表达式">
                <template #suffix>
                  <el-tooltip class="item" effect="dark" :content="'点击' + (showTransitionCronPicker ? '隐藏' : '显示') + 'cron快捷生成工具'" placement="top">
                    <i class="el-icon-question pointer-cursor no-select" @click="showTransitionCronPicker = !showTransitionCronPicker"></i>
                  </el-tooltip>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="cron快捷生成" label-width="120px" v-if="showTransitionCronPicker">
              <cron-picker ref="transitionCronPicker" :cron="form.transitionTriggers.rule" @change="onTransitionCronChange"/>
            </el-form-item>
          </el-card>
          <el-card class="box-card">
            <el-form-item label="延迟清理" label-width="70px" >
              <el-switch
                v-model="form.isDelayClean"
                style="display: block; margin-top: 5px;"
                active-color="#13ce66"
                :active-value="true"
                :inactive-value="false">
              </el-switch>
            </el-form-item>
            <el-form-item v-if="form.isDelayClean" label="设置清理条件">
              <div style="margin-left: 200px;">
                符合以下
                <el-select
                  id="select_clean_triggers_mode"
                  v-model="form.cleanTriggers.mode"
                  size="mini"
                  style="width:75px">
                    <el-option
                      v-for="item in triggersModes"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value">
                    </el-option>
                </el-select>
                条件
              </div>
            </el-form-item>
            <el-form-item v-if="form.isDelayClean">
              <el-form-item
                v-for="(trigger, index) in form.cleanTriggers.triggers"
                label=""
                :key="'规则 '+trigger.id">
                  <template>
                    <el-collapse>
                      <el-collapse-item>
                        <template slot="title">
                          规则{{trigger.id}}<i class="header-icon el-icon-delete"  @click="handleRemoveCleanTriggers(trigger)" style="margin-left: 10px;"/>
                        </template>
                        <el-form-item label="设置清理条件" label-width="120px" >
                          <div style="margin-left: 180px;">
                            符合以下
                            <el-select
                              v-model="trigger.mode"
                              size="mini"
                              style="width:75px">
                                <el-option
                                  v-for="item in modeList"
                                  :key="item"
                                  :label="item"
                                  :value="item">
                                </el-option>
                            </el-select>
                            条件
                          </div>
                        </el-form-item>
                        <el-form-item label-width="130px" :prop="'cleanTriggers.triggers.' + index + '.last_access_time'" :rules="rules.trigger">
                          <template slot="label">
                            上次访问时间
                            <el-tooltip effect="dark" placement="top-start">
                              <div slot="content">最近一次通过起始阶段对应站点访问文件已超过指定天数</div>
                              <i class="el-icon-question"></i>
                            </el-tooltip>
                          </template>
                          <el-input v-number-only="{minValue:0}" maxlength="6" v-model="trigger.last_access_time" placeholder="单位: 天"></el-input>
                        </el-form-item>
                        <el-form-item label-width="130px" :prop="'cleanTriggers.triggers.' + index + '.transition_time'" :rules="rules.trigger">
                          <template slot="label">
                            文件停留时间
                            <el-tooltip effect="dark" placement="top-start">
                              <div slot="content">文件迁移至目标阶段对应站点的时间已超过指定天数</div>
                              <i class="el-icon-question"></i>
                            </el-tooltip>
                          </template>
                          <el-input v-number-only="{minValue:0}" maxlength="6" v-model="trigger.transition_time" placeholder="单位: 天"></el-input>
                        </el-form-item>
                      </el-collapse-item>
                    </el-collapse>
                  </template>
              </el-form-item>
              <template>
                <el-button icon="el-icon-plus" size="mini" @click="addCleanTrigger">添加</el-button>
              </template>
            </el-form-item>
            <el-form-item label="任务超时时长" label-width="120px" prop="cleanTriggers.max_exec_time" v-if="form.isDelayClean" :rules="rules.max_exec_time">
              <el-input v-number-only="{minValue:0}" v-model.number="form.cleanTriggers.max_exec_time"  maxlength="12"   placeholder="单位：秒"></el-input>
            </el-form-item>
            <el-form-item label="cron" label-width="120px" prop="cleanTriggers.rule" v-if="form.isDelayClean" :rules="rules.cron">
              <el-input v-model="form.cleanTriggers.rule" maxlength="100"  placeholder="请输入Cron表达式">
                <template #suffix>
                  <el-tooltip class="item" effect="dark" :content="'点击' + (showCleanCronPicker? '隐藏' : '显示') + 'cron快捷生成工具'" placement="top">
                    <i class="el-icon-question pointer-cursor no-select" @click="showCleanCronPicker = !showCleanCronPicker"></i>
                  </el-tooltip>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="cron快捷生成" label-width="120px" v-if="showCleanCronPicker&&form.isDelayClean">
              <cron-picker ref="cleanCronPicker" :cron="form.cleanTriggers.rule" @change="onCleanCronChange"/>
            </el-form-item>
          </el-card>
        </el-form-item>
        <el-collapse v-model="transitionConf">
          <el-collapse-item name="2">
            <template slot="title">
              <div class="title">扩展配置</div>
            </template>
            <el-form-item label="文件范围"  prop="scope">
              <el-select id="input_transition_scope" v-model="form.scope" size="small" style="width:100%">
                <el-option
                  v-for="item in fileScopeTypes"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="查询条件" prop="matcher">
              <el-input
                id="input_transition_matcher"
                v-model="form.matcher"
                type="textarea"
                :rows="4"
                placeholder='指定文件的查询匹配条件，例如：{"id":"63ad46af4000010087460ffe"}'
                maxlength="1000"
                show-word-limit>
              </el-input>
              <el-button type="text" @click="jumpToFile">查询结果预览</el-button>
            </el-form-item>
            <el-row>
              <el-col :span="12">
                <el-form-item label="快速启动" prop="quickStart">
                  <el-select
                    v-model="form.quickStart"
                    size="small"
                    style="width:100%">
                    <el-option
                      v-for="item in booleanType"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value">
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="数据校验级别" prop="dataCheckLevel">
                  <el-select
                    v-model="form.dataCheckLevel"
                    size="small"
                    style="width:100%">
                    <el-option
                      v-for="item in checkLevels"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value">
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="是否回收空间" prop="isRecycleSpace">
              <el-select
                v-model="form.recycleSpace"
                size="small"
                style="width:100%">
                <el-option
                  v-for="item in booleanType"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="close" size="mini">关 闭</el-button>
        <el-button @click="submit" type="primary" :loading="saveButtonLoading" size="mini">保 存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import CronPicker from '@/components/common/CronPicker/index.vue'
import { listStageTag, createTransition, updateTransition, listTransition, addWsTransition, updateWsTransition } from "@/api/lifecycle"
import { FILE_SCOPE_STR_TYPES, TRIGGER_MODES } from '@/utils/common-define'
import numberOnly from '@/directives/numberOnly'

export default {
  components: {
    CronPicker
  },
  directives: {
    numberOnly
  },
  data() {
    return {
      operation: 'create',
      workspace: '',
      booleanType: [
        { value: true, label: '是' },
        { value: false, label: '否' },
      ],
      checkLevels: [
        { value: 'strict', label: '强校验'},
        { value: 'week', label: '弱校验'}
      ],
      modeList: [
        '所有',
        '任一'
      ],
      globalConf: [],
      showTransitionCronPicker: false,
      showCleanCronPicker: false,
      form1: {
        transition: '',
        isCustom: false,
      },
      form: {
        oldTransitionName: '',
        name: '',
        source: '',
        dest: '',
        transitionTriggers: {
          mode: '所有',
          max_exec_time: '',
          rule: '',
          triggers: [{
            "id" : "1", "mode" : '任一', "create_time" : '', "last_access_time" : '', "build_time" : ''
          }],
        },
        isDelayClean: false,
        cleanTriggers: {
          mode: '所有',
          max_exec_time: '',
          rule: '',
          triggers: [{
            "id" : "1", "mode" : '任一', "last_access_time" : '', "transition_time" : ''
          }],
        },
        quickStart: false,
        dataCheckLevel: 'strict',
        recycleSpace: true,
        scope: 'ALL',
        matcher: ''
      },
      rules1: {
        transition: [
          { required: true, message: '数据流不能为空', trigger: 'change' }
        ],
      },
      rules: {
        name: [
          { required: true, message: '请输入数据流名称', trigger: 'change' }
        ],
        source: [
          { required: true, message: '起始阶段不能为空', trigger: 'none' }
        ],
        dest: [
          { required: true, message: '目标阶段不能为空', trigger: 'none' }
        ],
        max_exec_time: [
          { required: true, message: '任务超时时长不能为空', trigger: 'none' }
        ],
        cron: [
          { required: true, message: 'cron 不能为空', trigger: 'none' }
        ],
        trigger: [
          { required: true, message: '该项不能为空', trigger: 'none' }
        ]
      },
      fileScopeTypes: FILE_SCOPE_STR_TYPES,
      triggersModes: TRIGGER_MODES,
      transitionConf: [],
      editDialogVisible: false,
      saveButtonLoading: false,
      stageTagList:[],
      residualStageTagList:[],
      transitionList:[],
      effectWsList: []
    }
  },
  methods: {
    getTitleByOperation() {
      if (this.operation === 'create') {
        return '创建数据流'
      }
      return this.operation === 'ws_add' ? '添加数据流' : '编辑数据流'
    },
    // 数据流选择有变化
    onTransitionChange() {
      let filter = {}
      filter['name_matcher'] = this.form1.transition
      listTransition(filter, 1, -1).then(res => {
        let transitionList = res.data
        let transition = transitionList[0]
        this.resetForm(transition)
      })
    },
    // 获取阶段标签列表
    initStageTagList() {
      this.stageTagList = []
      listStageTag(null, 1, -1).then(res => {
        let stageTagList = res.data
        if (stageTagList && stageTagList.length > 0) {
          for (let stageTag of stageTagList) {
            this.stageTagList.push(stageTag.name)
          }
        }
        this.refreshResidualTagList()
      })
    },
    // 获取数据流列表
    initTransitionList() {
      this.transitionList = []
      listTransition(null, 1, -1).then(res => {
        let transitionList = res.data
        if (transitionList && transitionList.length > 0) {
          for (let transition of transitionList) {
            this.transitionList.push(transition.name)
          }
        }
      })
    },
    // 刷新剩余可选的阶段标签列表
    refreshResidualTagList() {
      let residualStageTagList = []
      for (let stageTag of this.stageTagList) {
        if (stageTag !== this.form.source && stageTag !== this.form.dest) {
          residualStageTagList.push(stageTag)
        }
      }
      this.residualStageTagList = residualStageTagList
    },
    // 修改起始/目标阶段标签
    onStageTagChange() {
      this.refreshResidualTagList()
    },
    // 迁移配置，cron 改变
    onTransitionCronChange(res) {
      this.form.transitionTriggers.rule = res.cron
    },
    // 清理配置，cron 改变
    onCleanCronChange(res) {
      this.form.cleanTriggers.rule = res.cron
    },
    // 提交表单
    submit() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          let transition = {}
          if (this.form1.isCustom) {
            transition['customized'] = true
          }
          transition['name'] = this.form.name
          transition['source'] = this.form.source
          transition['dest'] = this.form.dest
          transition['matcher'] = this.form.matcher
          let transitionTriggers = {}
          transitionTriggers['mode'] = this.form.transitionTriggers.mode === '所有' ? 'ALL' : 'ANY'
          transitionTriggers['rule'] = this.form.transitionTriggers.rule
          transitionTriggers['max_exec_time'] = this.form.transitionTriggers.max_exec_time
          let triggers = []
          for (const trigger of this.form.transitionTriggers.triggers) {
            let curTrigger = {}
            curTrigger['id'] = trigger['id']
            curTrigger['mode'] = trigger['mode'] === '所有' ? 'ALL' : 'ANY'
            curTrigger['build_time'] = trigger['build_time'] + 'd'
            curTrigger['create_time'] = trigger['create_time'] + 'd'
            curTrigger['last_access_time'] = trigger['last_access_time'] + 'd'
            triggers.push(curTrigger)
          }
          transitionTriggers['triggers'] = triggers
          transition['transition_triggers'] = transitionTriggers
          
          transition['scope'] = this.form.scope
          transition['data_check_level'] = this.form.dataCheckLevel
          transition['is_recycle_space'] = this.form.recycleSpace
          transition['is_quick_start'] = this.form.quickStart

          // 若打开延迟清理，则补充相关配置
          if (this.form.isDelayClean) {
            let cleanTriggers = {}
            cleanTriggers['mode'] = this.form.cleanTriggers.mode === '所有' ? 'ALL' : 'ANY'
            cleanTriggers['rule'] = this.form.cleanTriggers.rule
            cleanTriggers['max_exec_time'] = this.form.cleanTriggers.max_exec_time
            let triggers = []
            for (const trigger of this.form.cleanTriggers.triggers) {
              let curTrigger = {}
              curTrigger['id'] = trigger['id']
              curTrigger['mode'] = trigger['mode'] === '所有' ? 'ALL' : 'ANY'
              curTrigger['transition_time'] = trigger['transition_time'] + 'd'
              curTrigger['last_access_time'] = trigger['last_access_time'] + 'd'
              triggers.push(curTrigger)
            }
            cleanTriggers['triggers'] = triggers
            transition['clean_triggers'] = cleanTriggers
          }

          this.saveButtonLoading = true
          if (this.operation === 'create') {
            createTransition(transition).then(res => {
              this.close()
              this.$message.success("数据流创建成功")
              this.$emit('onTransitionEdited')
            }).catch(() => {
              this.saveButtonLoading = false
            })
          } else if (this.operation === 'update') {
            updateTransition(this.form.oldTransitionName, transition).then(res => {
              this.close()
              let successMsg = "数据流修改成功"
              if (this.effectWsList != null && this.effectWsList.length > 0) {
                successMsg = successMsg + "，将影响如下工作区: 【"
                let counter = 0;
                this.effectWsList.forEach(ele => {
                  if (++counter != 1) {
                    successMsg = successMsg + ", "
                  }
                  successMsg = successMsg + ele
                });
                successMsg = successMsg + " 】"
              }
              this.$message.success(successMsg)
              this.$emit('onTransitionEdited')
            }).catch( () => {
              this.saveButtonLoading = false
            })
          } else if (this.operation === 'ws_add') {
            addWsTransition(this.workspace, transition).then(res => {
              this.close()
              this.$message.success("数据流添加成功")
              this.$emit('onTransitionEdited')
            }).catch(() => {
              this.saveButtonLoading = false
            })
          } else if (this.operation === 'ws_update') {
            updateWsTransition(this.workspace, this.form.oldTransitionName, transition).then(res => {
              this.close()
              this.$message.success("数据流修改成功，本次修改仅应用于当前工作区")
              this.$emit('onTransitionEdited')
            }).catch(() => {
              this.saveButtonLoading = false
            })
          }
          this.saveButtonLoading = false
        }
      })
    },
    // 移除迁移触发规则
    handleRemoveTriggers(trigger) {
      if (this.form.transitionTriggers.triggers.length <= 1) {
        this.$message.error("至少需要保留一条规则")
        return;
      }
      let index = this.form.transitionTriggers.triggers.indexOf(trigger)
      if (index !== -1) {
        this.form.transitionTriggers.triggers.splice(index, 1)
      }
    },
    // 动态添加迁移触发规则
    addTransitionTrigger() {
      let triggerId = this.generateTriggerId(this.form.transitionTriggers.triggers)
      let trigger = { id: triggerId, mode: "任一" }
      this.form.transitionTriggers.triggers.push(trigger)
    },
    // 移除清理触发规则
    handleRemoveCleanTriggers(trigger) {
      if (this.form.cleanTriggers.triggers.length <= 1) {
        this.$message.error("至少需要保留一条规则")
        return;
      }
      let index = this.form.cleanTriggers.triggers.indexOf(trigger)
      if (index !== -1) {
        this.form.cleanTriggers.triggers.splice(index, 1)
      }
    },
    // 动态添加清理触发规则
    addCleanTrigger() {
      let triggerId = this.generateTriggerId(this.form.cleanTriggers.triggers)
      let trigger = { id: triggerId, mode: "任一" }
      this.form.cleanTriggers.triggers.push(trigger)
    },
    // 获取规则 id
    generateTriggerId(triggers) {
      let max_id = 1
      triggers.forEach(ele => {
        let curId = parseInt(ele.id)
        if (max_id < curId) {
          max_id = curId
        }
      })
      return max_id + 1 + ''
    },
    // 显示数据创建/编辑对话框
    show(operation, data) {
      this.show(operation, data, null)
    },
    show(operation, data, ws, effectWsList) {
      this.operation = operation
      this.workspace = ws
      this.effectWsList = effectWsList
      if (this.operation === 'create') {
        this.clearForm()
      } else if (this.operation === 'update') {
        let transition = JSON.parse(JSON.stringify(data))
        this.resetForm(transition)
      } else if (this.operation === 'ws_add') {
        this.initTransitionList()
      } else if (this.operation === 'ws_update') {
        let transition = JSON.parse(JSON.stringify(data))
        this.resetForm(transition)
      }
      this.initStageTagList()
      setTimeout(()=>{this.editDialogVisible = true})
    },
    // 跳转到文件列表页面（新页面）
    jumpToFile() {
      const { href } = this.$router.resolve({
        path: "/file",
        query: {
          target: "checkJson",
          jsonStr: this.form.matcher
        }
      })
      window.open(href, "_blank")
    },
    // 关闭数据创建/编辑对话框
    close() {
      this.clear()
      this.editDialogVisible = false
    },
    clear() {
      setTimeout(()=>{
        // 重置表单
        this.clearForm()
      }, 500)
    },
    // 基于已有数据流信息填充表单
    resetForm(t) {
      this.form['oldTransitionName'] = t.name
      this.form['name'] = t.name
      this.form['source'] = t.source
      this.form['dest'] = t.dest
      this.form['quickStart'] = t.is_quick_start
      this.form['dataCheckLevel'] = t.data_check_level
      this.form['recycleSpace'] = t.is_recycle_space
      this.form['scope'] = t.scope
      this.form['matcher'] = t.matcher
      let transitionTriggers = {}
      transitionTriggers['mode'] = t.transition_triggers.mode === 'ALL' ? '所有' : '任一'
      transitionTriggers['rule'] = t.transition_triggers.rule
      transitionTriggers['max_exec_time'] = t.transition_triggers.max_exec_time / 1000
      let triggers = []
      for (const trigger of t.transition_triggers.triggers) {
        let curTrigger = {}
        curTrigger['id'] = trigger['id']
        curTrigger['mode'] = trigger['mode'] === 'ALL' ? '所有' : '任一'
        curTrigger['build_time'] = Number(trigger['build_time'].slice(0, trigger['build_time'].length - 1))
        curTrigger['create_time'] = Number(trigger['create_time'].slice(0, trigger['create_time'].length - 1))
        curTrigger['last_access_time'] = Number(trigger['last_access_time'].slice(0, trigger['last_access_time'].length - 1))
        triggers.push(curTrigger)
      }
      transitionTriggers['triggers'] = triggers
      this.form['transitionTriggers'] = transitionTriggers

      if (t.clean_triggers) {
        this.form['isDelayClean'] = true
        let cleanTriggers = {}
        cleanTriggers['mode'] = t.clean_triggers.mode === 'ALL' ? '所有' : '任一'
        cleanTriggers['rule'] = t.clean_triggers.rule
        cleanTriggers['max_exec_time'] = t.clean_triggers.max_exec_time / 1000
        let triggers = []
        for (const trigger of t.clean_triggers.triggers) {
          let curTrigger = {}
          curTrigger['id'] = trigger['id']
          curTrigger['mode'] = trigger['mode'] === 'ALL' ? '所有' : '任一'
          curTrigger['transition_time'] = Number(trigger['transition_time'].slice(0, trigger['transition_time'].length - 1))
          curTrigger['last_access_time'] = Number(trigger['last_access_time'].slice(0, trigger['last_access_time'].length - 1))
          triggers.push(curTrigger)
        }
        cleanTriggers['triggers'] = triggers
        this.form['cleanTriggers'] = cleanTriggers
      }
    },
    // 重置表单
    clearForm() {
      if (this.$refs['form']) {
        this.$refs['form'].resetFields()
      }
      if (this.$refs['form1']) {
        this.$refs['form1'].resetFields()
      }
      if (this.$refs['transitionCronPicker']) {
        this.$refs['transitionCronPicker'].reset()
      }
      if (this.$refs['cleanCronPicker']) {
        this.$refs['cleanCronPicker'].reset()
      }
      this. form1 = {
        transition: '',
        isCustom: false,
      },
      this.form = {
        oldTransitionName: '',
        name: '',
        source: '',
        dest: '',
        transitionTriggers: {
          mode: '所有',
          max_exec_time: '',
          rule: '',
          triggers: [{
            "id" : "1", "mode" : '任一'
          }],
        },
        isDelayClean: false,
        cleanTriggers: {
          mode: '所有',
          max_exec_time: '',
          rule: '',
          triggers: [{
            "id" : "1", "mode" : '任一'
          }],
        },
        quickStart: false,
        dataCheckLevel: 'strict',
        recycleSpace: true,
        scope: 'ALL',
        matcher: ''
      }
    }
  }
}
</script>
<style  scoped>
.title {
  font-size: 16px;
  height: 20px;
  line-height: 20px;
  margin-bottom: 10px;
  font-weight: 600;
  text-indent: 3px;
}
.edit-dialog >>> .el-dialog__body {
  max-height: 700px;
  overflow-y: auto;
  padding: 25px 25px;
}
.scm-attribute >>> .el-form-item__label {
  color: rgb(97, 161, 161);
  font-size: 10px;
  width: 50px !important;
}
</style>
