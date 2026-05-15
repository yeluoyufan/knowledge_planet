import request from '@/utils/request'

export interface UserVO {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  bio?: string
  role: string
  status: number
  managedBoardId: number
    followCount?: number
    fansCount?: number
  createTime: string
}

export interface UserUpdateRequest {
  nickname: string
  avatar?: string
  email?: string
  bio?: string
}

// 获取用户信息
export function getUserInfo(id: number) {
  return request({
    url: `/user/${id}`,
    method: 'get'
  })
}

// 修改个人信息
export function updateUserInfo(data: UserUpdateRequest) {
  return request({
    url: '/user/update',
    method: 'put',
    data
  })
}

export interface UserQuery {
    pageNum: number
    pageSize: number
    keyword?: string // 用于搜索用户名或昵称
    managedBoardId?: number // 按板块筛选板主
    role?: string // 按角色筛选
  }

// 分页获取用户列表 (管理员)
export function getUserList(params: UserQuery) {
  return request({
    url: '/user/list',
    method: 'get',
    params
  })
}

// 公开搜索用户
export function searchUsers(params: { pageNum: number; pageSize: number; keyword?: string }) {
  return request({
    url: '/user/search',
    method: 'get',
    params
  })
}

// 修改密码 请求体
export interface PasswordUpdateRequest {
    oldPassword: string
    newPassword: string
    confirmPassword: string
  }

  // 修改密码
  export function updatePassword(data: PasswordUpdateRequest) {
    return request({
      url: '/user/password',
      method: 'put',
      data
    })
  }

  // 获取用户点赞过的帖子
  export function getLikedPosts(params: { pageNum: number, pageSize: number, userId: number }) {
    return request({
      url: '/user/liked-posts',
      method: 'get',
      params
    })
  }
