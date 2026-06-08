<template>
  <div class="admin-page">
    <section class="admin-heading">
      <div>
        <h1>User Management</h1>
        <p>Search users, update roles, and manage account status.</p>
      </div>
      <el-button :loading="loading" @click="loadUsers">Refresh</el-button>
    </section>

    <section class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="Search name, email, or phone" clearable @keyup.enter="loadUsers" />
      <el-select v-model="filters.role" placeholder="Role" clearable>
        <el-option label="Attendee" value="PARENT" />
        <el-option label="Organizer" value="ORGANIZER" />
        <el-option label="Admin" value="ADMIN" />
      </el-select>
      <el-select v-model="filters.status" placeholder="Status" clearable>
        <el-option label="Active" value="ACTIVE" />
        <el-option label="Deactivated" value="DEACTIVATED" />
      </el-select>
      <el-button type="primary" @click="loadUsers">Search</el-button>
    </section>

    <section class="admin-card">
      <el-table :data="users" v-loading="loading" empty-text="No users found">
        <el-table-column prop="email" label="Email" min-width="230" />
        <el-table-column prop="fullName" label="Full Name" min-width="180" />
        <el-table-column prop="phone" label="Phone" min-width="150" />
        <el-table-column label="Role" width="150">
          <template #default="{ row }">
            <el-select v-model="row.role" size="small" @change="saveUser(row)">
              <el-option label="Attendee" value="PARENT" />
              <el-option label="Organizer" value="ORGANIZER" />
              <el-option label="Admin" value="ADMIN" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="Status" width="160">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" effect="plain">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created" min-width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="190" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="openEditor(row)">Edit</el-button>
            <el-button size="small" :type="row.status === 'ACTIVE' ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 'ACTIVE' ? 'Deactivate' : 'Reactivate' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="editorVisible" title="Edit User" width="520px">
      <el-form label-position="top">
        <el-form-item label="Full Name">
          <el-input v-model="editor.fullName" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="editor.phone" />
        </el-form-item>
        <el-form-item label="Role">
          <el-select v-model="editor.role" class="full-width">
            <el-option label="Attendee" value="PARENT" />
            <el-option label="Organizer" value="ORGANIZER" />
            <el-option label="Admin" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="editor.status" class="full-width">
            <el-option label="Active" value="ACTIVE" />
            <el-option label="Deactivated" value="DEACTIVATED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">Cancel</el-button>
        <el-button type="primary" @click="submitEditor">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAdminUsers, updateAdminUser, updateAdminUserStatus } from '@/api/admin/access4all'

const loading = ref(false)
const users = ref([])
const editorVisible = ref(false)
const filters = reactive({ keyword: '', role: '', status: '' })
const editor = reactive({ id: '', fullName: '', phone: '', role: 'PARENT', status: 'ACTIVE' })

function cleanParams() {
  return Object.fromEntries(Object.entries(filters).filter(([, value]) => value))
}

function formatDate(value) {
  const date = new Date(value)
  return value && !Number.isNaN(date.getTime()) ? date.toLocaleString('en-US') : 'N/A'
}

async function loadUsers() {
  loading.value = true
  try {
    users.value = await listAdminUsers(cleanParams())
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

async function saveUser(row) {
  try {
    const updated = await updateAdminUser(row.id, row)
    Object.assign(row, updated)
    ElMessage.success('User updated')
  } catch (error) {
    console.error(error)
  }
}

function openEditor(row) {
  Object.assign(editor, row)
  editorVisible.value = true
}

async function submitEditor() {
  try {
    await updateAdminUser(editor.id, editor)
    editorVisible.value = false
    ElMessage.success('User updated')
    await loadUsers()
  } catch (error) {
    console.error(error)
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ACTIVE' ? 'DEACTIVATED' : 'ACTIVE'
  try {
    const updated = await updateAdminUserStatus(row.id, nextStatus)
    Object.assign(row, updated)
    ElMessage.success('Status updated')
  } catch (error) {
    console.error(error)
  }
}

onMounted(loadUsers)
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
  gap: 20px;
  align-items: center;
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
.admin-card {
  background: #fff;
  border: 1px solid #e0e8f6;
  border-radius: 12px;
  box-shadow: 0 12px 28px rgba(26, 54, 100, 0.08);
}

.filter-bar {
  margin-bottom: 18px;
  padding: 18px;
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 180px 180px 120px;
  gap: 12px;
}

.admin-card {
  padding: 18px;
}

.full-width {
  width: 100%;
}

@media (max-width: 900px) {
  .admin-page {
    padding: 24px 16px 32px;
  }

  .admin-heading,
  .filter-bar {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
