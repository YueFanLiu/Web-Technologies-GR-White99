import request from '@/utils/request'

export function createEventDirectChat(eventId, userId, permissionMessage = 'You can only message confirmed attendees of your own event.') {
  return request({
    url: `/api/chats/events/${eventId}/direct`,
    method: 'post',
    data: { userId },
    headers: {
      permissionMessage
    }
  })
}

export function getChats() {
  return request({
    url: '/api/chats',
    method: 'get'
  })
}

export function getChatMessages(chatId, query) {
  return request({
    url: `/api/chats/${chatId}/messages`,
    method: 'get',
    params: query || undefined
  })
}

export function sendChatMessage(chatId, content) {
  return request({
    url: `/api/chats/${chatId}/messages`,
    method: 'post',
    data: {
      content,
      messageType: 'TEXT'
    },
    headers: {
      permissionMessage: 'You do not have permission to send messages in this chat.'
    }
  })
}

export function markChatRead(chatId) {
  return request({
    url: `/api/chats/${chatId}/read`,
    method: 'post',
    data: {},
    headers: {
      showErrorMessage: false
    }
  })
}
