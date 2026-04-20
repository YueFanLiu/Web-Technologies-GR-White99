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

                <el-button type="primary" style="width: 100%; margin-top: 15px;">Apply Filters</el-button>
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
              <el-button-group>
                <el-button icon="el-icon-arrow-left" size="small"></el-button>
                <el-button size="small">Today</el-button>
                <el-button icon="el-icon-arrow-right" size="small"></el-button>
              </el-button-group>
            </div>
          </div>

          <!-- 活动卡片列表（模拟数据） -->
          <div class="activity-list">
            <div class="activity-card" v-for="(item, index) in activityList" :key="index">
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
                    <el-rate v-model="item.rating" disabled show-score text-color="#ff9900" score-template="4.8"></el-rate>
                    <span>{{ item.reviews }} reviews</span>
                  </div>
                  <el-button type="primary" @click="goToDetails(item)">View Details</el-button>
                </div>
              </div>
            </div>
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
</template>

<script setup>
import { ref, reactive } from 'vue'
import {useRouter} from 'vue-router'

const router = useRouter()
const goToDetails = (item) =>{
  router.push('/product/eventDetails')
}
// 筛选表单
const form = reactive({
  location: 'cityville',
  dateRange: 'week',
  activityType: [],
  accessibility: []
})

// 中间活动列表模拟数据
const activityList = ref([
  {
    image: 'https://picsum.photos/id/102/200/150',
    title: 'Adventure Playground Day',
    recommended: true,
    date: 'Saturday, April 25, 2026',
    time: '10:00 AM - 2:00 PM',
    locationName: 'Greenwood Park',
    locationAddress: '123 Park Lane, Cityville',
    rating: 4.8,
    reviews: 18
  },
  {
    image: 'https://picsum.photos/id/103/200/150',
    title: 'Music & Fun Meetup',
    recommended: false,
    date: 'Sunday, April 26, 2026',
    time: '3:00 PM',
    locationName: 'Harmony Community Center',
    locationAddress: '456 Oak St, Cityville',
    rating: 4.7,
    reviews: 12
  },
  {
    image: 'https://picsum.photos/id/104/200/150',
    title: 'Inclusive Family Picnic',
    recommended: false,
    date: 'Saturday, May 2, 2026',
    time: '11:00 AM - 1:00 PM',
    locationName: 'Meadow View Park',
    locationAddress: '789 River Rd, Cityville',
    rating: 4.5,
    reviews: 8
  }
])

// 右侧热门活动模拟数据
const popularEvents = ref([
  {
    image: 'https://picsum.photos/id/102/80/60',
    title: 'Adventure Playground Day',
    date: 'Saturday, April 25, 2026',
    time: '10:00 AM - 2:00 PM'
  },
  {
    image: 'https://picsum.photos/id/104/80/60',
    title: 'Inclusive Family Picnic',
    date: 'Saturday, May 2, 2026',
    time: '11:00 AM - 1:00 PM'
  },
  {
    image: 'https://picsum.photos/id/105/80/60',
    title: 'Sensory-Friendly Art Workshop',
    date: 'Sunday, May 3, 2026',
    time: '2:00 PM - 4:00 PM'
  }
])
</script>

<style scoped lang="scss">


.page-wrapper {
  width: 100%;
  height: 100vh;
  background-color: #f0f4ff;
}
.full-container {
  height: 100%;
  width: 100%;
}
.page-header {
  padding: 0 !important; 
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  .logo {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    color: #303133;
  }
  .header-menu {
    border-bottom: none;
  }
}
.main-container {
  height: calc(100vh - 60px);
  padding: 0px 0px;
}
.aside-left {
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
  padding: 0px;
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
</style>