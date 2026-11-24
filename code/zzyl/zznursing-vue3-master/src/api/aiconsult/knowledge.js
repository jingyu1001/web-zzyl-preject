import request from '@/utils/request'

// 上传知识库文档
// 对应后端 KnowledgeBaseController 的 @RequestMapping("/knowledge/base") + @PostMapping("/upload")
export function uploadKnowledgeDoc(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/knowledge/base/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
      // 关闭防重复提交校验，避免连续上传同一文件被拦截
      repeatSubmit: false
    },
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded / e.total) * 100))
      }
    }
  })
}

// 查询知识库列表
export function listKnowledge(query) {
  return request({
    url: '/knowledge/base/list',
    method: 'get',
    params: query
  })
}

// 查询知识库详细
export function getKnowledge(id) {
  return request({
    url: '/knowledge/base/' + id,
    method: 'get'
  })
}

// 新增知识库（文档已上传OSS，提交表单字段 + 文档地址）
export function addKnowledge(data) {
  return request({
    url: '/knowledge/base',
    method: 'post',
    data: data
  })
}

// 修改知识库
export function updateKnowledge(data) {
  return request({
    url: '/aiconsult/knowledge',
    method: 'put',
    data: data
  })
}

// 删除知识库
export function delKnowledge(id) {
  return request({
    url: '/knowledge/base/' + id,
    method: 'delete'
  })
}

// 修改知识库状态
export function changeKnowledgeStatus(id, status) {
  const data = { id, status }
  return request({
    url: '/knowledge/base/changeStatus',
    method: 'put',
    data: data
  })
}
