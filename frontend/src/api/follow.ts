import request from '@/utils/request'

export function toggleFollow(userId: number) {
  return request({
    url: `/follow/toggle/${userId}`,
    method: 'post'
  })
}

export function getFollowStatus(userId: number) {
  return request({
    url: `/follow/status/${userId}`,
    method: 'get'
  })
}

export interface HotAuthor {
  id: number
  username: string
  nickname: string
  avatar: string
  fansCount: number
}

export function getHotAuthors(limit: number = 3) {
  return request({
    url: '/follow/hot',
    method: 'get',
    params: { limit }
  })
}

// 获取用户粉丝列表
export function getFansList(userId: number, params: { pageNum: number, pageSize: number }) {
  return request({
    url: `/follow/fans/${userId}`,
    method: 'get',
    params
  })
}

// 获取用户关注列表
export function getFolloweeList(userId: number, params: { pageNum: number, pageSize: number }) {
  return request({
    url: `/follow/followees/${userId}`,
    method: 'get',
    params
  })
}

