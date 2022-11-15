<template>
  <div class="app-container"> 
    <el-card style="width: 60%;"> 
      <el-tabs v-model="activeTab"> 
        <el-tab-pane label="基本资料" name="userinfo"> 
            <userInfo :user="user" /> 
        </el-tab-pane> 
        <el-tab-pane label="修改密码" name="resetPwd"> 
            <resetPwd :username="user_name" /> 
        </el-tab-pane> 
      </el-tabs> 
    </el-card> 
  </div> 
</template> 
<script> 
import userInfo from "./UserInfo"; 
import resetPwd from "./ModifyPassword"; 
import { getInfo } from "@/api/user"; 
import { mapGetters } from 'vuex'
export default { 
    name: "Profile", 
    components: {
        userInfo, 
        resetPwd 
    }, 
    data() {
        return { 
            user: {},
            activeTab: "userinfo" 
        }; 
    }, 
    computed: {
        ...mapGetters([
        'user_name'
        ])
    },
    created() { 
        this.getUser(); 
    }, 
    methods: { 
        getUser() { 
            getInfo(this.user_name).then(
                response => { 
                    this.user = response.data
                }); 
    } 
   } 
} 
</script>