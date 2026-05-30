import request from '@/utils/request'

export function getCurrentUserProfile() {
  return request({
    url: '/api/users/me',
    method: 'get'
  })
}

export function listPosts(query) {
  return request({
    url: '/api/posts',
    method: 'get',
    params: query
  })
}

export function getPublicPosts(query) {
  return request({
    url: '/api/posts/public',
    method: 'get',
    params: query
  })
}

export function getPost(id) {
  return request({
    url: `/api/posts/${id}`,
    method: 'get'
  })
}

export function getPostsByUser(userId) {
  return request({
    url: `/api/posts/user/${userId}`,
    method: 'get'
  })
}

export function getPostsByLocation(locationId) {
  return request({
    url: `/api/posts/location/${locationId}`,
    method: 'get'
  })
}

export function getPostsByEvent(eventId) {
  return request({
    url: `/api/posts/event/${eventId}`,
    method: 'get'
  })
}

export function getRegistrationsByUser(userId) {
  return request({
    url: `/api/registrations/user/${userId}`,
    method: 'get'
  })
}

export function searchPosts(keyword) {
  return request({
    url: '/api/posts/search',
    method: 'get',
    params: { keyword }
  })
}

export function createPost(data) {
  return request({
    url: '/api/posts',
    method: 'post',
    data
  })
}

export function updatePost(id, data) {
  return request({
    url: `/api/posts/${id}`,
    method: 'put',
    data,
    headers: {
      permissionMessage: 'You do not have permission to perform this action.'
    }
  })
}

export function deletePost(id) {
  return request({
    url: `/api/posts/${id}`,
    method: 'delete',
    headers: {
      permissionMessage: 'You do not have permission to perform this action.'
    }
  })
}

export function getPostImages(postId) {
  return request({
    url: `/api/posts/${postId}/images`,
    method: 'get'
  })
}

export function uploadPostImage(postId, data) {
  return request({
    url: `/api/posts/${postId}/images`,
    method: 'post',
    data,
    headers: {
      repeatSubmit: false
    }
  })
}

export function deletePostImage(postId, imageId) {
  return request({
    url: `/api/posts/${postId}/images/${imageId}`,
    method: 'delete'
  })
}

export function getPostReviews(postId) {
  return request({
    url: `/api/posts/${postId}/reviews`,
    method: 'get'
  })
}

export function createPostReview(postId, data) {
  return request({
    url: `/api/posts/${postId}/reviews`,
    method: 'post',
    data
  })
}

export function updatePostReview(postId, reviewId, data) {
  return request({
    url: `/api/posts/${postId}/reviews/${reviewId}`,
    method: 'put',
    data,
    headers: {
      permissionMessage: 'You do not have permission to perform this action.'
    }
  })
}

export function deletePostReview(postId, reviewId) {
  return request({
    url: `/api/posts/${postId}/reviews/${reviewId}`,
    method: 'delete',
    headers: {
      permissionMessage: 'You do not have permission to perform this action.'
    }
  })
}
