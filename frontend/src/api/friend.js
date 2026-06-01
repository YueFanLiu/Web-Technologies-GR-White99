import request from '@/utils/request'

export function getFriends() {
  return request({
    url: '/api/friends',
    method: 'get'
  })
}

export function getFriendRecommendations(limit = 6) {
  return request({
    url: '/api/friends/recommendations',
    method: 'get',
    params: {
      limit
    }
  })
}

export function removeFriend(friendUserId) {
  return request({
    url: `/api/friends/${friendUserId}`,
    method: 'delete'
  })
}

export function getIncomingFriendRequests() {
  return request({
    url: '/api/friend-requests/incoming',
    method: 'get'
  })
}

export function getOutgoingFriendRequests() {
  return request({
    url: '/api/friend-requests/outgoing',
    method: 'get'
  })
}

export function sendFriendRequest(addresseeId) {
  return request({
    url: '/api/friend-requests',
    method: 'post',
    data: {
      addresseeId
    }
  })
}

export function acceptFriendRequest(requestId) {
  return request({
    url: `/api/friend-requests/${requestId}/accept`,
    method: 'post'
  })
}

export function rejectFriendRequest(requestId) {
  return request({
    url: `/api/friend-requests/${requestId}/reject`,
    method: 'post'
  })
}

export function cancelFriendRequest(requestId) {
  return request({
    url: `/api/friend-requests/${requestId}`,
    method: 'delete'
  })
}
