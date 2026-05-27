import { login, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isHttp } from '@/utils/validate'
import defAva from '@/assets/images/profile.jpg'
import { normalizeRole } from '@/utils/accessControl'

let getInfoPromise = null

function unwrapCurrentUser(res) {
  const data = res?.data ?? res
  return data?.user || data
}

function normalizePhotoUrl(photo) {
  if (!photo) return defAva
  return isHttp(photo) ? photo : import.meta.env.VITE_APP_BASE_API + photo
}

function normalizeUser(rawUser) {
  const role = normalizeRole(rawUser?.role || '')
  return {
    ...rawUser,
    id: rawUser?.id || '',
    email: rawUser?.email || '',
    fullName: rawUser?.fullName || '',
    phone: rawUser?.phone || '',
    photo: rawUser?.photo || '',
    avatar: rawUser?.photo || '',
    role,
    createdAt: rawUser?.createdAt || '',
    accessibilityPreferences: rawUser?.accessibilityPreferences || {}
  }
}

const useUserStore = defineStore(
  'user',
  {
    state: () => ({
      token: getToken(),
      userInfo: null,
      id: '',
      name: '',
      nickName: '',
      email: '',
      fullName: '',
      phone: '',
      photo: '',
      role: '',
      createdAt: '',
      accessibilityPreferences: {},
      avatar: '',
      roles: [],
      permissions: []
    }),
    actions: {
      login(userInfo) {
        return login({
          email: userInfo.email.trim(),
          password: userInfo.password
        }).then((res) => {
          setToken(res.accessToken)
          this.token = res.accessToken
          return this.getInfo()
        }).catch((error) => {
          this.logOut()
          return Promise.reject(error)
        })
      },
      getInfo() {
        if (getInfoPromise) {
          return getInfoPromise
        }

        getInfoPromise = getInfo().then((res) => {
          const user = normalizeUser(unwrapCurrentUser(res))

          if (!user.id) {
            return Promise.reject(new Error('Current user profile is missing id'))
          }

          const avatar = normalizePhotoUrl(user.photo)

          this.userInfo = user
          this.id = user.id
          this.name = user.fullName || user.email
          this.nickName = user.fullName
          this.email = user.email
          this.fullName = user.fullName
          this.phone = user.phone
          this.photo = user.photo
          this.role = user.role
          this.createdAt = user.createdAt
          this.accessibilityPreferences = user.accessibilityPreferences
          this.avatar = avatar
          this.roles = user.role ? [user.role] : []
          this.permissions = res?.permissions || user.permissions || []

          return user
        }).finally(() => {
          getInfoPromise = null
        })
      },
      logOut() {
        return new Promise((resolve) => {
          getInfoPromise = null
          this.token = ''
          this.userInfo = null
          this.roles = []
          this.permissions = []
          this.id = ''
          this.name = ''
          this.nickName = ''
          this.email = ''
          this.fullName = ''
          this.phone = ''
          this.photo = ''
          this.role = ''
          this.createdAt = ''
          this.accessibilityPreferences = {}
          this.avatar = ''
          removeToken()
          resolve()
        })
      }
    }
  })

export default useUserStore
