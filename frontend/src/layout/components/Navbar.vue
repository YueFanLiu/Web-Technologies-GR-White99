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
        v-model="searchKeyword"
        placeholder="Search events..."
        prefix-icon="Search"
        class="search-input"
        @keyup.enter="handleSearch"
        clearable
        @clear="handleSearch"
      />
    </div>

    <!-- 右侧菜单 -->
    <div class="right-menu">
      <!-- 通知图标 -->
      <div class="right-menu-item" @click="goNotifications">
        <el-badge :value="unreadNotifications" :hidden="unreadNotifications <= 0" class="notification-badge">
          <svg-icon icon-class="bell" class="icon-item" />
        </el-badge>
      </div>

      <!-- 消息/邮件图标（带小红点） -->
      <div class="right-menu-item" @click="goMessages">
        <el-badge :value="unreadMessages" :hidden="unreadMessages <= 0" class="message-badge">
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
              <el-dropdown-item>Profile</el-dropdown-item>
            </router-link>
            <el-dropdown-item divided command="logout">
              <span>Logout</span>
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
        router
      >
        <el-menu-item index="/product/mainEvent">Home</el-menu-item>
        <el-sub-menu index="/my-events">
          <template #title>My Events</template>
          <el-menu-item index="/my-events/joined">Joined Activities</el-menu-item>
          <el-menu-item v-if="canCreateActivity" index="/manager/manageEvent">Created Activities</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/notifications">
          <span>Notifications</span>
          <el-badge :value="unreadNotifications" :hidden="unreadNotifications <= 0" class="nav-badge" />
        </el-menu-item>
        <el-menu-item index="/messages">
          <span>Messages</span>
          <el-badge :value="unreadMessages" :hidden="unreadMessages <= 0" class="nav-badge" />
        </el-menu-item>
        <el-menu-item index="/friends">Friends</el-menu-item>
        <el-menu-item index="/user/profile">Profile</el-menu-item>
        <el-sub-menu index="/post">
          <template #title>Posts</template>
          <el-menu-item index="/post/createPost">Create Post</el-menu-item>
          <el-menu-item index="/manager/managePost">My Posts</el-menu-item>
        </el-sub-menu>
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
import { useRouter } from 'vue-router'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { canCreateActivityForUser } from '@/utils/accessControl'
import { getUnreadNotificationCount } from '@/api/notifications'
import { getChats } from '@/api/chat'


const router = useRouter()
const searchKeyword = ref('')

function handleSearch() {
  if (!searchKeyword.value.trim()) return
  router.push({
    path: '/product/mainEvent',
    query: { keyword: searchKeyword.value.trim() }
  })
}

const appStore = useAppStore()
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const canCreateActivity = computed(() => canCreateActivityForUser(userStore.userInfo))
const unreadNotifications = ref(0)
const unreadMessages = ref(0)
let unreadRefreshTimer = null

function normalizeCount(res) {
  return Number(res?.unreadCount ?? res?.count ?? res?.data?.unreadCount ?? res?.data?.count ?? 0)
}

function loadUnreadNotifications() {
  getUnreadNotificationCount()
    .then((res) => {
      unreadNotifications.value = normalizeCount(res)
    })
    .catch(() => {
      unreadNotifications.value = 0
    })
}

function loadUnreadMessages() {
  getChats()
    .then((res) => {
      const chats = Array.isArray(res) ? res : res?.data || res?.rows || res?.list || res?.content || []
      unreadMessages.value = chats.reduce((total, chat) => total + Number(chat.unreadCount || 0), 0)
    })
    .catch(() => {
      unreadMessages.value = 0
    })
}

function refreshUnreadCounters() {
  loadUnreadNotifications()
  loadUnreadMessages()
}

function goNotifications() {
  router.push('/notifications')
}

function goMessages() {
  router.push('/messages')
}

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
      location.href = '/login'
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

onMounted(() => {
  refreshUnreadCounters()
  unreadRefreshTimer = window.setInterval(refreshUnreadCounters, 30000)
  window.addEventListener('app:unread-refresh', refreshUnreadCounters)
})

onBeforeUnmount(() => {
  if (unreadRefreshTimer) {
    window.clearInterval(unreadRefreshTimer)
    unreadRefreshTimer = null
  }
  window.removeEventListener('app:unread-refresh', refreshUnreadCounters)
})
</script>

<style lang='scss' scoped>
.navbar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 40px;
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

.sub-nav {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;

  .sub-nav-menu {
    justify-content: center;
    border-bottom: none;
    background: transparent;
  }

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    color: #1d3264;
    font-weight: 600;
  }

  :deep(.el-menu-item.is-active),
  :deep(.el-sub-menu.is-active .el-sub-menu__title) {
    color: #409eff;
    border-bottom-color: #409eff;
  }
}
</style>
