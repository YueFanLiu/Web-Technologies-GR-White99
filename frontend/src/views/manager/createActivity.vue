<template>
  <div class="create-activity-page">
    <main class="activity-shell">
      <section class="page-heading">
        <h1>Create Activity</h1>
        <p>Fill in the essential details to create your event.</p>
      </section>

      <section class="activity-grid">
        <div class="main-column">
          <section class="form-card">
            <div class="section-title">
              <span class="section-icon">
                <el-icon><InfoFilled /></el-icon>
              </span>
              <h2>1. Basic Information</h2>
            </div>

            <el-form
              ref="basicFormRef"
              class="activity-form"
              label-position="top"
              :model="form"
              :rules="rules"
            >
              <div class="two-column">
                <el-form-item label="Activity Title" prop="title">
                  <el-input v-model="form.title" size="large" placeholder="Enter activity title" />
                </el-form-item>

                <el-form-item label="Category" prop="category">
                  <el-select v-model="form.category" size="large" placeholder="Select a category">
                    <el-option label="Music" value="Music" />
                    <el-option label="Workshop" value="Workshop" />
                    <el-option label="Concert" value="Concert" />
                    <el-option label="Family Activity" value="Family Activity" />
                    <el-option label="Community Event" value="Community Event" />
                  </el-select>
                </el-form-item>
              </div>

              <el-form-item label="Description" prop="description">
                <el-input
                  v-model="form.description"
                  type="textarea"
                  :rows="4"
                  resize="none"
                  placeholder="Describe your activity..."
                />
              </el-form-item>
            </el-form>
          </section>

          <section class="form-card">
            <div class="section-title">
              <span class="section-icon">
                <el-icon><Calendar /></el-icon>
              </span>
              <h2>2. Schedule</h2>
            </div>

            <el-form
              ref="scheduleFormRef"
              class="activity-form schedule-form"
              label-position="top"
              :model="form"
              :rules="rules"
            >
              <el-form-item label="Date" prop="date">
                <el-date-picker
                  v-model="form.date"
                  type="date"
                  size="large"
                  placeholder="Select date"
                />
              </el-form-item>

              <el-form-item label="Start Time" prop="startTime">
                <el-time-picker
                  v-model="form.startTime"
                  size="large"
                  placeholder="Select start time"
                />
              </el-form-item>

              <el-form-item label="End Time" prop="endTime">
                <el-time-picker
                  v-model="form.endTime"
                  size="large"
                  placeholder="Select end time"
                />
              </el-form-item>
            </el-form>
          </section>

          <section class="form-card">
            <div class="section-title">
              <span class="section-icon">
                <el-icon><Location /></el-icon>
              </span>
              <h2>3. Location</h2>
            </div>

            <el-form
              ref="locationFormRef"
              class="activity-form two-column"
              label-position="top"
              :model="form"
              :rules="rules"
            >
              <el-form-item label="Venue Name" prop="venueName">
                <el-input v-model="form.venueName" size="large" placeholder="Enter venue name" />
              </el-form-item>

              <el-form-item label="Address" prop="address">
                <el-input v-model="form.address" size="large" placeholder="Enter full address" />
              </el-form-item>
            </el-form>
          </section>

          <section class="form-card">
            <div class="section-title">
              <span class="section-icon">
                <el-icon><UserFilled /></el-icon>
              </span>
              <h2>4. Accessibility Features</h2>
            </div>

            <el-checkbox-group v-model="form.accessibility" class="feature-grid">
              <el-checkbox-button
                v-for="feature in accessibilityFeatures"
                :key="feature.label"
                :label="feature.label"
                class="feature-option"
              >
                <el-icon><component :is="feature.icon" /></el-icon>
                <span>{{ feature.label }}</span>
              </el-checkbox-button>
            </el-checkbox-group>
          </section>
        </div>

        <aside class="side-column">
          <section class="side-card">
            <div class="side-title">
              <el-icon><Picture /></el-icon>
              <h2>Cover Image</h2>
            </div>
            <p>Upload a cover image for your activity.</p>

            <div class="cover-preview">
              <img :src="coverImage" alt="Activity cover preview" />
            </div>

            <el-upload
              v-model:file-list="coverList"
              action="#"
              accept="image/*"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleCoverChange"
              :on-remove="handleCoverRemove"
            >
              <el-button class="upload-button">
                <el-icon><Upload /></el-icon>
                Change Image
              </el-button>
            </el-upload>
          </section>

          <section class="side-card">
            <div class="side-title">
              <el-icon><User /></el-icon>
              <h2>Capacity</h2>
            </div>

            <el-form
              ref="capacityFormRef"
              class="activity-form"
              label-position="top"
              :model="form"
              :rules="rules"
            >
              <el-form-item label="Capacity" prop="capacity">
                <el-input
                  v-model="form.capacity"
                  size="large"
                  placeholder="Enter capacity"
                  type="number"
                >
                  <template #suffix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </el-form>
          </section>

          <section class="side-card action-card">
            <div class="side-title">
              <el-icon><Promotion /></el-icon>
              <h2>Publish</h2>
            </div>

            <el-button
              size="large"
              type="primary"
              class="publish-button"
              :loading="submitting"
              @click="publishActivity"
            >
              <el-icon><Promotion /></el-icon>
              Publish Activity
            </el-button>

            <el-button size="large" class="cancel-button" :disabled="submitting" @click="cancelCreate">
              Cancel
            </el-button>
          </section>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createEvent,
  createLocation,
  createLocationAccessibility,
  uploadEventImage
} from '@/api/manager/createActivity'
import useUserStore from '@/store/modules/user'
import { canCreateActivityForUser } from '@/utils/accessControl'
import {
  Calendar,
  InfoFilled,
  Location,
  Microphone,
  Picture,
  Promotion,
  Service,
  Upload,
  User,
  UserFilled,
  Van,
  VideoCamera
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const basicFormRef = ref()
const scheduleFormRef = ref()
const locationFormRef = ref()
const capacityFormRef = ref()
const submitting = ref(false)
const canCreateActivity = computed(() => canCreateActivityForUser(userStore.userInfo))

const form = ref({
  title: '',
  category: '',
  description: '',
  date: '',
  startTime: '',
  endTime: '',
  venueName: '',
  address: '',
  accessibility: [],
  capacity: ''
})

const coverList = ref([])
const defaultCoverImage = 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=760&q=80'
const coverImage = ref(defaultCoverImage)
const coverFile = ref(null)
let coverObjectUrl = ''

const rules = {
  title: [{ required: true, message: 'Please enter activity title', trigger: 'blur' }],
  category: [{ required: true, message: 'Please select a category', trigger: 'change' }],
  description: [{ required: true, message: 'Please enter description', trigger: 'blur' }],
  date: [{ required: true, message: 'Please select a date', trigger: 'change' }],
  startTime: [{ required: true, message: 'Please select start time', trigger: 'change' }],
  endTime: [{ required: true, message: 'Please select end time', trigger: 'change' }],
  venueName: [{ required: true, message: 'Please enter venue name', trigger: 'blur' }],
  address: [{ required: true, message: 'Please enter address', trigger: 'blur' }],
  capacity: [{ required: true, message: 'Please enter capacity', trigger: 'blur' }]
}

const accessibilityFeatures = [
  { label: 'Wheelchair Accessible', icon: Service, key: 'wheelchairAccessible' },
  { label: 'Elevator Available', icon: UserFilled, key: 'hasElevator' },
  { label: 'Accessible Restroom', icon: VideoCamera, key: 'accessibleToilet' },
  { label: 'Quiet / Low Noise', icon: Microphone, key: 'quietEnvironment' },
  { label: 'Step-free Access', icon: Van, key: 'stepFreeAccess' }
]

function toEntityId(response) {
  return response?.id || response?.data?.id
}

function buildDateTime(dateValue, timeValue) {
  const date = new Date(dateValue)
  const time = new Date(timeValue)

  return new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate(),
    time.getHours(),
    time.getMinutes(),
    0,
    0
  ).toISOString()
}

function buildAccessibilityPayload() {
  const selected = new Set(form.value.accessibility)
  const payload = accessibilityFeatures.reduce((result, feature) => {
    result[feature.key] = selected.has(feature.label)
    return result
  }, {})

  return {
    ...payload,
    notes: ''
  }
}

function hasSelectedAccessibility(payload) {
  return accessibilityFeatures.some((feature) => Boolean(payload[feature.key]))
}

function validateBusinessFields(startTime, endTime) {
  const capacity = Number(form.value.capacity)

  if (!Number.isInteger(capacity) || capacity <= 0) {
    ElMessage.warning('Capacity must be a positive whole number')
    return false
  }

  if (new Date(endTime).getTime() <= new Date(startTime).getTime()) {
    ElMessage.warning('End time must be later than start time')
    return false
  }

  return true
}

function handleCoverChange(file, fileList) {
  const rawFile = file.raw

  if (!rawFile?.type?.startsWith('image/')) {
    ElMessage.warning('Please choose an image file')
    coverList.value = fileList.filter((item) => item.uid !== file.uid)
    return
  }

  if (coverObjectUrl) {
    URL.revokeObjectURL(coverObjectUrl)
  }

  coverFile.value = rawFile
  coverObjectUrl = URL.createObjectURL(rawFile)
  coverImage.value = coverObjectUrl
  coverList.value = [file]
}

function handleCoverRemove() {
  coverFile.value = null

  if (coverObjectUrl) {
    URL.revokeObjectURL(coverObjectUrl)
    coverObjectUrl = ''
  }

  coverImage.value = defaultCoverImage
}

async function publishActivity() {
  if (!canCreateActivity.value) {
    ElMessage.warning('You do not have permission to create activities')
    router.replace('/product/mainEvent')
    return
  }

  if (submitting.value) {
    return
  }

  const valid = await Promise.all([
    basicFormRef.value?.validate().catch(() => false),
    scheduleFormRef.value?.validate().catch(() => false),
    locationFormRef.value?.validate().catch(() => false),
    capacityFormRef.value?.validate().catch(() => false)
  ]).then((results) => results.every(Boolean))
  if (!valid) {
    return
  }

  const startTime = buildDateTime(form.value.date, form.value.startTime)
  const endTime = buildDateTime(form.value.date, form.value.endTime)

  if (!validateBusinessFields(startTime, endTime)) {
    return
  }

  submitting.value = true

  try {
    const location = await createLocation({
      name: form.value.venueName,
      description: form.value.description,
      address: form.value.address,
      city: '',
      country: ''
    })
    const locationId = toEntityId(location)

    if (!locationId) {
      throw new Error('Create location response did not include an id')
    }

    const accessibilityPayload = buildAccessibilityPayload()
    if (hasSelectedAccessibility(accessibilityPayload)) {
      await createLocationAccessibility(locationId, accessibilityPayload)
    }

    const event = await createEvent({
      title: form.value.title,
      description: form.value.description,
      category: form.value.category,
      startTime,
      endTime,
      capacity: Number(form.value.capacity),
      price: 0,
      isVirtual: false,
      status: 'PUBLISHED',
      locationId
    })
    const eventId = toEntityId(event)

    if (!eventId) {
      throw new Error('Create event response did not include an id')
    }

    if (coverFile.value) {
      await uploadEventImage(eventId, coverFile.value)
    }

    ElMessage.success('Activity published')
    router.push('/manager/mainActivity')
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || 'Failed to publish activity')
  } finally {
    submitting.value = false
  }
}

function cancelCreate() {
  router.push('/product/mainEvent')
}

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.getInfo()
  }

  if (!canCreateActivity.value) {
    ElMessage.warning('You do not have permission to create activities')
    router.replace('/product/mainEvent')
  }
})

onBeforeUnmount(() => {
  if (coverObjectUrl) {
    URL.revokeObjectURL(coverObjectUrl)
  }
})
</script>

<style scoped lang="scss">
.create-activity-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.activity-shell {
  padding: 36px 44px 48px;
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
  color: #53617f;
  font-size: 16px;
}

.activity-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 390px;
  gap: 28px;
  align-items: start;
}

.main-column,
.side-column {
  display: grid;
  gap: 20px;
}

.form-card,
.side-card {
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid #e0e8f6;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.09);
}

.form-card {
  padding: 24px;
}

.side-card {
  padding: 22px;
}

.section-title,
.side-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title {
  margin-bottom: 20px;
}

.section-icon {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border: 1px solid #2f75f6;
  border-radius: 50%;
  color: #0f66e9;
  background: #f4f8ff;
  font-size: 17px;
}

.section-title h2,
.side-title h2 {
  margin: 0;
  color: #0f66e9;
  font-size: 18px;
  font-weight: 800;
}

.side-title .el-icon {
  color: #0f66e9;
  font-size: 24px;
}

.side-card > p {
  margin: 12px 0 18px;
  color: #667091;
  font-size: 14px;
}

.activity-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.activity-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.activity-form :deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: #1c2c57;
  font-size: 14px;
  font-weight: 700;
}

.activity-form :deep(.el-input),
.activity-form :deep(.el-select),
.activity-form :deep(.el-date-editor.el-input),
.activity-form :deep(.el-date-editor.el-input__wrapper) {
  width: 100%;
}

.activity-form :deep(.el-input__wrapper),
.activity-form :deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d6dfef inset;
}

.activity-form :deep(.el-textarea__inner) {
  padding: 16px;
  color: #26365f;
  font-size: 15px;
  line-height: 1.6;
}

.two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.schedule-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.feature-grid :deep(.el-checkbox-button__inner) {
  width: 100%;
  min-height: 124px;
  padding: 16px 10px;
  display: grid;
  place-items: center;
  gap: 10px;
  border: 1px solid #bdd3f5;
  border-radius: 12px;
  color: #18315f;
  background: #fbfdff;
  font-size: 14px;
  line-height: 1.3;
  text-align: center;
  box-shadow: none;
}

.feature-grid :deep(.el-checkbox-button__inner .el-icon) {
  color: #2f75f6;
  font-size: 32px;
}

.feature-grid :deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) {
  border-color: #5f96ff;
  background: #eef6ff;
  color: #0f66e9;
  box-shadow: none;
}

.cover-preview {
  overflow: hidden;
  border-radius: 14px;
}

.cover-preview img {
  width: 100%;
  height: 250px;
  display: block;
  object-fit: cover;
}

.upload-button {
  width: 190px;
  height: 44px;
  margin-top: 16px;
  border-color: #2f75f6;
  border-radius: 10px;
  color: #0f66e9;
  background: #fff;
  font-weight: 700;
}

.action-card {
  display: grid;
  gap: 14px;
}

.publish-button,
.cancel-button {
  width: 100%;
  height: 52px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 700;
}

.publish-button {
  margin-top: 4px;
  box-shadow: 0 10px 22px rgba(47, 117, 246, 0.22);
}

.cancel-button {
  margin-left: 0;
  border-color: #2f75f6;
  color: #0f66e9;
  background: #fff;
}

@media (max-width: 1180px) {
  .activity-shell {
    padding: 30px 24px 40px;
  }

  .activity-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .action-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 820px) {
  .two-column,
  .schedule-form,
  .feature-grid,
  .side-column {
    grid-template-columns: 1fr;
  }

  .cover-preview img {
    height: 210px;
  }
}

@media (max-width: 560px) {
  .activity-shell {
    padding: 24px 16px 32px;
  }

  .page-heading h1 {
    font-size: 34px;
  }

  .form-card,
  .side-card {
    padding: 20px;
  }
}
</style>
