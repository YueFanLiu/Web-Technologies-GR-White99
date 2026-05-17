import request from '@/utils/request'

export function getCurrentUserProfile() {
  return request({
    url: '/api/users/me',
    method: 'get'
  })
}

export function listPosts() {
  return request({
    url: '/api/posts',
    method: 'get'
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
    data
  })
}

export function deletePost(id) {
  return request({
    url: `/api/posts/${id}`,
    method: 'delete'
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
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function deletePostImage(postId, imageId) {
  return request({
    url: `/api/posts/${postId}/images/${imageId}`,
    method: 'delete'
  })
}
