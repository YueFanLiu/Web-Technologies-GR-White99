<template>
  <div class="page-wrapper">
    <el-container class="full-container">
      <!-- 主体三栏布局 -->
      <el-container class="main-container">
        <!-- 左侧筛选栏 -->
        <el-aside width="240px" class="aside-left">
          <div class="filters-panel">
            <el-collapse accordion>
              <el-collapse-item title="Filters" name="1">
                <!-- 位置筛选 -->
                <div class="filter-item">
                  <label>Location</label>
                  <el-select v-model="form.location" placeholder="Select location">
                    <el-option label="Cityville" value="cityville"></el-option>
                  </el-select>
                </div>

                <!-- 日期筛选 -->
                <div class="filter-item">
                  <label>Date</label>
                  <el-select v-model="form.dateRange" placeholder="Select date range">
                    <el-option label="This Week" value="week"></el-option>
                  </el-select>
                  <div class="date-calendar">
                    <el-button-group>
                      <el-button size="small">23</el-button>
                      <el-button size="small">24</el-button>
                      <el-button size="small">25</el-button>
                      <el-button size="small" type="primary">26</el-button>
                      <el-button size="small">28</el-button>
                      <el-button size="small">29</el-button>
                    </el-button-group>
                  </div>
                </div>

                <!-- 活动类型 -->
                <div class="filter-item">
                  <label>Activity Type</label>
                  <el-checkbox-group v-model="form.activityType">
                    <el-checkbox label="all">All</el-checkbox>
                    <el-checkbox label="outdoor">Outdoor</el-checkbox>
                    <el-checkbox label="class">Class</el-checkbox>
                    <el-checkbox label="workshop">Workshop</el-checkbox>
                  </el-checkbox-group>
                </div>

                <!-- 无障碍选项 -->
                <div class="filter-item">
                  <label>Accessibility Options</label>
                  <el-checkbox-group v-model="form.accessibility">
                    <el-checkbox label="wheelchair">Wheelchair Accessible</el-checkbox>
                    <el-checkbox label="elevator">Elevator</el-checkbox>
                    <el-checkbox label="restroom">Accessible Restroom</el-checkbox>
                    <el-checkbox label="low-noise">Low Noise Level</el-checkbox>
                  </el-checkbox-group>
                </div>

                <el-button type="primary" style="width: 100%; margin-top: 15px;" @click="fetchEvents">Apply Filters</el-button>
              </el-collapse-item>
            </el-collapse>

            <div class="suggestion-box">
              Can't decide? Here are some events you might like!
            </div>
          </div>
        </el-aside>

        <!-- 中间活动列表 -->
        <el-main class="main-content">
          <div class="activity-header">
            <h3>Activity List</h3>
            <div class="header-controls">
              <el-button type="success" icon="el-icon-plus" @click="openCreateDialog">
                Create Activity
              </el-button>

              <el-button-group>
                <el-button icon="el-icon-arrow-left" size="small"></el-button>
                <el-button size="small">Today</el-button>
                <el-button icon="el-icon-arrow-right" size="small"></el-button>
              </el-button-group>
            </div>
          </div>

          <!-- 活动卡片列表（模拟数据） -->
          <div class="activity-list" v-loading="loading">
            <div class="activity-card" v-for="(item, index) in activityList" :key="index">
              <div class="delete-btn">
                <el-button type="danger" size="mini" @click.stop="handleDelete(item.id)">Delete</el-button>
                <el-button type="primary" size="mini" @click.stop="openEditDialog(item)">Edit</el-button>
              </div>
              <div class="card-image">
                <img :src="item.image" alt="activity" />
              </div>
              <div class="card-info">
                <div class="card-header">
                  <h4>{{ item.title }}</h4>
                  <el-tag type="primary" v-if="item.recommended">Recommended</el-tag>
                </div>
                <p><i class="el-icon-date"></i> {{ item.date }}</p>
                <p><i class="el-icon-time"></i> {{ item.time }}</p>
                <p><i class="el-icon-location"></i> {{ item.locationName }}</p>
                <p><i class="el-icon-location-outline"></i> {{ item.locationAddress }}</p>

                <div class="card-footer">
                  <div class="rating">
                    <el-rate v-model="item.rating" disabled show-score text-color="#ff9900"></el-rate>
                    <span>{{ item.reviews }} reviews</span>
                  </div>
                  <el-button type="primary" @click="goToDetails(item)">View Details</el-button>
                </div>
              </div>
            </div>
            <el-empty v-if="!loading && activityList.length === 0" description="No events found" />
          </div>
        </el-main>

        <!-- 右侧地图 + 热门活动 -->
        <el-aside width="320px" class="aside-right">
          <!-- 地图区域 -->
          <div class="map-container">
            <img src="https://picsum.photos/id/101/320/200" alt="map" class="map-image" />
            <div class="map-controls">
              <el-button icon="el-icon-plus" circle size="small"></el-button>
            </div>
          </div>

          <!-- 热门活动 -->
          <div class="popular-events">
            <div class="section-header">
              <h4>Popular Events</h4>
              <el-dropdown>
                <el-button icon="el-icon-more" size="small" text></el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item>View All</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </div>

            <div class="event-item" v-for="(item, index) in popularEvents" :key="index">
              <img :src="item.image" alt="event" class="event-thumb" />
              <div class="event-info">
                <p class="event-title">{{ item.title }}</p>
                <p class="event-meta">{{ item.date }}</p>
                <p class="event-meta">{{ item.time }}</p>
              </div>
            </div>
          </div>
        </el-aside>
      </el-container>
    </el-container>
  </div>

  <el-dialog v-model="dialogVisible" :title="isEdit ? 'Edit Activity' : 'Create New Activity'" width="500px">
    <el-form :model="eventForm" label-width="120px">
      <el-form-item label="Title">
        <el-input v-model="eventForm.title" placeholder="Please enter activity title" />
      </el-form-item>

      <el-form-item label="Description">
        <el-input v-model="eventForm.description" type="textarea" rows="3" />
      </el-form-item>

      <el-form-item label="Category">
        <el-select v-model="eventForm.category" placeholder="select category">
          <el-option label="concert" value="concert" />
          <el-option label="workshop" value="workshop" />
          <el-option label="outdoor" value="outdoor" />
        </el-select>
      </el-form-item>

      <el-form-item label="Start Time">
        <el-date-picker v-model="eventForm.startTime" type="datetime" 
          placeholder="Select start time" value-format="YYYY-MM-DDTHH:mm:ss" 
          format="YYYY-MM-DD HH:mm:ss" style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="End Time">
        <el-date-picker v-model="eventForm.endTime" type="datetime" 
          placeholder="Select end time" value-format="YYYY-MM-DDTHH:mm:ss" 
          format="YYYY-MM-DD HH:mm:ss" style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="Capacity">
        <el-input v-model="eventForm.capacity" type="number" />
      </el-form-item>
      <el-form-item label="Price">
        <el-input v-model="eventForm.price" type="number" />
      </el-form-item>

      <el-form-item label="Location">
        <el-select 
          v-model="eventForm.locationId" 
          placeholder="Select a location"
          style="width: 100%"
        >
          <el-option
            v-for="loc in locationOptions"
            :key="loc.id"
            :label="`${loc.name} - ${loc.address || loc.city || ''}`"
            :value="loc.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">Cancel</el-button>
      <el-button type="primary" @click="isEdit ? handleUpdateEvent() : handleCreateEvent()">
        {{ isEdit ? 'Confirm Update' : 'Confirm Create' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listEvent, createEvent, delEvent, updateEvent } from '@/api/events/index.js'
import { listLocations } from '@/api/location/index.js'
import { useRoute } from 'vue-router'
const route = useRoute()
const router = useRouter()
const goToDetails = (item) => {
  router.push({
    path: '/product/eventDetails',
    query: { id: item.id }
  })
}
const searchKeyword = ref('')
const locationOptions = ref([])
// 筛选表单
const form = reactive({
  location: 'cityville',
  dateRange: 'week',
  activityType: [],
  accessibility: []
})

const loading = ref(false)
const activityList = ref([])
const popularEvents = ref([])
const isEdit = ref(false)
const currentEventId = ref(null)

// 格式化时间、日期、图片
function formatDate(value) {
  if (!value) return 'Date TBA'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date TBA'
  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}


function formatTime(startValue, endValue) {
  if (!startValue) return 'Time TBA'
  const start = new Date(startValue)
  if (Number.isNaN(start.getTime())) return 'Time TBA'

  const options = { hour: 'numeric', minute: '2-digit' }
  const startText = start.toLocaleTimeString('en-US', options)
  if (!endValue) return startText
  const end = new Date(endValue)
  if (Number.isNaN(end.getTime())) return startText
  return `${startText} - ${end.toLocaleTimeString('en-US', options)}`
}

function fallbackImage(id, width = 220, height = 150) {
  const seed = encodeURIComponent(id || 'event')
  return `https://picsum.photos/seed/${seed}/${width}/${height}`
}

// 数据映射
function mapEvent(event, index) {
  const location = event.location || {}
  const image = event.coverImageUrl || event.imageUrls?.[0] || fallbackImage(event.id || index)

  return {
    id: event.id,
    image,
    title: event.title || 'Untitled event',
    recommended: index === 0,
    date: formatDate(event.startTime),
    time: formatTime(event.startTime, event.endTime),
    locationName: location.name || (event.isVirtual ? 'Online event' : 'Location TBA'),
    locationAddress: [location.address, location.city, location.country].filter(Boolean).join(', ') || 'Address TBA',
    rating: Number(event.averageRating || 0),
    reviews: Number(event.reviewCount || 0),
    description: event.description || '',
    category: event.category || '',
    startTime: event.startTime || '',
    endTime: event.endTime || '',
    capacity: event.capacity || 10,
    price: event.price || 0,
    location: location
  }
}
//获取locaiton列表
const fetchLocations = async () => {
  try {
    const res = await listLocations()
    locationOptions.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('Failed to load locations:', error)
  }
}

// 获取活动列表
const fetchEvents = async () => {
  loading.value = true
  try {
    const params = {
      limit: 1000,
      upcomingOnly: false
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (form.category) params.category = form.category
    if (form.keyword) params.keyword = form.keyword
    if (form.status) params.status = form.status

    const res = await listEvent(params)
    const events = Array.isArray(res) ? res : []
    const mappedEvents = events.map(mapEvent)
    activityList.value = mappedEvents
    popularEvents.value = mappedEvents.slice(0, 3)
  } catch (error) {
    console.error('error:', error)
    ElMessage.error('Failed to load events')
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const eventForm = reactive({
  title: '',
  description: '',
  category: '',
  startTime: '',
  endTime: '',
  capacity: 10,
  price: 0,
  locationId: ''
})

const openEditDialog = (item) => {
  isEdit.value = true
  currentEventId.value = item.id
  
  eventForm.title = item.title
  eventForm.description = item.description || ''
  eventForm.category = item.category || ''
  eventForm.startTime = item.startTime || ''
  eventForm.endTime = item.endTime || ''
  eventForm.capacity = item.capacity || 10
  eventForm.price = item.price || 0
  eventForm.locationId = item.location?.id || ''
  
  dialogVisible.value = true
}

const openCreateDialog = () => {
  isEdit.value = false
  currentEventId.value = null
  Object.assign(eventForm, {
    title: '', description: '', category: '',
    startTime: '', endTime: '', capacity: 10,
    price: 0, locationId: ''
  })
  dialogVisible.value = true
}


// 更新活动
const handleUpdateEvent = async () => {
  try {
    const postData = {
      title: eventForm.title,
      description: eventForm.description,
      category: eventForm.category,
      startTime: eventForm.startTime,
      endTime: eventForm.endTime,
      capacity: eventForm.capacity,
      price: eventForm.price,
      isVirtual: false,
      status: "PUBLISHED",
      locationId: eventForm.locationId
    }
    await updateEvent(currentEventId.value, postData)
    ElMessage.success('Updated successfully!')
    dialogVisible.value = false
    await fetchEvents()
  } catch (error) {
    console.error('Update failed:', error)
    ElMessage.error('Update failed')
  }
}

// 删除活动
const handleDelete = async (id) => {
  ElMessageBox.confirm(
    'Confirm to delete this event?',
    'Warning',
    {
      confirmButtonText: 'Confirm',
      cancelButtonText: 'Cancel',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await delEvent(id)
      ElMessage.success('Deleted successfully!')
      fetchEvents()
    } catch (err) {
      ElMessage.error('Delete failed')
      console.error(err)
    }
  }).catch(() => {
    ElMessage.info('Canceled')
  })
}

const handleCreateEvent = async () => {
  try {
    const postData = {
      title: eventForm.title,
      description: eventForm.description,
      category: eventForm.category,
      startTime: eventForm.startTime,
      endTime: eventForm.endTime,
      capacity: eventForm.capacity,
      price: eventForm.price,
      isVirtual: false,
      status: "PUBLISHED",
      locationId: eventForm.locationId
    }
    await createEvent(postData)
    ElMessage.success('Activity created successfully!')
    dialogVisible.value = false
    fetchEvents()
  } catch (error) {
    console.error('Create failed:', error)
    ElMessage.error('Creation failed')
  }
}

onMounted(() => {
  if (route.query.keyword) {
    searchKeyword.value = route.query.keyword
  }
  fetchEvents()
  fetchLocations()
})
</script>

<style scoped lang="scss">
.page-wrapper {
  width: 100%;
  height: 100vh;
  background-color: #f0f4ff;
}

.main-container {
  height: calc(100vh - 60px);
}
.aside-left {
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
  .filters-panel {
    padding: 15px;
    height: 100%;
    overflow-y: auto;
    .filter-item {
      margin: 15px 0;
      label {
        display: block;
        margin-bottom: 8px;
        font-size: 14px;
        color: #606266;
      }
      .date-calendar {
        margin-top: 8px;
      }
    }
    .suggestion-box {
      margin-top: 20px;
      padding: 12px;
      background-color: #ecf5ff;
      border-radius: 4px;
      font-size: 13px;
      color: #409eff;
    }
  }
}
.main-content {
  padding: 20px;
  .activity-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h3 {
      margin: 0;
      font-size: 18px;
    }
  }
  .activity-list {
    .activity-card {
      background-color: #fff;
      border-radius: 8px;
      padding: 15px;
      margin-bottom: 15px;
      display: flex;
      gap: 15px;
      box-shadow: 0 2px 12px rgba(0,0,0,0.05);
      .card-image {
        width: 220px;
        img {
          width: 100%;
          height: 150px;
          object-fit: cover;
          border-radius: 6px;
        }
      }
      .card-info {
        flex: 1;
        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 10px;
          h4 {
            margin: 0;
            font-size: 18px;
          }
        }
        p {
          margin: 6px 0;
          font-size: 14px;
          color: #606266;
          i {
            margin-right: 6px;
            color: #409eff;
          }
        }
        .card-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: 15px;
          .rating {
            display: flex;
            align-items: center;
            gap: 8px;
            span {
              font-size: 13px;
              color: #909399;
            }
          }
        }
      }
    }
  }
}
.aside-right {
  background-color: #fff;
  border-left: 1px solid #e4e7ed;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  .map-container {
    position: relative;
    .map-image {
      width: 100%;
      height: 220px;
      border-radius: 8px;
      object-fit: cover;
    }
    .map-controls {
      position: absolute;
      right: 10px;
      top: 10px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
  }
  .popular-events {
    flex: 1;
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      h4 {
        margin: 0;
        font-size: 16px;
      }
    }
    .event-item {
      display: flex;
      gap: 10px;
      margin-bottom: 12px;
      .event-thumb {
        width: 80px;
        height: 60px;
        border-radius: 4px;
        object-fit: cover;
      }
      .event-info {
        .event-title {
          margin: 0;
          font-size: 14px;
          font-weight: 500;
        }
        .event-meta {
          margin: 2px 0;
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
}

.delete-btn {
  display: flex;
  gap: 5px;
  position: absolute;
  top: 10px;
  right: 15px;
  z-index: 10;
}
.activity-card {
  position: relative;
}
</style>