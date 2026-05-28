<template>
  <div class="activity-map">
    <div v-if="unavailable" class="map-state">Map unavailable</div>
    <div v-else ref="mapEl" class="leaflet-map"></div>
    <div v-if="!unavailable && markers.length === 0" class="map-state map-state--overlay">
      No mapped events
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

const props = defineProps({
  markers: {
    type: Array,
    default: () => []
  },
  unavailable: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['view-details'])

const mapEl = ref(null)
let map = null
let markerLayer = null

L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow
})

function getDefaultCenter() {
  return [48.8566, 2.3522]
}

function createPopup(marker) {
  const container = document.createElement('div')
  container.className = 'activity-map-popup'

  const title = document.createElement('strong')
  title.textContent = marker.title
  container.appendChild(title)

  const address = document.createElement('p')
  address.textContent = marker.address || marker.venue || 'Address TBA'
  container.appendChild(address)

  const button = document.createElement('button')
  button.type = 'button'
  button.textContent = 'View Details'
  button.addEventListener('click', () => emit('view-details', marker))
  container.appendChild(button)

  return container
}

function renderMarkers() {
  if (!map || props.unavailable) {
    return
  }

  markerLayer.clearLayers()

  const bounds = []
  props.markers.forEach((marker) => {
    const latLng = [marker.latitude, marker.longitude]
    bounds.push(latLng)
    L.marker(latLng)
      .bindPopup(createPopup(marker))
      .addTo(markerLayer)
  })

  if (bounds.length > 1) {
    map.fitBounds(bounds, { padding: [24, 24], maxZoom: 14 })
  } else if (bounds.length === 1) {
    map.setView(bounds[0], 13)
  } else {
    map.setView(getDefaultCenter(), 11)
  }
}

async function initMap() {
  if (map || props.unavailable || !mapEl.value) {
    return
  }

  await nextTick()
  map = L.map(mapEl.value, {
    zoomControl: true,
    attributionControl: false
  }).setView(getDefaultCenter(), 11)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(map)

  markerLayer = L.layerGroup().addTo(map)
  renderMarkers()
  setTimeout(() => map?.invalidateSize(), 0)
}

onMounted(initMap)

watch(
  () => props.unavailable,
  (value) => {
    if (!value) {
      initMap()
    }
  }
)

watch(
  () => props.markers,
  () => {
    renderMarkers()
    setTimeout(() => map?.invalidateSize(), 0)
  },
  { deep: true }
)

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
    markerLayer = null
  }
})
</script>

<style scoped lang="scss">
.activity-map {
  position: relative;
  width: 100%;
  height: 220px;
  overflow: hidden;
  border-radius: 8px;
  background: #eef2f7;
}

.leaflet-map {
  width: 100%;
  height: 100%;
}

.map-state {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #606266;
  font-size: 14px;
  background: #f5f7fa;
}

.map-state--overlay {
  position: absolute;
  inset: 0;
  height: auto;
  background: rgba(245, 247, 250, 0.86);
  pointer-events: none;
}

:global(.activity-map-popup) {
  min-width: 150px;
}

:global(.activity-map-popup strong) {
  display: block;
  margin-bottom: 4px;
  color: #303133;
}

:global(.activity-map-popup p) {
  margin: 0 0 8px;
  color: #606266;
  font-size: 12px;
}

:global(.activity-map-popup button) {
  border: 0;
  border-radius: 4px;
  padding: 5px 8px;
  color: #fff;
  background: #409eff;
  cursor: pointer;
}
</style>
