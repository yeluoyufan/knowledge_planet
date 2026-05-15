import request from '@/utils/request'

// 1. 板块相关
export interface BoardRequest {
  name: string
  description?: string
  icon?: string
  moderatorId?: number | null
  sort?: number
}

// 添加板块
export function addBoard(data: BoardRequest) {
  return request({
    url: '/admin/board/add',
    method: 'post',
    data
  })
}

// 修改板块
export function updateBoard(id: number, data: BoardRequest) {
  return request({
    url: `/admin/board/${id}`,
    method: 'put',
    data
  })
}

// 删除板块
export function deleteBoard(id: number) {
  return request({
    url: `/admin/board/${id}`,
    method: 'delete'
  })
}

// 帖子相关

// 置顶/取消置顶 (接口描述暗示是切换开关，或者是POST动作)
export function toggleTopPost(id: number) {
  return request({
    url: `/admin/post/top/${id}`,
    method: 'post'
  })
}

// 管理员删除帖子
export function deletePostAdmin(id: number) {
  return request({
    url: `/admin/post/${id}`,
    method: 'delete'
  })
}

export interface ModeratorAppointRequest {
    userId: number
    boardId: number
}

export function appointModerator(data: ModeratorAppointRequest) {
    return request({
      url: 'admin/appoint',
      method: 'post',
      data
    })
}

// 切换用户状态 (禁用/启用)
export function toggleUserStatus(id: number) {
  return request({
    url: `/admin/user/status/${id}`,
    method: 'post'
  })
}

export interface CommentManageVO {
  id: number
  postId: number
  postTitle: string
  boardId: number
  content: string
  userId: number
  authorName: string
  authorAvatar: string
  createTime: string
}

export interface CommentManageQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  postId?: number
  searchField?: string
}

export function getCommentManagePage(params: CommentManageQuery) {
  return request({
    url: '/admin/comment/list',
    method: 'get',
    params
  })
}

export function deleteCommentManage(id: number) {
  return request({
    url: `/admin/comment/${id}`,
    method: 'delete'
  })
}
