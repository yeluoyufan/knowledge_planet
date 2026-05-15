import request from '@/utils/request'

export function toggleLike(postId: number | string) {
  return request({
    url: `/like/toggle/${postId}`,
    method: 'post'
  })
}

export function getLikeStatus(postId: number | string) {
  return request({
    url: `/like/status/${postId}`,
    method: 'get'
  })
}
