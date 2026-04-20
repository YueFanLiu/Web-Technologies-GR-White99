<template>
  <div class="navbar" :class="'nav' + settingsStore.navType">
    <!-- 左侧 Logo/平台名称 -->
    <div class="navbar-brand">
      <svg-icon icon-class="location" class="brand-icon" />
      <span class="brand-name">Accessible Events Platform</span>
    </div>

    <!-- 中间搜索框 -->
    <div class="navbar-search">
      <el-input
        placeholder="Search events..."
        prefix-icon="Search"
        class="search-input"
      />
    </div>

    <!-- 右侧菜单 -->
    <div class="right-menu">
      <!-- 通知图标 -->
      <div class="right-menu-item">
        <el-badge :value="unreadNotifications" class="notification-badge">
          <svg-icon icon-class="bell" class="icon-item" />
        </el-badge>
      </div>

      <!-- 消息/邮件图标（带小红点） -->
      <div class="right-menu-item">
        <el-badge :value="unreadMessages" class="message-badge">
          <svg-icon icon-class="message" class="icon-item" />
        </el-badge>
      </div>

      <!-- 用户头像下拉 -->
      <el-dropdown @command="handleCommand" class="avatar-container right-menu-item hover-effect" trigger="hover">
        <div class="avatar-wrapper">
          <img :src="userStore.avatar" class="user-avatar" />
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/user/profile">
              <el-dropdown-item>个人中心</el-dropdown-item>
            </router-link>
            <el-dropdown-item divided command="logout">
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    
  </div>

  <div class="sub-nav">
      <el-menu
        mode="horizontal"
        :default-active="$route.path"
        class="sub-nav-menu"
      >
        <el-menu-item index="/home">Home</el-menu-item>
        <el-sub-menu index="/my-events">
          <template #title>My Events</template>
          <el-menu-item index="/my-events/created">我创建的活动</el-menu-item>
          <el-menu-item index="/my-events/joined">我参与的活动</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/notifications">
          <span>Notifications</span>
          <el-badge :value="unreadNotifications" class="nav-badge" />
        </el-menu-item>
        <el-menu-item index="/profile">Profile</el-menu-item>
      </el-menu>
    </div>
</template>

<script setup>
import { ElMessageBox } from 'element-plus'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from '@/components/TopNav'
import TopBar from './TopBar'
import Logo from './Sidebar/Logo'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import HeaderSearch from '@/components/HeaderSearch'
import RuoYiGit from '@/components/RuoYi/Git'
import RuoYiDoc from '@/components/RuoYi/Doc'
import useAppStore from '@/store/modules/app'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'

const appStore = useAppStore()
const userStore = useUserStore()
const settingsStore = useSettingsStore()

function toggleSideBar() {
  appStore.toggleSideBar()
}

function handleCommand(command) {
  switch (command) {
    case "setLayout":
      setLayout()
      break
    case "logout":
      logout()
      break
    default:
      break
  }
}

function logout() {
  ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logOut().then(() => {
      location.href = '/index'
    })
  }).catch(() => { })
}

const emits = defineEmits(['setLayout'])
function setLayout() {
  emits('setLayout')
}

async function toggleTheme(event) {
  const x = event?.clientX || window.innerWidth / 2
  const y = event?.clientY || window.innerHeight / 2
  const wasDark = settingsStore.isDark

  const isReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches
  const isSupported = document.startViewTransition && !isReducedMotion

  if (!isSupported) {
    settingsStore.toggleTheme()
    return
  }

  try {
    const transition = document.startViewTransition(async () => {
      await new Promise((resolve) => setTimeout(resolve, 10))
      settingsStore.toggleTheme()
      await nextTick()
    })
    await transition.ready

    const endRadius = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y))
    const clipPath = [`circle(0px at ${x}px ${y}px)`, `circle(${endRadius}px at ${x}px ${y}px)`]
    document.documentElement.animate(
      {
        clipPath: !wasDark ? [...clipPath].reverse() : clipPath
      }, {
        duration: 650,
        easing: "cubic-bezier(0.4, 0, 0.2, 1)",
        fill: "forwards",
        pseudoElement: !wasDark ? "::view-transition-old(root)" : "::view-transition-new(root)"
      }
    )
    await transition.finished
  } catch (error) {
    console.warn("View transition failed, falling back to immediate toggle:", error)
    settingsStore.toggleTheme()
  }
}
</script>

<style lang='scss' scoped>
.navbar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  background: #f8f9ff;
  border-bottom: 1px solid #eee;

  &-brand {
    display: flex;
    align-items: center;
    gap: 8px;
    .brand-icon {
      font-size: 24px;
      color: #409eff;
    }
    .brand-name {
      font-size: 24px;
      font-weight: 600;
      color: #333;
    }
  }

  &-search {
    flex: 1;
    max-width: 800px;
    margin: 0 40px;
    .search-input {
      --el-input-height: 40px;
      border-radius: 20px;
    }
  }

  .right-menu {
    display: flex;
    align-items: center;
    gap: 20px;
    .right-menu-item {
      display: flex;
      align-items: center;
      .icon-item {
        font-size: 20px;
        color: #666;
      }
      .user-avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        object-fit: cover;
      }
    }
  }

  .sub-nav {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    .sub-nav-menu {
      background: transparent;
      border-bottom: none;
      .el-menu-item.is-active {
        border-bottom: 2px solid #409eff;
        color: #409eff;
      }
    }
  }
}
</style>
