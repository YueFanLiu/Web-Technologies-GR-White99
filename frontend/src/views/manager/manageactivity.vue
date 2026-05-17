<template>
  <div class="manage-activity-page">
    <main class="manage-shell">
      <section class="page-heading">
        <h1>Manage Activity</h1>
        <el-button class="back-button" @click="backToEvents">
          <el-icon><ArrowLeft /></el-icon>
          Back to Events
        </el-button>
      </section>

      <section class="manage-grid">
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
              <el-tag class="published-tag" effect="plain">
                <el-icon><CircleCheckFilled /></el-icon>
                Published
              </el-tag>
            </div>
          </section>

          <section class="form-card">
            <div class="section-title">
              <el-icon><Document /></el-icon>
              <h2>Basic Information</h2>
            </div>

            <el-form class="activity-form" label-position="top">
              <div class="two-column">
                <el-form-item label="Activity Title">
                  <el-input v-model="form.title" size="large" />
                </el-form-item>

                <el-form-item label="Category">
                  <el-select v-model="form.category" size="large">
                    <el-option label="Music" value="Music" />
                    <el-option label="Workshop" value="Workshop" />
                    <el-option label="Family Activity" value="Family Activity" />
                    <el-option label="Community Event" value="Community Event" />
                  </el-select>
                </el-form-item>
              </div>

              <el-form-item label="Description">
                <el-input v-model="form.description" type="textarea" :rows="3" resize="none" />
              </el-form-item>
            </el-form>
          </section>

          <section class="form-card">
            <div class="section-title">
              <el-icon><Clock /></el-icon>
              <h2>Schedule</h2>
            </div>

            <el-form class="activity-form schedule-form" label-position="top">
              <el-form-item label="Date">
                <el-date-picker v-model="form.date" type="date" size="large" placeholder="Select date" />
              </el-form-item>

              <el-form-item label="Start Time">
                <el-time-picker v-model="form.startTime" size="large" placeholder="Select start time" />
              </el-form-item>

              <el-form-item label="End Time">
                <el-time-picker v-model="form.endTime" size="large" placeholder="Select end time" />
              </el-form-item>
            </el-form>
          </section>

          <section class="form-card">
            <div class="section-title">
              <el-icon><Location /></el-icon>
              <h2>Location</h2>
            </div>

            <el-form class="activity-form" label-position="top">
              <el-form-item label="Venue Name">
                <el-input v-model="form.venueName" size="large" />
              </el-form-item>

              <el-form-item label="Address">
                <el-input v-model="form.address" size="large" />
              </el-form-item>
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
            </el-checkbox-group>
          </section>
        </div>

        <aside class="side-column">
          <section class="side-card">
            <h2>Status</h2>
            <el-tag class="status-pill" effect="plain">
              <el-icon><CircleCheckFilled /></el-icon>
              Published
            </el-tag>
            <p>This event is visible to the public.</p>
            <span>Last updated: May 10, 2025 10:30 AM</span>
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
            <el-button size="large" class="action-button" @click="sendUpdate">
              <el-icon><Promotion /></el-icon>
              Send Update
            </el-button>
            <el-button size="large" class="danger-button" @click="cancelEvent">
              <el-icon><CircleCloseFilled /></el-icon>
              Cancel Event
            </el-button>
            <el-button size="large" type="primary" class="save-button" @click="saveChanges">
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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Calendar,
  CircleCheckFilled,
  CircleCloseFilled,
  Clock,
  Document,
  FolderChecked,
  Location,
  Promotion,
  User,
  UserFilled
} from '@element-plus/icons-vue'

const router = useRouter()

const activity = {
  title: 'Inclusive Music Festival',
  date: 'June 15, 2025',
  time: '2:00 PM - 10:00 PM',
  location: 'Greenfield Park, Main Pavilion',
  cover: 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=760&q=80'
}

const form = ref({
  title: 'Inclusive Music Festival',
  category: 'Music',
  description: 'An inclusive outdoor music festival celebrating diversity and accessibility for everyone.',
  date: '',
  startTime: '',
  endTime: '',
  venueName: 'Greenfield Park, Main Pavilion',
  address: '123 Park Lane, Springfield, IL 62704',
  accessibility: [
    'Wheelchair Accessible',
    'Elevator Available',
    'Accessible Restroom',
    'Quiet / Low Noise'
  ]
})

const registration = {
  capacity: 300,
  registered: 128
}

const remainingSpots = computed(() => registration.capacity - registration.registered)

function backToEvents() {
  router.push('/product/mainEvent')
}

function viewAttendees() {
  router.push('/product/attendeeList')
}

function sendUpdate() {
  ElMessage.success('Update sent')
}

function cancelEvent() {
  ElMessage.warning('Event cancelled')
}

function saveChanges() {
  ElMessage.success('Changes saved')
}
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
