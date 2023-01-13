<template>
  <div>
    <el-form :model="form" ref="form" :rules="rules" size="mini" label-width="140px" label-position="left">
      <el-form-item label="桶生成规则">
        <el-radio-group v-model="form.bucketGenerateRule">
          <el-radio label="auto">自动生成</el-radio>
          <el-radio label="custom">手动指定</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="桶名" prop="bucketName" v-if="form.bucketGenerateRule === 'custom'">
        <el-input v-model="form.bucketName" maxlength="30" placeholder="请指定一个已经存在的桶作为桶名"></el-input>
      </el-form-item>
      <el-form-item label="桶名前缀" prop="bucketPrefix" v-if="form.bucketGenerateRule === 'auto'">
        <el-input v-model="form.bucketPrefix" maxlength="30" placeholder="留空使用默认前缀：[wsName]-scmfile"></el-input>
      </el-form-item>
      <el-form-item prop="bucketShardingType" v-if="form.bucketGenerateRule === 'auto'">
        <template slot="label">
          分桶规则
          <el-tooltip effect="dark" placement="bottom">
            <div slot="content">
              当前分桶规则下，文件内容将存储在 "{{form.bucketPrefix?form.bucketPrefix:'[wsName]-scmfile'}}{{this.$util.getShardingStr(form.bucketShardingType, '-')}}" 桶中
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
        </template>
        <scm-sharding-select v-model="form.bucketShardingType"/>
      </el-form-item>
      <el-form-item prop="objectShardingType">
        <template slot="label">
          桶内分目录规则
          <el-tooltip class="item" effect="dark" placement="bottom">
            <div slot="content">
              当前分目录规则下，生成的对象key示例为：{{this.$util.getShardingStr(form.objectShardingType)}}{{form.objectShardingType==='none'?'':'/'}}[objectId]
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
        </template>
        <scm-sharding-select v-model="form.objectShardingType" :dayEnabled="true"/>
      </el-form-item>
      <el-form-item label="使用独立s3用户">
         <template slot="label">
          使用独立s3用户
          <el-tooltip class="item" effect="dark" placement="bottom" content="待创建的工作区使用单独的 CephS3 用户">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </template>
        <el-switch
          v-model="useWorkspaceS3User">
        </el-switch>
      </el-form-item>
      <el-form-item :label="hasStandby ? '主库用户配置' : '用户配置'" v-if="useWorkspaceS3User">
        <el-form-item label="用户名" label-width="100px" prop="userInfo.primary.username" :rules="rules.username">
          <el-input v-model="form.userInfo.primary.username" maxlength="100" placeholder="用户名为 S3 用户的 AccessKey"></el-input>
        </el-form-item>
        <el-form-item label="密码文件" label-width="100px" prop="userInfo.primary.password" :rules="rules.password">
          <template slot="label">
           密码文件
           <el-tooltip class="item" effect="dark" placement="bottom">
              <div slot="content">
                {{passwordTip}}
              </div>
              <i class="el-icon-question"></i>
            </el-tooltip>
          </template>
          <el-input v-model="form.userInfo.primary.password" maxlength="100" placeholder="请输入密码文件路径"></el-input>
        </el-form-item>
      </el-form-item>
      <el-form-item label="备库用户配置" v-if="useWorkspaceS3User && hasStandby">
        <el-form-item label="用户名" label-width="100px" prop="userInfo.standby.username" :rules="rules.username">
          <el-input v-model="form.userInfo.standby.username" maxlength="100" placeholder="用户名为 S3 用户的 AccessKey"></el-input>
        </el-form-item>
        <el-form-item label="密码文件" label-width="100px" prop="userInfo.standby.password" :rules="rules.password">
          <template slot="label">
           密码文件
           <el-tooltip class="item" effect="dark" placement="bottom">
              <div slot="content">
                {{passwordTip}}
              </div>
              <i class="el-icon-question"></i>
            </el-tooltip>
          </template>
          <el-input v-model="form.userInfo.standby.password" maxlength="100" placeholder="请输入密码文件路径"></el-input>
        </el-form-item>
      </el-form-item>
    </el-form>

  </div>
</template>

<script>
import ScmShardingSelect from '@/components/selector/ScmShardingSelect.vue'

export default {
  components: {
    ScmShardingSelect
  },
  props: {
    siteName: {
      type: String,
      required: true
    },
    hasStandby: {
      type: Boolean,
      required: true
    }
  },
  data(){
    return {
      passwordTip: `请参考用户手册的运维指南章节，使用内容服务管理工具 scmadmin.sh encrypt 生成 S3 用户 SecretKey 的密码文件，并放置在站点：${this.siteName} 下节点所在的机器上`,
      useWorkspaceS3User: false,
      form: {
        bucketPrefix: '',
        bucketName: '',
        bucketGenerateRule: 'auto',
        bucketShardingType: 'month',
        objectShardingType: 'none',
        userInfo: {
          primary: {
            username: '',
            password: ''
          },
          standby: {
            username: '',
            password: ''
          }
        }
      },
      rules: {
        bucketName: {required: true, message: "请输入桶名", trigger: 'blur'},
        username: {required: true, message: "请输入用户名", trigger: 'blur'},
        password: {required: true, message: "请输入密码文件", trigger: 'blur'}
      }

    }
  },

  methods: {
    toFormData(){
      let formData = {
        site: this.siteName
      }
      if (this.form.bucketGenerateRule == 'custom') {
        formData['bucket_name'] = this.form.bucketName
      } else {
        formData['data_sharding_type'] = this.form.bucketShardingType
        if (this.form.bucketPrefix) {
          formData['container_prefix'] = this.form.bucketPrefix
        }
      }
      formData['object_sharding_type'] = this.form.objectShardingType
      if (this.useWorkspaceS3User) {
        formData['user_info'] = {
          primary: {
            user: this.form.userInfo.primary.username,
            password: this.form.userInfo.primary.password
          }
        }
        if (this.form.userInfo.standby.username) {
          formData['user_info']['standby'] = {
            user: this.form.userInfo.standby.username,
            password: this.form.userInfo.standby.password
          }
        }
      }
      return formData
    },

    validate() {
      let result = false
      this.$refs['form'].validate(valid => {
        result = valid
      })
      return result
    },

    clear() {
      this.form = {
        bucketPrefix: '',
        bucketName: '',
        bucketGenerateRule: 'auto',
        bucketShardingType: 'month',
        objectShardingType: 'none',
        userInfo: {
          primary: {
            username: '',
            password: ''
          },
          standby: {
            username: '',
            password: ''
          }
        }
      }
    }
  }

}
</script>

<style scoped>
</style>
