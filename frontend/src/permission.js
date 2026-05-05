import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken, setToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import { verifyEmail } from '@/api/login'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register','forgotPassword']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

function consumeSupabaseHashToken() {
  const hash = window.location.hash
  if (!hash || !hash.includes('access_token=')) {
    return
  }

  const params = new URLSearchParams(hash.slice(1))
  const accessToken = params.get('access_token')
  if (accessToken) {
    setToken(accessToken)
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
  consumeSupabaseHashToken()
  try {
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

  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    //if (to.path === '/login') {
      //next({ path: '/product/mainEvent' })
     // NProgress.done()}
    if (isWhiteList(to.path)) {
      next()
    } else {
      /*if (useUserStore().roles.length === 0) {
        isRelogin.show = true
        // 判断当前用户是否已拉取完user_info信息
        useUserStore().getInfo().then(() => {
          isRelogin.show = false
          usePermissionStore().generateRoutes().then(accessRoutes => {
            // 根据roles权限生成可访问的路由表
            accessRoutes.forEach(route => {
              if (!isHttp(route.path)) {
                router.addRoute(route) // 动态添加可访问路由表
              }
            })
            next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
          })
        }).catch(err => {
          useUserStore().logOut().then(() => {
            ElMessage.error(err)
            next({ path: '/product/mainEvent' })
          })
        })
      } else {
        next()
      }*/
      next()
    }
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      next()
    } else {
      next(`/login`) // 否则全部重定向到登录页
      
    }
  }
   /*if (to.meta.title) {
    useSettingsStore().setTitle(to.meta.title)
  }
  
  // ✅ 直接允许所有访问，不检查token
  next()
  NProgress.done()*/
})

router.afterEach(() => {
  NProgress.done()
})
