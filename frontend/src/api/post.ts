import request from '@/utils/request'
import type { Tag } from './tag'

// 定义帖子数据的类型（根据你的接口文档）
export interface PostVO {
  id: number
  title: string
  content: string // 列表页可能只需要摘要，但接口返回了 content
  boardId: number
  boardName: string
  tags?: Tag[] // 标签列表
  userId: number
  authorName: string
  authorAvatar: string
  viewCount: number
  replyCount: number
  likeCount?: number
  status: number
  isTop?: boolean
  rejectReason?: string
  hasLiked?: boolean
  hasFavorited?: boolean
  createTime: string
}

// 定义分页响应结构
export interface PagePostVO {
  records: PostVO[]
  total: number
  size: number
  current: number
  pages: number
}

// 获取帖子列表的参数类型
export interface PostQuery {
  pageNum: number
  pageSize: number
  boardId?: number // 可选，不传则查询全部
  userId?: number
  keyword?: string
  status?: number
  sortField?: string
  sortOrder?: string
}

export interface PostUpdateRequest {
    id: number
    title: string
    content: string
  }

// 获取帖子列表
export function getPostList(params: PostQuery) {
  return request({
    url: '/post/list',
    method: 'get',
    params // axios 会自动将对象转为 ?pageNum=1&pageSize=10...
  })
}

// 定义发布帖子的参数结构
export interface PostCreateRequest {
    title: string;
    content: string;
    boardId: number;
    tagIds?: number[];
    customTags?: string[];
  }
  
  // 发布帖子接口
  export function createPost(data: PostCreateRequest) {
    return request({
      url: '/post/create',
      method: 'post',
      data
    })
  }

// 获取帖子详情
export function getPostDetail(id: number, increaseView: boolean = true) {
    return request({
      url: `/post/${id}`, // 注意这里是 restful 风格路径
      method: 'get',
      params: { increaseView }
    })
  }
  // 删除帖子
  export function deletePost(id: number) {
    return request({
      url: `/post/${id}`,
      method: 'delete'
    })
  }

  // 编辑帖子
  export function updatePost(data: PostUpdateRequest) {
    return request({
      url: '/post/update',
      method: 'put',
      data
    })
  }

  // 审核帖子
  export function auditPost(postId: number, status: number, rejectReason?: string) {
    return request({
      url: '/post/audit',
      method: 'post',
      params: { postId, status, rejectReason }
    })
  }

  // 获取热门帖子
  export function getHotPosts(boardId?: number, limit: number = 5) {
    return request({
      url: '/post/hot',
      method: 'get',
      params: { boardId, limit }
    })
  }
