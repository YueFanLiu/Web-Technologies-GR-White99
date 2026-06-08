<template>
  <div class="admin-page">
    <section class="admin-heading">
      <div>
        <h1>Platform Reports</h1>
        <p>Usage, bookings, ticket sales, and feedback statistics.</p>
      </div>
      <el-button :loading="loading" type="primary" @click="loadReport">Refresh</el-button>
    </section>

    <section class="filter-bar">
      <el-date-picker v-model="range" type="daterange" start-placeholder="From" end-placeholder="To" value-format="YYYY-MM-DD" />
      <el-button @click="loadReport">Apply</el-button>
    </section>

    <section class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </article>
    </section>

    <section class="report-grid">
      <article class="admin-card">
        <h2>Users by Role</h2>
        <el-table :data="report.usersByRole || []" size="small">
          <el-table-column prop="label" label="Role" />
          <el-table-column prop="count" label="Count" width="100" />
        </el-table>
      </article>
      <article class="admin-card">
        <h2>Users by Status</h2>
        <el-table :data="report.usersByStatus || []" size="small">
          <el-table-column prop="label" label="Status" />
          <el-table-column prop="count" label="Count" width="100" />
        </el-table>
      </article>
    </section>

    <section class="admin-card">
      <h2>Top Events</h2>
      <el-tabs>
        <el-tab-pane label="Registrations">
          <el-table :data="report.topEventsByRegistrations || []" size="small">
            <el-table-column prop="title" label="Event" min-width="220" />
            <el-table-column prop="count" label="Count" width="120" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="Revenue">
          <el-table :data="report.topEventsByRevenue || []" size="small">
            <el-table-column prop="title" label="Event" min-width="220" />
            <el-table-column label="Revenue" width="140">
              <template #default="{ row }">{{ formatMoney(row.revenue) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="Rating">
          <el-table :data="report.topEventsByRating || []" size="small">
            <el-table-column prop="title" label="Event" min-width="220" />
            <el-table-column label="Rating" width="120">
              <template #default="{ row }">{{ row.rating ? Number(row.rating).toFixed(1) : 'N/A' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getPlatformUsageReport } from '@/api/admin/access4all'

const loading = ref(false)
const range = ref([])
const report = ref({})

function formatMoney(value) {
  const amount = Number(value || 0)
  return amount === 0 ? 'Free' : `S$${amount.toFixed(2)}`
}

const metrics = computed(() => [
  { label: 'Users', value: report.value.totalUsers || 0 },
  { label: 'Events', value: report.value.totalEvents || 0 },
  { label: 'Posts', value: report.value.totalPosts || 0 },
  { label: 'Registrations', value: report.value.totalRegistrations || 0 },
  { label: 'Ticket Sales', value: formatMoney(report.value.ticketSalesTotal) },
  { label: 'Avg Rating', value: report.value.averageFeedbackRating ? Number(report.value.averageFeedbackRating).toFixed(1) : 'N/A' }
])

async function loadReport() {
  loading.value = true
  try {
    const params = {}
    if (Array.isArray(range.value) && range.value.length === 2) {
      params.from = range.value[0]
      params.to = range.value[1]
    }
    report.value = await getPlatformUsageReport(params)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(loadReport)
</script>

<style scoped lang="scss">
.admin-page {
  min-height: 100vh;
  padding: 32px 40px 48px;
  background: #f7faff;
}

.admin-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 22px;
}

.admin-heading h1 {
  margin: 0;
  color: #071a47;
  font-size: 40px;
  font-weight: 800;
}

.admin-heading p {
  margin: 8px 0 0;
  color: #667091;
}

.filter-bar,
.admin-card,
.metric-card {
  background: #fff;
  border: 1px solid #e0e8f6;
  border-radius: 12px;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.08);
}

.filter-bar {
  margin-bottom: 18px;
  padding: 18px;
  display: flex;
  gap: 12px;
}

.metric-grid {
  margin-bottom: 18px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 20px;
}

.metric-card span {
  color: #667091;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  color: #071a47;
  font-size: 28px;
}

.report-grid {
  margin-bottom: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.admin-card {
  padding: 20px;
}

.admin-card h2 {
  margin: 0 0 14px;
  color: #071a47;
}

@media (max-width: 1000px) {
  .metric-grid,
  .report-grid {
    grid-template-columns: 1fr;
  }

  .admin-page {
    padding: 24px 16px 32px;
  }
}
</style>
