<template>
  <div class="profile-page">
    <main class="profile-shell">
      <section class="page-heading">
        <div>
          <h1>Profile</h1>
          <p>Manage your personal information.</p>
        </div>
      </section>

      <section class="profile-grid" v-loading="loading">
        <article class="profile-card summary-card">
          <h2>Profile Summary</h2>

          <div class="avatar-frame">
            <img :src="profileForm.avatar" alt="User avatar" class="profile-avatar" @error="handleAvatarError" />
          </div>
          <el-upload
            action="#"
            :http-request="handleAvatarUpload"
            :show-file-list="false"
            :disabled="!isEditing || uploadingAvatar"
            accept="image/*"
          >
            <el-button size="small" :disabled="!isEditing || uploadingAvatar" :loading="uploadingAvatar">
              <el-icon><Upload /></el-icon>
              Upload Avatar
            </el-button>
          </el-upload>

          <h3>{{ profileForm.fullName }}</h3>
          <span class="role-pill">
            <el-icon><User /></el-icon>
            {{ profileForm.role }}
          </span>

          <div class="summary-list">
            <span>
              <el-icon><Message /></el-icon>
              {{ profileForm.email }}
            </span>
            <span>
              <el-icon><Calendar /></el-icon>
              {{ profileForm.memberSince }}
            </span>
          </div>
        </article>

        <article class="profile-card info-card">
          <h2>Personal Information</h2>

          <el-form label-position="top" class="profile-form">
            <div class="form-grid">
              <el-form-item label="Full Name">
                <el-input v-model="profileForm.fullName" :disabled="!isEditing" />
              </el-form-item>
              <el-form-item label="Email">
                <el-input v-model="profileForm.email" disabled />
              </el-form-item>
              <el-form-item label="Phone Number">
                <el-input v-model="profileForm.phone" :disabled="!isEditing">
                  <template #prefix>
                    <el-icon><Phone /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="Role">
                <el-input v-model="profileForm.role" disabled />
              </el-form-item>
            </div>
          </el-form>
        </article>
      </section>

      <section class="profile-card accessibility-card">
        <h2>Accessibility Preferences</h2>

        <div class="preference-grid">
          <label
            v-for="preference in accessibilityPreferences"
            :key="preference.key"
            class="preference-item"
          >
            <span class="preference-icon">{{ preference.icon }}</span>
            <span class="preference-copy">
              <strong>{{ preference.label }}</strong>
              <small>{{ preference.description }}</small>
            </span>
            <el-switch
              v-model="profileForm.preferences[preference.key]"
              :disabled="!isEditing"
              active-text="Yes"
              inactive-text="No"
              inline-prompt
            />
          </label>
        </div>
      </section>

      <section class="profile-card action-card">
        <el-button class="profile-action" @click="enableEditing">
          <el-icon><EditPen /></el-icon>
          Edit Profile
        </el-button>
        <el-button
          type="primary"
          class="profile-action primary-action"
          :disabled="!isEditing"
          @click="saveChanges"
        >
          <el-icon><Check /></el-icon>
          Save Changes
        </el-button>
        <el-button class="profile-action logout-action" @click="logout">
          <el-icon><SwitchButton /></el-icon>
          Logout
        </el-button>
      </section>
    </main>
  </div>
</template>

<script setup name="Profile">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Check, EditPen, Message, Phone, SwitchButton, Upload, User } from '@element-plus/icons-vue'
import { getCurrentUserProfile, updateCurrentUserProfile, uploadCurrentUserAvatar } from '@/api/profile'
import useUserStore from '@/store/modules/user'
import { normalizeRole } from '@/utils/accessControl'
import defaultAvatar from '@/assets/images/profile.jpg'

const userStore = useUserStore()
const loading = ref(false)
const isEditing = ref(false)
const uploadingAvatar = ref(false)

const profileForm = reactive({
  avatar: defaultAvatar,
  fullName: '',
  email: '',
  phone: '',
  role: '',
  memberSince: 'Member since unavailable',
  preferences: {
    wheelchair: false,
    elevator: false,
    restroom: false,
    quiet: false,
    stepFree: false
  }
})

const accessibilityPreferences = [
  {
    key: 'wheelchair',
    icon: 'WC',
    label: 'Wheelchair Accessible',
    description: 'I use a wheelchair or need wheelchair access'
  },
  {
    key: 'elevator',
    icon: 'EL',
    label: 'Elevator Needed',
    description: 'I need elevator access'
  },
  {
    key: 'restroom',
    icon: 'AR',
    label: 'Accessible Restroom',
    description: 'I require an accessible restroom'
  },
  {
    key: 'quiet',
    icon: 'QL',
    label: 'Quiet / Low Noise',
    description: 'I prefer quiet or low noise environments'
  },
  {
    key: 'stepFree',
    icon: 'SF',
    label: 'Step-free Access',
    description: 'I need routes without steps'
  }
]

function unwrapResponse(res) {
  return res?.data ?? res?.user ?? res?.profile ?? res ?? {}
}

function formatMemberSince(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return 'Member since unavailable'

  return `Member since ${date.toLocaleDateString('en-US', {
    month: 'long',
    year: 'numeric'
  })}`
}

function roleLabel(value) {
  return normalizeRole(value)
}

function normalizePhotoUrl(photo) {
  if (!photo) return defaultAvatar
  return /^https?:\/\//i.test(photo) ? photo : `${import.meta.env.VITE_APP_BASE_API}${photo}`
}

function applyPreferences(preferences = {}) {
  profileForm.preferences.wheelchair = Boolean(preferences.wheelchairAccessible)
  profileForm.preferences.elevator = Boolean(preferences.elevatorNeeded)
  profileForm.preferences.restroom = Boolean(preferences.accessibleRestroom)
  profileForm.preferences.quiet = Boolean(preferences.quietEnvironment)
  profileForm.preferences.stepFree = Boolean(preferences.stepFreeAccess)
}

function buildPreferencesPayload() {
  return {
    wheelchairAccessible: Boolean(profileForm.preferences.wheelchair),
    elevatorNeeded: Boolean(profileForm.preferences.elevator),
    accessibleRestroom: Boolean(profileForm.preferences.restroom),
    quietEnvironment: Boolean(profileForm.preferences.quiet),
    stepFreeAccess: Boolean(profileForm.preferences.stepFree)
  }
}

function buildProfilePayload(includePreferences = true) {
  const payload = {
    fullName: profileForm.fullName,
    phone: profileForm.phone
  }

  if (includePreferences) {
    payload.accessibilityPreferences = buildPreferencesPayload()
  }

  return payload
}

function applyProfile(profile) {
  const role = profile.role || userStore.role

  profileForm.fullName = profile.fullName || ''
  profileForm.email = profile.email || ''
  profileForm.phone = profile.phone || ''
  profileForm.role = roleLabel(role)
  profileForm.memberSince = formatMemberSince(profile.createdAt)
  profileForm.avatar = normalizePhotoUrl(profile.photo)
  applyPreferences(profile.accessibilityPreferences)
}

async function loadProfile() {
  loading.value = true
  try {
    const profile = unwrapResponse(await getCurrentUserProfile())
    applyProfile(profile)
    await userStore.getInfo()
  } catch (error) {
    applyProfile({})
  } finally {
    loading.value = false
  }
}

function enableEditing() {
  isEditing.value = true
}

async function saveChanges() {
  if (!isEditing.value) return

  loading.value = true
  try {
    let updatedProfile
    try {
      updatedProfile = unwrapResponse(await updateCurrentUserProfile(buildProfilePayload(true)))
    } catch (error) {
      updatedProfile = unwrapResponse(await updateCurrentUserProfile(buildProfilePayload(false)))
      ElMessage.warning('Profile saved without accessibility preferences')
    }
    applyProfile(updatedProfile)
    await userStore.getInfo()
    isEditing.value = false
    ElMessage.success('Profile changes saved')
  } catch (error) {
    console.error('Failed to save profile:', error)
    ElMessage.error('Failed to save profile')
  } finally {
    loading.value = false
  }
}

async function handleAvatarUpload({ file }) {
  uploadingAvatar.value = true
  try {
    const updatedProfile = unwrapResponse(await uploadCurrentUserAvatar(file))
    applyProfile(updatedProfile)
    await userStore.getInfo()
    ElMessage.success('Avatar uploaded')
  } catch (error) {
    console.error('Failed to upload avatar:', error)
    ElMessage.error('Failed to upload avatar')
  } finally {
    uploadingAvatar.value = false
  }
}

function handleAvatarError(event) {
  event.target.src = defaultAvatar
}

function logout() {
  ElMessageBox.confirm('Are you sure you want to logout?', 'Logout', {
    confirmButtonText: 'Logout',
    cancelButtonText: 'Cancel',
    type: 'warning'
  }).then(() => {
    userStore.logOut().then(() => {
      location.href = '/login'
    })
  }).catch(() => {})
}

onMounted(loadProfile)
</script>

<style scoped lang="scss">
.profile-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.profile-shell {
  padding: 34px 44px 48px;
}

.page-heading {
  margin-bottom: 24px;
}

.page-heading h1 {
  margin: 0;
  color: #0d1f4f;
  font-size: 42px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: 0;
}

.page-heading p {
  margin: 10px 0 0;
  color: #4f5d7c;
  font-size: 16px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(280px, 390px) minmax(0, 1fr);
  gap: 24px;
}

.profile-card {
  border: 1px solid #e0e8f6;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(26, 54, 100, 0.07);
}

.profile-card h2 {
  margin: 0;
  color: #071a47;
  font-size: 20px;
  line-height: 1.3;
  font-weight: 800;
}

.summary-card {
  padding: 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.summary-card h2 {
  align-self: flex-start;
  margin-bottom: 18px;
}

.avatar-frame {
  margin: 0 auto 14px;
  width: 132px;
  height: 132px;
  border-radius: 50%;
  padding: 4px;
  background: #eaf3ff;
}

.profile-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.summary-card h3 {
  margin: 0 0 10px;
  color: #071a47;
  font-size: 24px;
  line-height: 1.2;
  font-weight: 800;
}

.role-pill {
  min-height: 30px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  color: #0f66e9;
  background: #ddebff;
  font-size: 14px;
  font-weight: 800;
}

.summary-list {
  margin-top: 22px;
  display: grid;
  gap: 14px;
  color: #344466;
  font-size: 15px;
  text-align: left;
}

.summary-list span {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.summary-list .el-icon {
  color: #1d3264;
}

.info-card {
  padding: 30px;
}

.profile-form {
  margin-top: 34px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px 34px;
}

.profile-form :deep(.el-form-item__label) {
  color: #10234f;
  font-weight: 700;
}

.profile-form :deep(.el-input__wrapper),
.profile-form :deep(.el-select__wrapper) {
  min-height: 52px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d3deed inset;
}

.accessibility-card {
  margin-top: 24px;
  padding: 30px 28px;
}

.preference-grid {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.preference-item {
  min-height: 96px;
  padding: 16px;
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  border: 1px solid #e0e8f6;
  border-radius: 8px;
  background: #fff;
}

.preference-icon {
  width: 50px;
  height: 50px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #0f66e9;
  background: #e8f2ff;
  font-size: 14px;
  font-weight: 800;
}

.preference-copy {
  min-width: 0;
  display: grid;
  gap: 7px;
  text-align: left;
}

.preference-copy strong {
  color: #10234f;
  font-size: 15px;
  line-height: 1.25;
}

.preference-copy small {
  color: #4f5d7c;
  font-size: 14px;
  line-height: 1.4;
}

.action-card {
  margin-top: 24px;
  padding: 20px 28px;
  display: flex;
  justify-content: flex-end;
  gap: 14px;
}

.profile-action {
  min-width: 190px;
  height: 48px;
  border-radius: 8px;
  color: #10234f;
  font-weight: 800;
}

.profile-action :deep(.el-icon) {
  margin-right: 6px;
}

.primary-action {
  color: #fff;
}

@media (max-width: 1180px) {
  .profile-grid,
  .preference-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .profile-shell {
    padding: 28px 24px 40px;
  }

  .profile-grid,
  .form-grid,
  .preference-grid {
    grid-template-columns: 1fr;
  }

  .action-card {
    flex-direction: column;
  }

  .profile-action {
    width: 100%;
    min-width: 0;
  }
}

@media (max-width: 560px) {
  .profile-shell {
    padding: 24px 16px 32px;
  }

  .page-heading h1 {
    font-size: 34px;
  }

  .summary-card,
  .info-card,
  .accessibility-card,
  .action-card {
    padding: 22px;
  }

  .preference-item {
    grid-template-columns: 50px minmax(0, 1fr);
  }

  .preference-item :deep(.el-switch) {
    grid-column: 2;
    justify-self: flex-start;
  }
}
</style>
