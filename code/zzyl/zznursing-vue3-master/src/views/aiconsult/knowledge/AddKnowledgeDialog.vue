<!-- 添加知识库弹窗 -->
<template>
  <el-dialog
    title="添加知识库"
    v-model="formVisible"
    width="520px"
    @close="cancel"
  >
    <el-form
      ref="queryRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="分类" prop="category">
        <el-select v-model="formData.category" placeholder="请选择" class="wt-400">
          <el-option
            v-for="item in categoryOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="标签数组" prop="tags">
        <el-input
          v-model="formData.tags"
          placeholder="请输入标签"
          class="wt-400"
        />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="0">禁用</el-radio>
          <el-radio :value="1">启用</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="优先级" prop="priority">
        <el-select v-model="formData.priority" placeholder="请选择" class="wt-400">
          <el-option
            v-for="item in priorityOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="文档上传">
        <el-upload
          ref="uploadRef"
          class="upload-demo"
          :auto-upload="true"
          :show-file-list="false"
          :http-request="handleUpload"
          :before-upload="beforeUpload"
          accept=".pdf,.txt,.md"
        >
          <el-button type="primary" plain>{{ fileInfo.status === 'success' ? '重新上传' : '点击上传' }}</el-button>
          <template #tip>
            <div class="el-upload__tip">支持 pdf/txt/md，单个文件大小不超过 10MB</div>
          </template>
        </el-upload>
        <div v-if="fileInfo.name" class="uploaded-file">
          <el-icon><Document /></el-icon>
          <span class="file-name">{{ fileInfo.name }}</span>
          <el-progress
            v-if="fileInfo.status === 'uploading'"
            :percentage="fileInfo.percentage"
            :stroke-width="6"
            class="file-progress"
          />
          <el-icon v-else-if="fileInfo.status === 'success'" class="ok-btn"><CircleCheck /></el-icon>
          <el-icon v-else-if="fileInfo.status === 'fail'" class="fail-btn"><CircleClose /></el-icon>
          <el-icon class="remove-btn" @click="removeFile"><Close /></el-icon>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { Document, Close, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { uploadKnowledgeDoc } from '@/api/aiconsult/knowledge'

const { proxy } = getCurrentInstance()

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['handleClose', 'handleSub'])

// 分类选项
const categoryOptions = [
  { value: 1, label: '运营支持管理类' },
  { value: 2, label: '护理业务管理类' },
  { value: 3, label: '人事行政管理类' },
  { value: 4, label: '后勤安全管理类' }
]

// 优先级选项 1-5
const priorityOptions = [1, 2, 3, 4, 5]

const formVisible = ref(false)
const formData = ref({
  category: undefined,
  tags: '',
  status: 1,
  priority: undefined,
  documentUrl: ''
})

// 上传相关状态：name 为空表示尚未选择文件
const uploadRef = ref(null)
const fileInfo = reactive({
  name: '',
  status: '', // uploading / success / fail
  percentage: 0
})
// 上传序号，用于丢弃被后一次选择取代的旧上传结果
let uploadSeq = 0

const rules = {
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ],
  priority: [
    { required: true, message: '请选择优先级', trigger: 'change' }
  ]
}

watch(
  () => props.visible,
  (val) => {
    formVisible.value = val
  }
)

/** 选择文件前的校验：格式与大小 */
function beforeUpload(file) {
  const ext = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : ''
  if (!['pdf', 'txt', 'md'].includes(ext)) {
    proxy.$modal.msgError('仅支持 pdf/txt/md 格式的文件')
    return false
  }
  if (file.size / 1024 / 1024 > 10) {
    proxy.$modal.msgError('文件大小不能超过 10MB')
    return false
  }
  return true
}

/** 自定义上传：交给 axios 请求，便于统一携带 token 与处理错误 */
function handleUpload(options) {
  const seq = ++uploadSeq
  fileInfo.name = options.file.name
  fileInfo.status = 'uploading'
  fileInfo.percentage = 0
  formData.value.documentUrl = ''

  return uploadKnowledgeDoc(options.file, (percentage) => {
    if (seq === uploadSeq) {
      fileInfo.percentage = percentage
    }
  }).then(res => {
    if (seq !== uploadSeq) {
      return
    }
    fileInfo.status = 'success'
    fileInfo.percentage = 100
    // 后端返回 OSS 访问地址，随表单一起提交
    formData.value.documentUrl = res.url
    options.onSuccess(res)
  }).catch(err => {
    if (seq !== uploadSeq) {
      return
    }
    fileInfo.status = 'fail'
    formData.value.documentUrl = ''
    options.onError(err)
  })
}

function removeFile() {
  uploadSeq++
  fileInfo.name = ''
  fileInfo.status = ''
  fileInfo.percentage = 0
  formData.value.documentUrl = ''
  uploadRef.value && uploadRef.value.clearFiles()
}

function resetForm() {
  formData.value = {
    category: undefined,
    tags: '',
    status: 1,
    priority: undefined,
    documentUrl: ''
  }
  removeFile()
  proxy.resetForm('queryRef')
}

function submitForm() {
  proxy.$refs['queryRef'].validate((valid) => {
    if (valid) {
      if (fileInfo.status === 'uploading') {
        proxy.$modal.msgError('文档正在上传，请稍候')
        return
      }
      if (fileInfo.status === 'fail') {
        proxy.$modal.msgError('文档上传失败，请重新上传或移除该文件')
        return
      }
      // 知识标题取上传文件名（去掉后缀）
      const title = fileInfo.name ? fileInfo.name.replace(/\.[^.]+$/, '') : ''
      emit('handleSub', { ...formData.value, title })
      resetForm()
    }
  })
}

function cancel() {
  resetForm()
  emit('handleClose')
}

defineExpose({ resetForm })
</script>

<style scoped>
.wt-400 {
  width: 400px;
}
.uploaded-file {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  width: fit-content;
  max-width: 400px;
}
.uploaded-file .file-name {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-progress {
  width: 100px;
}
.uploaded-file .ok-btn {
  color: #67c23a;
}
.uploaded-file .fail-btn {
  color: #f56c6c;
}
.uploaded-file .remove-btn {
  cursor: pointer;
  color: #909399;
}
.uploaded-file .remove-btn:hover {
  color: #f56c6c;
}
</style>
