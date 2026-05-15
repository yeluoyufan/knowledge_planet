import request from '@/utils/request'

export interface Tag {
  id: number
  name: string
  createTime: string
}

export function getAllTags() {
  return request({
    url: '/public/tag/all',
    method: 'get'
  })
}

export interface TagPageQuery {
  pageNum: number
  pageSize: number
  keyword?: string
}

export function getTagPage(params: TagPageQuery) {
  return request({
    url: '/tag/page',
    method: 'get',
    params
  })
}

export function createTag(name: string) {
  return request({
    url: '/tag/create',
    method: 'post',
    data: { name }
  })
}

export function updateTag(id: number, name: string) {
  return request({
    url: '/tag/update',
    method: 'put',
    data: { id, name }
  })
}

export function deleteTag(id: number) {
  return request({
    url: `/tag/${id}`,
    method: 'delete'
  })
}
