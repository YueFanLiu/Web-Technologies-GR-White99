import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken, setToken } from '@/utils/auth'
import { isPathMatch } from '@/utils/validate'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import { verifyEmail } from '@/api/login'
import { supabase } from '@/utils/supabase'
import { roleAllowed } from '@/utils/accessControl'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register', '/forgotPassword', '/confirmPassword']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

async function consumeSupabaseHashToken() {
  const hash = window.location.hash
  if (!hash || !hash.includes('access_token=')) {
    return
  }

  const params = new URLSearchParams(hash.slice(1))
  const accessToken = params.get('access_token')
  const refreshToken = params.get('refresh_token')
  if (accessToken) {
    setToken(accessToken)
  }
  if (accessToken && refreshToken) {
    await supabase.auth.setSession({
      access_token: accessToken,
      refresh_token: refreshToken
    })
  }

  window.history.replaceState(null, document.title, window.location.pathname + window.location.search)
}

async function consumeSupabaseTokenHash(to) {
  const tokenHash = to.query.token_hash
  if (!tokenHash) {
    return null
  }

  const res = await verifyEmail({
    tokenHash,
    type: to.query.type || 'signup'
  })

  if (res?.accessToken) {
    setToken(res.accessToken)
  }

  return {
    path: to.path,
    query: {},
    replace: true
  }
}

router.beforeEach(async (to, from, next) => {
  NProgress.start()

  try {
    await consumeSupabaseHashToken()
    const cleanRoute = await consumeSupabaseTokenHash(to)
    if (cleanRoute) {
      next(cleanRoute)
      return
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || 'Email verification failed')
    next('/login')
    return
  }

  to.meta.title && useSettingsStore().setTitle(to.meta.title)

  if (!getToken()) {
    if (isWhiteList(to.path) || to.meta.publicAccess) {
      next()
    } else {
      next('/login')
    }
    return
  }

  if (isWhiteList(to.path)) {
    next()
    return
  }

  const userStore = useUserStore()
  if (!userStore.userInfo) {
    try {
      await userStore.getInfo()
    } catch (error) {
      await userStore.logOut()
      ElMessage.error(error?.response?.data?.message || error?.message || 'Failed to load current user')
      next('/login')
      return
    }
  }

  if (to.meta.roles?.length && !roleAllowed(userStore.userInfo, to.meta.roles)) {
    next('/401')
    return
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})
