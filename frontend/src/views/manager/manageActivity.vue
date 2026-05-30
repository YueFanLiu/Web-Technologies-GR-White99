<template>
  <div class="manage-activity-page">
    <main class="manage-shell" v-loading="loading">
      <section class="page-heading">
        <h1>Manage Activity</h1>
        <el-button class="back-button" @click="backToEvents">
          <el-icon><ArrowLeft /></el-icon>
          Back to Events
        </el-button>
      </section>

      <el-empty v-if="!eventId" description="No activity selected" />

      <section v-else class="manage-grid">
        <div class="main-column">
          <section class="overview-card">
            <img :src="activity.cover" :alt="activity.title" class="overview-image" />

            <div class="overview-copy">
              <h2>{{ activity.title }}</h2>
              <div class="overview-meta">
                <p>
                  <el-icon><Calendar /></el-icon>
                  <span>{{ activity.date }}</span>
                </p>
                <p>
                  <el-icon><Clock /></el-icon>
                  <span>{{ activity.time }}</span>
                </p>
                <p>
                  <el-icon><Location /></el-icon>
                  <span>{{ activity.location }}</span>
                </p>
              </div>
              <el-tag class="published-tag" :class="statusClass" effect="plain">
                <el-icon><CircleCheckFilled /></el-icon>
                {{ form.status || 'DRAFT' }}
              </el-tag>
            </div>
          </section>

          <section class="form-card">
            <div class="section-title">
              <el-icon><Document /></el-icon>
              <h2>Basic Information</h2>
            </div>

            <el-form ref="formRef" class="activity-form" label-position="top" :model="form" :rules="rules">
              <div class="two-column">
                <el-form-item label="Activity Title" prop="title">
                  <el-input v-model="form.title" size="large" />
                </el-form-item>

                <el-form-item label="Category" prop="category">
                  <el-select v-model="form.category" size="large">
                    <el-option
                      v-for="category in EVENT_CATEGORY_OPTIONS"
                      :key="category.value"
                      :label="category.label"
                      :value="category.value"
                    />
                  </el-select>
                </el-form-item>
              </div>

              <el-form-item label="Description" prop="description">
                <el-input v-model="form.description" type="textarea" :rows="3" resize="none" />
              </el-form-item>
            </el-form>
          </section>

          <section class="form-card">
            <div class="section-title">
              <el-icon><Clock /></el-icon>
              <h2>Schedule</h2>
            </div>

            <el-form class="activity-form schedule-form" label-position="top" :model="form" :rules="rules">
              <el-form-item label="Start Date" prop="startDate">
                <el-date-picker v-model="form.startDate" type="date" size="large" placeholder="Select start date" />
              </el-form-item>

              <el-form-item label="End Date" prop="endDate">
                <el-date-picker v-model="form.endDate" type="date" size="large" placeholder="Select end date" />
              </el-form-item>

              <el-form-item label="Start Time" prop="startTime">
                <el-time-picker v-model="form.startTime" size="large" placeholder="Select start time" />
              </el-form-item>

              <el-form-item label="End Time" prop="endTime">
                <el-time-picker v-model="form.endTime" size="large" placeholder="Select end time" />
              </el-form-item>
            </el-form>
          </section>

          <section class="form-card">
            <div class="section-title">
              <el-icon><Location /></el-icon>
              <h2>Location</h2>
            </div>

            <el-form class="activity-form" label-position="top" :model="form" :rules="rules">
              <div class="two-column">
                <el-form-item label="Venue Name" prop="venueName">
                  <el-input v-model="form.venueName" size="large" />
                </el-form-item>

                <el-form-item label="Address" prop="address">
                  <el-input v-model="form.address" size="large" />
                </el-form-item>
              </div>
            </el-form>
          </section>

          <section class="form-card">
            <div class="section-title">
              <el-icon><UserFilled /></el-icon>
              <h2>Accessibility Features</h2>
            </div>

            <el-checkbox-group v-model="form.accessibility" class="accessibility-row">
              <el-checkbox label="Wheelchair Accessible" />
              <el-checkbox label="Elevator Available" />
              <el-checkbox label="Accessible Restroom" />
              <el-checkbox label="Quiet / Low Noise" />
              <el-checkbox label="Step-free Access" />
            </el-checkbox-group>
          </section>
        </div>

        <aside class="side-column">
          <section class="side-card">
            <h2>Status</h2>
            <el-select v-model="form.status" size="large" class="status-select">
              <el-option label="Published" value="PUBLISHED" />
              <el-option label="Draft" value="DRAFT" />
              <el-option label="Cancelled" value="CANCELLED" />
            </el-select>
            <el-form class="activity-form side-form" label-position="top">
              <el-form-item label="Capacity">
                <el-input v-model="form.capacity" size="large" type="number" min="1" />
              </el-form-item>
              <el-form-item label="Price">
                <el-input v-model="form.price" size="large" type="number" min="0" />
              </el-form-item>
            </el-form>
            <p>{{ statusDescription }}</p>
            <span>Last updated: {{ activity.updatedAt }}</span>
          </section>

          <section class="side-card">
            <h2>Registration Summary</h2>

            <div class="summary-list">
              <div>
                <span class="summary-icon">
                  <el-icon><User /></el-icon>
                </span>
                <p>Capacity</p>
                <strong>{{ registration.capacity }}</strong>
              </div>
              <div>
                <span class="summary-icon">
                  <el-icon><UserFilled /></el-icon>
                </span>
                <p>Registered</p>
                <strong>{{ registration.registered }}</strong>
              </div>
              <div>
                <span class="summary-icon">
                  <el-icon><User /></el-icon>
                </span>
                <p>Remaining Spots</p>
                <strong>{{ remainingSpots }}</strong>
              </div>
            </div>
          </section>

          <section class="side-card action-card">
            <h2>Actions</h2>

            <el-button size="large" class="action-button" @click="viewAttendees">
              <el-icon><UserFilled /></el-icon>
              View Attendees
            </el-button>
            <el-button size="large" class="danger-button" :disabled="form.status === 'CANCELLED'" @click="cancelEvent">
              <el-icon><CircleCloseFilled /></el-icon>
              Cancel Event
            </el-button>
            <el-button size="large" type="primary" class="save-button" :loading="saving" @click="saveChanges">
              <el-icon><FolderChecked /></el-icon>
              Save Changes
            </el-button>
          </section>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Calendar,
  CircleCheckFilled,
  CircleCloseFilled,
  Clock,
  Document,
  FolderChecked,
  Location,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import {
  createLocationAccessibility,
  deleteLocationAccessibility,
  getEvent,
  getEventRegistrations,
  getLocationAccessibility,
  updateEvent,
  updateLocation,
  updateLocationAccessibility
} from '@/api/manager/manageActivity'
import useUserStore from '@/store/modules/user'
import { canManageActivityForUser } from '@/utils/accessControl'
import { EVENT_CATEGORY_OPTIONS } from '@/constants/events'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const eventId = computed(() => route.query.id)

const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const originalEvent = ref(null)
const originalLocation = ref(null)
const hasAccessibility = ref(false)

function fallbackCover(id) {
  return `https://picsum.photos/seed/manage-${encodeURIComponent(id || 'activity')}/760/430`
}

const form = reactive({
  title: '',
  category: '',
  description: '',
  startDate: '',
  endDate: '',
  startTime: '',
  endTime: '',
  venueName: '',
  address: '',
  accessibility: [],
  capacity: 1,
  price: 0,
  status: 'PUBLISHED'
})

const registration = reactive({
  capacity: 0,
  registered: 0
})

const rules = {
  title: [{ required: true, message: 'Please enter activity title', trigger: 'blur' }],
  category: [{ required: true, message: 'Please select a category', trigger: 'change' }],
  description: [{ required: true, message: 'Please enter description', trigger: 'blur' }],
  startDate: [{ required: true, message: 'Please select a start date', trigger: 'change' }],
  endDate: [{ required: true, message: 'Please select an end date', trigger: 'change' }],
  startTime: [{ required: true, message: 'Please select start time', trigger: 'change' }],
  endTime: [{ required: true, message: 'Please select end time', trigger: 'change' }],
  venueName: [{ required: true, message: 'Please enter venue name', trigger: 'blur' }],
  address: [{ required: true, message: 'Please enter address', trigger: 'blur' }]
}

const accessibilityFeatures = [
  { label: 'Wheelchair Accessible', key: 'wheelchairAccessible' },
  { label: 'Elevator Available', key: 'hasElevator' },
  { label: 'Accessible Restroom', key: 'accessibleToilet' },
  { label: 'Quiet / Low Noise', key: 'quietEnvironment' },
  { label: 'Step-free Access', key: 'stepFreeAccess' }
]

const remainingSpots = computed(() => Math.max(registration.capacity - registration.registered, 0))

const activity = computed(() => {
  const event = originalEvent.value || {}
  const location = originalLocation.value || event.location || {}
  return {
    title: form.title || 'Untitled activity',
    date: formatDate(buildDateTime(form.startDate, form.startTime)),
    time: formatTime(buildDateTime(form.startDate, form.startTime), buildDateTime(form.endDate, form.endTime)),
    location: [form.venueName || location.name, form.address || location.address].filter(Boolean).join(', ') || 'Location TBA',
    cover: event.coverImageUrl || event.imageUrls?.[0] || fallbackCover(event.id),
    updatedAt: event.updatedAt ? formatDateTime(event.updatedAt) : 'Not available'
  }
})

const statusClass = computed(() => String(form.status || '').toLowerCase())
const statusDescription = computed(() => {
  if (form.status === 'CANCELLED') return 'This event is cancelled and should not accept new attendees.'
  if (form.status === 'DRAFT') return 'This event is saved as a draft.'
  return 'This event is visible to the public.'
})

function toDate(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function toLocalDateTime(value) {
  const date = new Date(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function buildDateTime(dateValue, timeValue) {
  if (!dateValue || !timeValue) return ''
  const date = new Date(dateValue)
  const time = new Date(timeValue)
  if (Number.isNaN(date.getTime()) || Number.isNaN(time.getTime())) return ''

  return new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate(),
    time.getHours(),
    time.getMinutes(),
    0,
    0
  )
}

function formatDate(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return 'Date TBA'
  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function formatTime(startValue, endValue) {
  const start = new Date(startValue)
  const end = new Date(endValue)
  if (!startValue || Number.isNaN(start.getTime())) return 'Time TBA'
  const options = { hour: 'numeric', minute: '2-digit' }
  if (!endValue || Number.isNaN(end.getTime())) return start.toLocaleTimeString('en-US', options)
  return `${start.toLocaleTimeString('en-US', options)} - ${end.toLocaleTimeString('en-US', options)}`
}

function formatDateTime(value) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return 'Not available'
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  })
}

function extractSelectedAccessibility(accessibility) {
  if (!accessibility) return []
  return accessibilityFeatures
    .filter((feature) => accessibility[feature.key])
    .map((feature) => feature.label)
}

function buildAccessibilityPayload() {
  const selected = new Set(form.accessibility)
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

async function loadActivity() {
  if (!eventId.value) {
    return
  }

  loading.value = true
  try {
    if (!userStore.userInfo) {
      await userStore.getInfo()
    }

    const event = await getEvent(eventId.value)
    if (!canManageActivityForUser(userStore.userInfo, event)) {
      ElMessage.warning('You do not have permission to manage this activity')
      router.replace('/product/mainEvent')
      return
    }

    const location = event.location || {}
    originalEvent.value = event
    originalLocation.value = location

    form.title = event.title || ''
    form.category = event.category || ''
    form.description = event.description || ''
    form.startDate = toDate(event.startTime)
    form.endDate = toDate(event.endTime)
    form.startTime = toDate(event.startTime)
    form.endTime = toDate(event.endTime)
    form.venueName = location.name || ''
    form.address = location.address || ''
    form.capacity = Number(event.capacity || 1)
    form.price = Number(event.price || 0)
    form.status = event.status || 'DRAFT'

    registration.capacity = form.capacity

    await Promise.all([loadRegistrations(), loadAccessibility(location.id)])
  } catch (error) {
    console.error(error)
    ElMessage.error('Failed to load activity')
  } finally {
    loading.value = false
  }
}

async function loadRegistrations() {
  try {
    const registrations = await getEventRegistrations(eventId.value)
    registration.registered = Array.isArray(registrations) ? registrations.length : 0
  } catch (error) {
    registration.registered = 0
  }
}

async function loadAccessibility(locationId) {
  if (!locationId) return
  try {
    const accessibility = await getLocationAccessibility(locationId)
    hasAccessibility.value = true
    form.accessibility = extractSelectedAccessibility(accessibility)
  } catch (error) {
    hasAccessibility.value = false
    form.accessibility = []
  }
}

function validateBusinessFields(startTime, endTime) {
  const capacity = Number(form.capacity)
  const requiredValues = [
    form.title,
    form.category,
    form.description,
    form.startDate,
    form.endDate,
    form.startTime,
    form.endTime,
    form.venueName,
    form.address
  ]

  if (requiredValues.some((value) => !value)) {
    ElMessage.warning('Please complete all required fields')
    return false
  }

  if (!Number.isInteger(capacity) || capacity <= 0) {
    ElMessage.warning('Capacity must be a positive whole number')
    return false
  }

  if (Number(form.price || 0) < 0) {
    ElMessage.warning('Price must not be negative')
    return false
  }

  if (!startTime || !endTime || new Date(endTime).getTime() <= new Date(startTime).getTime()) {
    ElMessage.warning('End time must be later than start time')
    return false
  }

  return true
}

async function saveChanges() {
  if (!canManageActivityForUser(userStore.userInfo, originalEvent.value)) {
    ElMessage.warning('You do not have permission to manage this activity')
    return
  }

  if (saving.value || !eventId.value || !originalLocation.value?.id) {
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const startTime = buildDateTime(form.startDate, form.startTime)
  const endTime = buildDateTime(form.endDate, form.endTime)

  if (!validateBusinessFields(startTime, endTime)) {
    return
  }

  saving.value = true
  try {
    await updateLocation(originalLocation.value.id, {
      name: form.venueName,
      description: form.description,
      address: form.address,
      city: originalLocation.value.city || '',
      country: originalLocation.value.country || '',
      latitude: originalLocation.value.latitude,
      longitude: originalLocation.value.longitude
    })

    const accessibilityPayload = buildAccessibilityPayload()
    const hasSelectedAccessibilityFeatures = hasSelectedAccessibility(accessibilityPayload)
    if (hasAccessibility.value && hasSelectedAccessibilityFeatures) {
      await updateLocationAccessibility(originalLocation.value.id, accessibilityPayload)
    } else if (hasAccessibility.value) {
      await deleteLocationAccessibility(originalLocation.value.id)
      hasAccessibility.value = false
    } else if (hasSelectedAccessibilityFeatures) {
      await createLocationAccessibility(originalLocation.value.id, accessibilityPayload)
      hasAccessibility.value = true
    }

    await updateEvent(eventId.value, {
      title: form.title,
      description: form.description,
      category: form.category,
      startTime: toLocalDateTime(startTime),
      endTime: toLocalDateTime(endTime),
      capacity: Number(form.capacity),
      price: Number(form.price || 0),
      isVirtual: false,
      status: form.status,
      locationId: originalLocation.value.id
    })

    ElMessage.success('Activity saved')
    await loadActivity()
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || 'Failed to save activity')
  } finally {
    saving.value = false
  }
}

async function cancelEvent() {
  try {
    await ElMessageBox.confirm('Cancel this event?', 'Warning', {
      confirmButtonText: 'Confirm',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    form.status = 'CANCELLED'
    await saveChanges()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

function backToEvents() {
  router.push('/manager/mainActivity')
}

function viewAttendees() {
  if (!canManageActivityForUser(userStore.userInfo, originalEvent.value)) {
    ElMessage.warning('You do not have permission to view attendees')
    return
  }

  router.push({
    path: '/manager/attendeeList',
    query: { id: eventId.value }
  })
}

onMounted(loadActivity)
</script>

<style scoped lang="scss">
.manage-activity-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.manage-shell {
  padding: 36px 44px 48px;
}

.page-heading {
  margin-bottom: 26px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.page-heading h1 {
  margin: 0;
  color: #0d1f4f;
  font-size: 42px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: 0;
}

.back-button {
  height: 46px;
  padding: 0 22px;
  border-color: #c7d8f4;
  border-radius: 10px;
  color: #0f66e9;
  background: #fff;
  font-weight: 700;
}

.manage-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  align-items: start;
}

.main-column,
.side-column {
  display: grid;
  gap: 20px;
}

.overview-card,
.form-card,
.side-card {
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid #e0e8f6;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.09);
}

.overview-card {
  padding: 16px;
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 28px;
  align-items: center;
}

.overview-image {
  width: 100%;
  height: 206px;
  border-radius: 12px;
  object-fit: cover;
}

.overview-copy h2 {
  margin: 0 0 18px;
  color: #071a47;
  font-size: 26px;
  line-height: 1.2;
  font-weight: 800;
}

.overview-meta {
  display: grid;
  gap: 13px;
}

.overview-meta p {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #26365f;
  font-size: 16px;
}

.overview-meta .el-icon {
  color: #0f66e9;
  font-size: 20px;
}

.published-tag,
.status-pill {
  width: fit-content;
  margin-top: 18px;
  padding: 0 16px;
  height: 38px;
  border: 0;
  border-radius: 8px;
  color: #16833d;
  background: #dcf6e3;
  font-size: 15px;
  font-weight: 800;
}

.published-tag.cancelled {
  color: #ec2d3d;
  background: #ffe5e9;
}

.published-tag.draft {
  color: #c47a00;
  background: #fff1d2;
}

.published-tag :deep(.el-tag__content),
.status-pill :deep(.el-tag__content) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-card,
.side-card {
  padding: 22px;
}

.section-title {
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title .el-icon {
  color: #0f66e9;
  font-size: 22px;
}

.section-title h2,
.side-card h2 {
  margin: 0;
  color: #0f66e9;
  font-size: 19px;
  font-weight: 800;
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
.activity-form :deep(.el-date-editor.el-input__wrapper),
.status-select {
  width: 100%;
}

.activity-form :deep(.el-input__wrapper),
.activity-form :deep(.el-textarea__inner),
.status-select :deep(.el-input__wrapper) {
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
}

.accessibility-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 28px;
}

.accessibility-row :deep(.el-checkbox) {
  margin-right: 0;
  color: #1c2c57;
  font-weight: 600;
}

.accessibility-row :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  border-color: #2f75f6;
  background-color: #2f75f6;
}

.side-card > p {
  margin: 20px 0 24px;
  color: #26365f;
  font-size: 15px;
  line-height: 1.6;
}

.side-form {
  margin-top: 18px;
}

.side-card > span {
  color: #667091;
  font-size: 14px;
}

.summary-list {
  margin-top: 24px;
  display: grid;
  gap: 22px;
}

.summary-list div {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
}

.summary-icon {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #0f66e9;
  background: #eef5ff;
  font-size: 22px;
}

.summary-list p {
  margin: 0;
  color: #1c2c57;
  font-size: 15px;
  font-weight: 600;
}

.summary-list strong {
  color: #071a47;
  font-size: 18px;
  font-weight: 800;
}

.action-card {
  display: grid;
  gap: 14px;
}

.action-card h2 {
  margin-bottom: 6px;
}

.action-button,
.danger-button,
.save-button {
  width: 100%;
  height: 52px;
  margin-left: 0;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 800;
}

.action-button {
  border-color: #9fc0f7;
  color: #0f66e9;
  background: #fff;
}

.danger-button {
  border-color: #ff8c99;
  color: #ec2d3d;
  background: #fff;
}

.save-button {
  box-shadow: 0 10px 22px rgba(47, 117, 246, 0.22);
}

@media (max-width: 1180px) {
  .manage-shell {
    padding: 30px 24px 40px;
  }

  .manage-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .action-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 860px) {
  .overview-card,
  .two-column,
  .schedule-form,
  .side-column {
    grid-template-columns: 1fr;
  }

  .overview-image {
    height: 220px;
  }
}

@media (max-width: 560px) {
  .manage-shell {
    padding: 24px 16px 32px;
  }

  .page-heading {
    align-items: flex-start;
    flex-direction: column;
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
