<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="知识标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="queryParams.category" placeholder="请选择" clearable style="width: 200px">
          <el-option
            v-for="item in categoryOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 200px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="knowledgeList">
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="标题" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="分类" prop="category" align="center" width="140">
        <template #default="scope">
          <span>{{ categoryLabel(scope.row.category) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="标签" prop="tags" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="status" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" prop="priority" align="center" width="80" />
      <el-table-column label="创建时间" prop="createTime" align="center" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handlePreview(scope.row)">预览</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          <el-button link type="primary" :icon="scope.row.status === 1 ? 'Lock' : 'Unlock'" @click="handleStatusChange(scope.row)">
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加知识库弹窗 -->
    <AddKnowledgeDialog
      :visible="addDialogVisible"
      @handleClose="addDialogVisible = false"
      @handleSub="handleAddSubmit"
    />
  </div>
</template>

<script setup name="Knowledge">
import { listKnowledge, delKnowledge, changeKnowledgeStatus, addKnowledge } from "@/api/aiconsult/knowledge"
import AddKnowledgeDialog from "./AddKnowledgeDialog.vue"

// 后端接口未就绪前使用本地模拟数据，接口就绪后改为 false 即可走真实接口
const USE_MOCK = false

const { proxy } = getCurrentInstance()

// 分类选项：暂用本地常量，后端就绪后可替换为字典数据
const categoryOptions = [
  { value: 1, label: "运营支持管理类" },
  { value: 2, label: "护理业务管理类" },
  { value: 3, label: "人事行政管理类" },
  { value: 4, label: "后勤安全管理类" }
]

const knowledgeList = ref([])
const loading = ref(true)
const total = ref(0)
const addDialogVisible = ref(false)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: undefined,
    category: undefined,
    status: undefined
  }
})
const { queryParams } = toRefs(data)

// 本地模拟数据（结构对应 knowledge_base 表）
let mockData = [
  { id: 1, title: "护理员工工作手册", category: 1, tags: "www", status: 1, priority: 2, documentUrl: "https://www.baidu.com", createTime: "2026-03-25 20:23:28", remark: "" },
  { id: 2, title: "养老机构服务安全管理制度", category: 4, tags: "安全,制度", status: 1, priority: 5, documentUrl: "", createTime: "2026-04-02 09:15:40", remark: "" },
  { id: 3, title: "入住老人健康档案管理规范", category: 2, tags: "健康档案", status: 0, priority: 3, documentUrl: "", createTime: "2026-04-10 14:30:00", remark: "" }
]

function isBlank(value) {
  return value === undefined || value === null || value === ""
}

/** 分类值转名称 */
function categoryLabel(category) {
  const item = categoryOptions.find(i => i.value === category)
  return item ? item.label : category
}

/** 查询知识库列表 */
function getList() {
  loading.value = true
  if (USE_MOCK) {
    const { title, category, status, pageNum, pageSize } = queryParams.value
    const filtered = mockData.filter(item => {
      return (!title || item.title.includes(title)) &&
        (isBlank(category) || item.category === category) &&
        (isBlank(status) || item.status === status)
    })
    total.value = filtered.length
    const start = (pageNum - 1) * pageSize
    knowledgeList.value = filtered.slice(start, start + pageSize)
    loading.value = false
  } else {
    listKnowledge(queryParams.value).then(response => {
      knowledgeList.value = response.rows
      total.value = response.total
      loading.value = false
    })
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 预览按钮操作 */
function handlePreview(row) {
  if (row.documentUrl) {
    window.open(row.documentUrl, "_blank")
  } else {
    proxy.$modal.msg("该知识暂无文档链接")
  }
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除知识"' + row.title + '"？').then(() => {
    if (USE_MOCK) {
      mockData = mockData.filter(item => item.id !== row.id)
      getList()
      proxy.$modal.msgSuccess("删除成功")
    } else {
      return delKnowledge(row.id).then(() => {
        getList()
        proxy.$modal.msgSuccess("删除成功")
      })
    }
  }).catch(() => {})
}

/** 禁用/启用按钮操作 */
function handleStatusChange(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const text = row.status === 1 ? "禁用" : "启用"
  proxy.$modal.confirm('确认要' + text + '知识"' + row.title + '"吗？').then(() => {
    if (USE_MOCK) {
      row.status = newStatus
      proxy.$modal.msgSuccess(text + "成功")
    } else {
      return changeKnowledgeStatus(row.id, newStatus).then(() => {
        row.status = newStatus
        proxy.$modal.msgSuccess(text + "成功")
      })
    }
  }).catch(() => {})
}

/** 新增按钮操作 */
function handleAdd() {
  addDialogVisible.value = true
}

/** 新增提交 */
function handleAddSubmit(data) {
  if (USE_MOCK) {
    const newId = mockData.length > 0 ? Math.max(...mockData.map(i => i.id)) + 1 : 1
    mockData.unshift({
      id: newId,
      title: data.title || '新知识库',
      category: data.category,
      tags: data.tags,
      status: data.status,
      priority: data.priority,
      documentUrl: data.documentUrl || '',
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19)
    })
    addDialogVisible.value = false
    getList()
    proxy.$modal.msgSuccess("新增成功")
  } else {
    if (!data.documentUrl) {
      proxy.$modal.msgError("请上传知识库文档")
      return
    }
    const submitData = {
      title: data.title,
      category: data.category,
      tags: data.tags,
      status: data.status,
      priority: data.priority,
      documentUrl: data.documentUrl
    }
    addKnowledge(submitData).then(() => {
      addDialogVisible.value = false
      getList()
      proxy.$modal.msgSuccess("新增成功")
    }).catch(() => {})
  }
}

getList()
</script>
