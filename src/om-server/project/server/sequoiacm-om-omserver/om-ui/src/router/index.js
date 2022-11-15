import Vue from 'vue'
import Router from 'vue-router'
import { getUsername, isAdminUser } from '@/utils/auth'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'

/**
 * Note: sub-menu only appear when route children.length >= 1
 * Detail see: https://panjiachen.github.io/vue-element-admin-site/guide/essentials/router-and-nav.html
 *
 * hidden: true                   if set true, item will not show in the sidebar(default is false)
 * alwaysShow: true               if set true, will always show the root menu
 *                                if not set alwaysShow, when item has more than one children route,
 *                                it will becomes nested mode, otherwise not show the root menu
 * redirect: noRedirect           if set noRedirect will no redirect in the breadcrumb
 * name:'router-name'             the name is used by <keep-alive> (must set!!!)
 * meta : {
    roles: ['admin','editor']    control the page roles (you can set multiple roles)
    title: 'title'               the name show in sidebar and breadcrumb (recommend set)
    icon: 'svg-name'/'el-icon-x' the icon show in the sidebar
    breadcrumb: false            if set false, the item will hidden in breadcrumb(default is true)
    activeMenu: '/example/list'  if set path, the sidebar will highlight the path you set
  }
 */

/**
 * constantRoutes
 * a base page that does not have permission requirements
 * all roles can be accessed
 */
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },

  {
    path: '/404',
    component: () => import('@/views/404'),
    hidden: true
  },

  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',

  },

  {
    path: '/dashboard',
    component: Layout,
    name: 'Dashboard',
    redirect: '/dashboard/index',
    meta: { title: 'Dashboard', icon: 'el-icon-monitor' },
    children: [
      {
        path: 'index',
        name: 'Table',
        component: () => import('@/views/dashboard/index'),
        meta: { title: '系统监控', keepAlive: true }
      },
      {
        path: 'instance/:id',
        name: 'Instance',
        hidden: true,
        component: () => import('@/views/dashboard/instance'),
        meta: { title: '节点状态' }
      }
    ]
  },

  {
    path: '/workspace',
    component: Layout,
    name: 'Workspace',
    redirect: '/workspace/table',
    meta: { title: '工作区', icon: 'tree' },
    children: [
      {
        path: 'table',
        name: 'Table',
        component: () => import('@/views/workspace/index'),
        meta: { title: '工作区管理', icon: 'el-icon-collection', keepAlive:true }
      },
      {
        path: 'detail/:name',
        name: 'Detail',
        hidden: true,
        component: () => import('@/views/workspace/detail'),
        meta: { title: '工作区详情', icon: 'el-icon-collection' }
      }
    ]
  },

  {
    path: '/schedule',
    component: Layout,
    meta: { title: '调度任务', icon: 'el-icon-time' },
    redirect: '/schedule/table',
    children: [
      {
        path: 'table',
        name: 'Table',
        component: () => import('@/views/schedule/index'),
        meta: { title: '调度任务管理', icon: 'el-icon-time', keepAlive:true }
      },
      {
        path: 'tasks/:id',
        name: 'tasks',
        hidden: true,
        component: () => import('@/views/schedule/taskList'),
        meta: { title: '任务运行记录', icon: 'tree' }
      }
    ]
  },

  {
    path: '/file',
    component: Layout,
    meta: { title: '文件', icon: 'el-icon-document' },
    redirect: '/file/table',
    children: [
      {
        path: 'table',
        name: 'Table',
        component: () => import('@/views/file/index'),
        meta: { title: '文件管理', icon: 'el-icon-document', keepAlive:true }
      }
    ]
  },

  {
    path: '/object',
    component: Layout,
    meta: { title: '对象', icon: 'el-icon-takeaway-box' },
    redirect: '/object/table',
    children: [
      {
        path: 'table',
        name: 'Table',
        component: () => import('@/views/object/index'),
        meta: { title: '桶对象管理', icon: 'el-icon-takeaway-box', keepAlive:true }
      }
    ]
  },

  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/system/user/profile/index'),
        meta: { title: '个人中心', icon: 'el-icon-user' }
      }
    ]
  },

  {
    path: '/system',
    component: Layout,
    meta: { title: '系统管理', icon: 'el-icon-setting', roles: ['ROLE_AUTH_ADMIN'] },
    redirect: '/user/table',
    children: [
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/system/user/index'),
        meta: { title: '用户管理', icon: 'el-icon-user',  keepAlive:true }      // icon: 'el-icon-setting',
      }, 
      {
          path: 'role',
          name: 'Role',
          component: () => import('@/views/system/role/index'),
          meta: { title: '角色管理', icon: 'el-icon-s-custom', keepAlive:true }    // icon: 'el-icon-setting'
      }
    ]
  },

  // 404 page must be placed at the end !!!
  { path: '*', redirect: '/404', hidden: true }
]

const createRouter = () => new Router({
  // mode: 'history', // require service support
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()

// Detail see: https://github.com/vuejs/vue-router/issues/1234#issuecomment-357941465
export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher // reset router
}

export default router
