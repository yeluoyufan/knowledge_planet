import request from '@/utils/request'
import type { PostVO } from './post'

export function toggleFavorite(postId: number) {
  return request({
    url: `/favorite/toggle/${postId}`,
    method: 'post'
  })
}

export function getFavoriteStatus(postId: number) {
  return request({
    url: `/favorite/status/${postId}`,
    method: 'get'
  })
}

export interface FavoritePage {
  records: PostVO[]
  total: number
  size: number
  current: number
  pages: number
}

export function getFavoriteList(params: { pageNum: number; pageSize: number }) {
  return request({
    url: '/favorite/list',
    method: 'get',
    params
  })
}

