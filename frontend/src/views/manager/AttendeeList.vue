<template>
  <div class="attendee-list-page">
    <main class="attendee-shell">
      <section class="page-heading">
        <el-button class="back-icon" @click="backToManage">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div>
          <h1>Attendee List</h1>
          <p>Manage Activity / Summer Inclusive Sports Day / Attendees</p>
        </div>
      </section>

      <section class="overview-card">
        <img :src="event.image" :alt="event.title" class="event-image" />

        <div class="event-info">
          <div class="title-row">
            <h2>{{ event.title }}</h2>
            <el-tag class="published-tag" effect="plain">Published</el-tag>
          </div>

          <div class="event-meta">
            <p>
              <el-icon><Calendar /></el-icon>
              <span>{{ event.date }}</span>
            </p>
            <p>
              <el-icon><Clock /></el-icon>
              <span>{{ event.time }}</span>
            </p>
            <p>
              <el-icon><Location /></el-icon>
              <span>{{ event.location }}</span>
            </p>
            <p>
              <el-icon><User /></el-icon>
              <span>Registered: {{ registered }} / {{ capacity }}</span>
            </p>
          </div>
        </div>
      </section>

      <section class="summary-grid">
        <article class="summary-card">
          <span class="summary-icon blue">
            <el-icon><User /></el-icon>
          </span>
          <div>
            <p>Total Capacity</p>
            <strong>{{ capacity }}</strong>
          </div>
        </article>

        <article class="summary-card">
          <span class="summary-icon green">
            <el-icon><UserFilled /></el-icon>
          </span>
          <div>
            <p>Registered</p>
            <strong>{{ registered }}</strong>
          </div>
        </article>

        <article class="summary-card">
          <span class="summary-icon amber">
            <el-icon><Plus /></el-icon>
          </span>
          <div>
            <p>Remaining Spots</p>
            <strong>{{ remainingSpots }}</strong>
          </div>
        </article>
      </section>

      <section class="table-card">
        <div class="toolbar">
          <el-input
            v-model="searchText"
            size="large"
            class="search-input"
            placeholder="Search attendee"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-select v-model="statusFilter" size="large" class="status-filter">
            <el-option label="All Status" value="all" />
            <el-option label="Confirmed" value="Confirmed" />
            <el-option label="Pending" value="Pending" />
            <el-option label="Cancelled" value="Cancelled" />
          </el-select>
        </div>

        <el-table :data="filteredAttendees" class="attendee-table">
          <el-table-column label="Attendee" min-width="220">
            <template #default="{ row }">
              <div class="attendee-cell">
                <img :src="row.avatar" :alt="row.name" />
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="email" label="Contact" min-width="210" />

          <el-table-column label="Ticket" min-width="170">
            <template #default="{ row }">
              <div class="ticket-cell">
                <span>{{ row.ticket }}</span>
                <small>{{ row.quantity }} {{ row.quantity === 1 ? 'Ticket' : 'Tickets' }}</small>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="Status" width="150">
            <template #default="{ row }">
              <el-tag class="status-tag" :class="row.status.toLowerCase()" effect="plain">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Actions" width="150" align="center">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button :icon="View" aria-label="View attendee" @click="viewAttendee(row)" />
                <el-button :icon="Delete" class="remove-button" aria-label="Remove attendee" @click="removeAttendee(row)" />
              </div>
            </template>
          </el-table-column>
        </el-table>
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
  Clock,
  Delete,
  Location,
  Plus,
  Search,
  User,
  UserFilled,
  View
} from '@element-plus/icons-vue'

const router = useRouter()

const searchText = ref('')
const statusFilter = ref('all')

const capacity = 50
const registered = 32
const remainingSpots = computed(() => capacity - registered)

const event = {
  title: 'Summer Inclusive Sports Day',
  date: 'Sat, 24 May 2025',
  time: '10:00 AM - 3:00 PM',
  location: 'Central Park Sports Ground, Paris, France',
  image: 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&w=760&q=80'
}

const attendees = ref([
  {
    name: 'Sarah Johnson',
    email: 'sarah.j@email.com',
    ticket: 'General Admission',
    quantity: 1,
    status: 'Confirmed',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=96&q=80'
  },
  {
    name: 'Michael Brown',
    email: 'michael.b@email.com',
    ticket: 'General Admission',
    quantity: 2,
    status: 'Confirmed',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=96&q=80'
  },
  {
    name: 'Emily Davis',
    email: 'emily.d@email.com',
    ticket: 'General Admission',
    quantity: 1,
    status: 'Pending',
    avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=96&q=80'
  },
  {
    name: 'David Wilson',
    email: 'david.w@email.com',
    ticket: 'General Admission',
    quantity: 1,
    status: 'Confirmed',
    avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80'
  },
  {
    name: 'Olivia Martinez',
    email: 'olivia.m@email.com',
    ticket: 'General Admission',
    quantity: 1,
    status: 'Cancelled',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=96&q=80'
  }
])

const filteredAttendees = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()

  return attendees.value.filter((attendee) => {
    const matchesSearch = !keyword ||
      attendee.name.toLowerCase().includes(keyword) ||
      attendee.email.toLowerCase().includes(keyword)
    const matchesStatus = statusFilter.value === 'all' || attendee.status === statusFilter.value

    return matchesSearch && matchesStatus
  })
})

function backToManage() {
  router.push('/product/manageActivity')
}

function viewAttendee(attendee) {
  ElMessage.info(`Viewing ${attendee.name}`)
}

function removeAttendee(attendee) {
  ElMessage.warning(`${attendee.name} removed`)
}
</script>

<style scoped lang="scss">
.attendee-list-page {
  min-height: 100vh;
  background: #f7faff;
  color: #10234f;
  font-family: Inter, "Segoe UI", Arial, sans-serif;
}

.attendee-shell {
  padding: 34px 44px 48px;
}

.page-heading {
  margin-bottom: 26px;
  display: flex;
  align-items: center;
  gap: 22px;
}

.back-icon {
  width: 48px;
  height: 48px;
  padding: 0;
  border-color: #d6dfef;
  border-radius: 50%;
  color: #0f66e9;
  background: #fff;
  font-size: 20px;
}

.page-heading h1 {
  margin: 0;
  color: #0d1f4f;
  font-size: 38px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: 0;
}

.page-heading p {
  margin: 8px 0 0;
  color: #667091;
  font-size: 14px;
}

.overview-card,
.summary-card,
.table-card {
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid #e0e8f6;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.09);
}

.overview-card {
  padding: 22px;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 32px;
  align-items: center;
}

.event-image {
  width: 100%;
  height: 176px;
  border-radius: 12px;
  object-fit: cover;
}

.title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
}

.title-row h2 {
  margin: 0;
  color: #071a47;
  font-size: 25px;
  line-height: 1.2;
  font-weight: 800;
}

.published-tag {
  height: 28px;
  border: 0;
  border-radius: 7px;
  color: #16833d;
  background: #dcf6e3;
  font-weight: 800;
}

.event-meta {
  margin-top: 18px;
  display: grid;
  gap: 12px;
}

.event-meta p {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #53617f;
  font-size: 15px;
}

.event-meta .el-icon {
  color: #0f66e9;
  font-size: 19px;
}

.summary-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.summary-card {
  min-height: 112px;
  padding: 22px;
  display: flex;
  align-items: center;
  gap: 18px;
}

.summary-icon {
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 28px;
}

.summary-icon.blue {
  color: #0f66e9;
  background: #eef5ff;
}

.summary-icon.green {
  color: #159947;
  background: #e9f9ef;
}

.summary-icon.amber {
  color: #d98a00;
  background: #fff5df;
}

.summary-card p {
  margin: 0 0 8px;
  color: #667091;
  font-size: 15px;
}

.summary-card strong {
  color: #071a47;
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
}

.table-card {
  margin-top: 20px;
  overflow: hidden;
}

.toolbar {
  padding: 22px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 18px;
  border-bottom: 1px solid #e0e8f6;
}

.search-input :deep(.el-input__wrapper),
.status-filter :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d6dfef inset;
}

.attendee-table {
  width: 100%;
}

.attendee-table :deep(th.el-table__cell) {
  height: 56px;
  background: #fbfdff;
  color: #1c2c57;
  font-weight: 800;
}

.attendee-table :deep(td.el-table__cell) {
  height: 72px;
  color: #26365f;
}

.attendee-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}

.attendee-cell img {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}

.attendee-cell span {
  color: #071a47;
  font-weight: 700;
}

.ticket-cell {
  display: grid;
  gap: 4px;
}

.ticket-cell span {
  color: #1c2c57;
  font-weight: 600;
}

.ticket-cell small {
  color: #667091;
  font-size: 13px;
}

.status-tag {
  min-width: 88px;
  justify-content: center;
  border: 0;
  border-radius: 7px;
  font-weight: 700;
}

.status-tag.confirmed {
  color: #16833d;
  background: #dcf6e3;
}

.status-tag.pending {
  color: #c47a00;
  background: #fff1d2;
}

.status-tag.cancelled {
  color: #e23a4a;
  background: #ffe5e9;
}

.row-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.row-actions .el-button {
  width: 42px;
  height: 42px;
  padding: 0;
  margin-left: 0;
  border-color: #d6dfef;
  border-radius: 9px;
  color: #1c2c57;
  background: #fff;
}

.row-actions .remove-button {
  color: #ef3c4d;
}

@media (max-width: 980px) {
  .attendee-shell {
    padding: 28px 24px 40px;
  }

  .overview-card,
  .summary-grid,
  .toolbar {
    grid-template-columns: 1fr;
  }

  .event-image {
    height: 220px;
  }
}

@media (max-width: 560px) {
  .attendee-shell {
    padding: 24px 16px 32px;
  }

  .page-heading h1 {
    font-size: 32px;
  }

  .overview-card,
  .summary-card {
    padding: 18px;
  }
}
</style>
