<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ t('datasources.title') }}</h1>
        <p class="page-desc">{{ t('datasources.desc') }}</p>
      </div>
      <button class="btn-primary" @click="openCreateModal">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        {{ t('datasources.addButton') }}
      </button>
    </div>

    <!-- 数据源列表 -->
    <div class="tools-table-wrap">
      <table class="tools-table">
        <thead>
          <tr>
            <th>{{ t('datasources.columns.name') }}</th>
            <th>{{ t('datasources.columns.type') }}</th>
            <th>{{ t('datasources.columns.connection') }}</th>
            <th>{{ t('datasources.columns.status') }}</th>
            <th>{{ t('datasources.columns.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ds in datasources" :key="ds.id" class="tool-row">
            <!-- 名称 + 描述 -->
            <td>
              <div class="tool-info">
                <div class="tool-icon-wrap" :class="{ 'icon-ok': ds.lastTestOk === true, 'icon-fail': ds.lastTestOk === false }">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/>
                  </svg>
                </div>
                <div>
                  <div class="tool-name">{{ ds.name }}</div>
                  <div class="tool-desc" v-if="ds.description">{{ ds.description }}</div>
                </div>
              </div>
            </td>
            <!-- 类型 -->
            <td>
              <span class="type-badge" :class="'type-' + ds.dbType">{{ dbTypeLabel(ds.dbType) }}</span>
            </td>
            <!-- 连接信息 -->
            <td>
              <div class="conn-info">
                <code class="conn-host">{{ ds.host }}:{{ ds.port }}</code>
                <span class="conn-db">{{ ds.databaseName }}<template v-if="ds.schemaName"> / {{ ds.schemaName }}</template></span>
              </div>
            </td>
            <!-- 状态 -->
            <td>
              <div class="status-cell">
                <span class="status-dot" :class="statusClass(ds)"></span>
                <label class="toggle-switch">
                  <input type="checkbox" :checked="ds.enabled" @change="toggleDs(ds)" />
                  <span class="toggle-slider"></span>
                </label>
              </div>
            </td>
            <!-- 操作 -->
            <td>
              <div class="row-actions">
                <button class="row-btn test-btn" @click="testConnection(ds)" :disabled="testing === ds.id" :title="t('datasources.testButton')">
                  <svg v-if="testing !== ds.id" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
                  </svg>
                  <span v-else class="spinner"></span>
                </button>
                <button class="row-btn" @click="openEditModal(ds)" :title="t('common.edit')">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button class="row-btn danger" @click="deleteDs(ds.id)" :title="t('common.delete')">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                  </svg>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="datasources.length === 0">
            <td colspan="5" class="empty-row">
              <div class="empty-state">
                <span class="empty-icon">🗄</span>
                <p>{{ t('datasources.empty') }}</p>
                <button class="btn-primary" style="margin-top: 8px" @click="openCreateModal">
                  {{ t('datasources.addButton') }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ editingDs ? t('datasources.modal.editTitle') : t('datasources.modal.newTitle') }}</h2>
          <button class="modal-close" @click="closeModal">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <!-- 基本信息 -->
          <div class="form-section-title">{{ t('datasources.sections.basic') }}</div>
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">{{ t('datasources.fields.name') }} *</label>
              <input v-model="form.name" class="form-input" :placeholder="t('datasources.placeholders.name')" />
            </div>
            <div class="form-group">
              <label class="form-label">{{ t('datasources.fields.dbType') }} *</label>
              <select v-model="form.dbType" class="form-input" @change="onDbTypeChange">
                <option value="mysql">MySQL</option>
                <option value="postgresql">PostgreSQL</option>
                <option value="clickhouse">ClickHouse</option>
                <option value="mariadb">MariaDB</option>
              </select>
            </div>
            <div class="form-group full-width">
              <label class="form-label">{{ t('datasources.fields.description') }}</label>
              <input v-model="form.description" class="form-input" :placeholder="t('datasources.placeholders.description')" />
            </div>
          </div>

          <!-- 连接信息 -->
          <div class="form-section-title">{{ t('datasources.sections.connection') }}</div>
          <div class="form-grid">
            <div class="form-group" style="flex: 2">
              <label class="form-label">{{ t('datasources.fields.host') }} *</label>
              <input v-model="form.host" class="form-input" :placeholder="t('datasources.placeholders.host')" />
            </div>
            <div class="form-group" style="flex: 1">
              <label class="form-label">{{ t('datasources.fields.port') }} *</label>
              <input v-model.number="form.port" type="number" class="form-input" :placeholder="String(defaultPort)" />
            </div>
            <div class="form-group">
              <label class="form-label">{{ t('datasources.fields.databaseName') }} *</label>
              <input v-model="form.databaseName" class="form-input" :placeholder="t('datasources.placeholders.databaseName')" />
            </div>
            <div class="form-group" v-if="form.dbType === 'postgresql'">
              <label class="form-label">{{ t('datasources.fields.schemaName') }}</label>
              <input v-model="form.schemaName" class="form-input" :placeholder="t('datasources.placeholders.schemaName')" />
            </div>
          </div>

          <!-- 认证信息 -->
          <div class="form-section-title">{{ t('datasources.sections.auth') }}</div>
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">{{ t('datasources.fields.username') }}</label>
              <input v-model="form.username" class="form-input" autocomplete="off" :placeholder="t('datasources.placeholders.username')" />
            </div>
            <div class="form-group">
              <label class="form-label">{{ t('datasources.fields.password') }}</label>
              <div class="password-wrap">
                <input v-model="form.password" :type="showPassword ? 'text' : 'password'" class="form-input" autocomplete="new-password" :placeholder="t('datasources.placeholders.password')" />
                <button class="password-toggle" @click="showPassword = !showPassword" type="button">
                  <svg v-if="!showPassword" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                </button>
              </div>
            </div>
          </div>

          <!-- 高级配置 -->
          <div class="form-section-title advanced-toggle" @click="showAdvanced = !showAdvanced">
            {{ t('datasources.sections.advanced') }}
            <svg :class="{ rotated: showAdvanced }" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
          <div class="form-grid" v-if="showAdvanced">
            <div class="form-group full-width">
              <label class="form-label">{{ t('datasources.fields.extraParams') }}</label>
              <input v-model="form.extraParams" class="form-input" :placeholder="t('datasources.placeholders.extraParams')" />
              <span class="form-hint">{{ t('datasources.hints.extraParams') }}</span>
            </div>
          </div>

          <!-- 连接测试结果 -->
          <div v-if="modalTestResult !== null" class="test-result" :class="modalTestResult ? 'test-ok' : 'test-fail'">
            <svg v-if="modalTestResult" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            {{ modalTestResult ? t('datasources.messages.testSuccess') : t('datasources.messages.testFailed') }}
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-test" @click="testInModal" :disabled="modalTesting || !canSave">
            <span v-if="modalTesting" class="spinner"></span>
            <template v-else>{{ t('datasources.testButton') }}</template>
          </button>
          <div style="flex: 1"></div>
          <button class="btn-secondary" @click="closeModal">{{ t('common.cancel') }}</button>
          <button class="btn-primary" @click="saveDs" :disabled="!canSave">{{ t('common.save') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { datasourceApi } from '@/api/index'

const { t } = useI18n()

interface Datasource {
  id: number | string
  name: string
  description: string
  dbType: string
  host: string
  port: number
  databaseName: string
  username: string
  password: string
  extraParams: string
  schemaName: string
  enabled: boolean
  lastTestOk: boolean | null
  lastTestTime: string | null
}

const datasources = ref<Datasource[]>([])
const showModal = ref(false)
const editingDs = ref<Datasource | null>(null)
const testing = ref<number | string | null>(null)
const showPassword = ref(false)
const showAdvanced = ref(false)
const modalTesting = ref(false)
const modalTestResult = ref<boolean | null>(null)

const PORT_MAP: Record<string, number> = {
  mysql: 3306, mariadb: 3306, postgresql: 5432, clickhouse: 8123,
}

const defaultPort = computed(() => PORT_MAP[form.value.dbType] || 3306)

const defaultForm = () => ({
  name: '', description: '', dbType: 'mysql', host: '', port: 3306,
  databaseName: '', username: '', password: '', extraParams: '', schemaName: '', enabled: true,
})
const form = ref<any>(defaultForm())

const canSave = computed(() => form.value.name && form.value.host && form.value.port && form.value.databaseName)

onMounted(loadDatasources)

async function loadDatasources() {
  try {
    const res: any = await datasourceApi.list()
    datasources.value = res.data || []
  } catch { datasources.value = [] }
}

function dbTypeLabel(dbType: string) {
  const labels: Record<string, string> = { mysql: 'MySQL', postgresql: 'PostgreSQL', clickhouse: 'ClickHouse', mariadb: 'MariaDB' }
  return labels[dbType] || dbType
}

function statusClass(ds: Datasource) {
  if (!ds.enabled) return 'dot-disabled'
  if (ds.lastTestOk === true) return 'dot-ok'
  if (ds.lastTestOk === false) return 'dot-fail'
  return 'dot-unknown'
}

function onDbTypeChange() {
  form.value.port = PORT_MAP[form.value.dbType] || 3306
  if (form.value.dbType !== 'postgresql') form.value.schemaName = ''
}

function openCreateModal() {
  editingDs.value = null
  form.value = defaultForm()
  showPassword.value = false
  showAdvanced.value = false
  modalTestResult.value = null
  showModal.value = true
}

function openEditModal(ds: Datasource) {
  editingDs.value = ds
  form.value = { ...ds }
  showPassword.value = false
  showAdvanced.value = !!(ds.extraParams)
  modalTestResult.value = null
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingDs.value = null
  modalTestResult.value = null
}

async function saveDs() {
  try {
    let saved: any
    if (editingDs.value) {
      saved = await datasourceApi.update(editingDs.value.id, form.value)
    } else {
      saved = await datasourceApi.create(form.value)
    }
    closeModal()
    await loadDatasources()
    // 保存成功后自动触发测试连接
    const id = saved?.data?.id
    if (id) {
      autoTestAfterSave(id)
    }
  } catch (e: any) { ElMessage.error(e?.message || t('datasources.messages.saveFailed')) }
}

async function autoTestAfterSave(id: number | string) {
  testing.value = id
  try {
    const res: any = await datasourceApi.test(id)
    const ok = res.data?.success
    ElMessage({ type: ok ? 'success' : 'warning', message: ok ? t('datasources.messages.testSuccess') : t('datasources.messages.testFailed') })
    await loadDatasources()
  } catch { /* ignore */ }
  finally { testing.value = null }
}

async function testInModal() {
  if (!editingDs.value) {
    // 新建模式：先保存再测试
    try {
      const saved: any = await datasourceApi.create(form.value)
      editingDs.value = saved.data
      form.value = { ...saved.data }
      await loadDatasources()
    } catch (e: any) {
      ElMessage.error(e?.message || t('datasources.messages.saveFailed'))
      return
    }
  } else {
    // 编辑模式：先保存更新
    try {
      await datasourceApi.update(editingDs.value.id, form.value)
      await loadDatasources()
    } catch (e: any) {
      ElMessage.error(e?.message || t('datasources.messages.saveFailed'))
      return
    }
  }

  modalTesting.value = true
  modalTestResult.value = null
  try {
    const res: any = await datasourceApi.test(editingDs.value!.id)
    modalTestResult.value = !!res.data?.success
    await loadDatasources()
  } catch { modalTestResult.value = false }
  finally { modalTesting.value = false }
}

async function deleteDs(id: string | number) {
  try { await ElMessageBox.confirm(t('datasources.messages.deleteConfirm'), t('datasources.messages.deleteTitle'), { type: 'warning' }) } catch { return }
  try {
    await datasourceApi.delete(id)
    await loadDatasources()
  } catch (e: any) { ElMessage.error(e?.message || t('datasources.messages.deleteFailed')) }
}

async function toggleDs(ds: Datasource) {
  try {
    await datasourceApi.toggle(ds.id, !ds.enabled)
    await loadDatasources()
  } catch (e: any) { ElMessage.error(e?.message || t('datasources.messages.toggleFailed')) }
}

async function testConnection(ds: Datasource) {
  testing.value = ds.id
  try {
    const res: any = await datasourceApi.test(ds.id)
    const ok = res.data?.success
    ElMessage({ type: ok ? 'success' : 'error', message: ok ? t('datasources.messages.testSuccess') : t('datasources.messages.testFailed') })
    await loadDatasources()
  } catch (e: any) {
    ElMessage.error(e?.message || t('datasources.messages.testFailed'))
  } finally { testing.value = null }
}
</script>

<style scoped>
.page-container { height: 100%; overflow-y: auto; padding: 24px; background: var(--mc-bg); }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; }
.page-title { font-size: 20px; font-weight: 700; color: var(--mc-text-primary); margin: 0 0 4px; }
.page-desc { font-size: 14px; color: var(--mc-text-secondary); margin: 0; }
.btn-primary { display: flex; align-items: center; gap: 6px; padding: 8px 16px; background: var(--mc-primary); color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-primary:hover { background: var(--mc-primary-hover); }
.btn-primary:disabled { background: var(--mc-border); cursor: not-allowed; }
.btn-secondary { padding: 8px 16px; background: var(--mc-bg-elevated); color: var(--mc-text-primary); border: 1px solid var(--mc-border); border-radius: 8px; font-size: 14px; cursor: pointer; }
.btn-secondary:hover { background: var(--mc-bg-sunken); }
.btn-test { display: flex; align-items: center; gap: 6px; padding: 8px 16px; background: transparent; color: var(--mc-primary); border: 1px solid var(--mc-primary); border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; min-width: 90px; justify-content: center; }
.btn-test:hover { background: var(--mc-primary-bg); }
.btn-test:disabled { opacity: 0.5; cursor: not-allowed; }

/* Table */
.tools-table-wrap { background: var(--mc-bg-elevated); border: 1px solid var(--mc-border); border-radius: 12px; overflow: hidden; }
.tools-table { width: 100%; border-collapse: collapse; }
.tools-table th { padding: 12px 16px; text-align: left; font-size: 12px; font-weight: 600; color: var(--mc-text-secondary); text-transform: uppercase; letter-spacing: 0.05em; background: var(--mc-bg-sunken); border-bottom: 1px solid var(--mc-border); }
.tool-row { border-bottom: 1px solid var(--mc-border-light); transition: background 0.1s; }
.tool-row:hover { background: var(--mc-bg-sunken); }
.tool-row:last-child { border-bottom: none; }
.tools-table td { padding: 14px 16px; font-size: 14px; color: var(--mc-text-primary); }
.tool-info { display: flex; align-items: center; gap: 10px; }
.tool-icon-wrap { width: 32px; height: 32px; background: var(--mc-bg-sunken); border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: var(--mc-text-secondary); }
.tool-icon-wrap.icon-ok { background: #e8f5e9; color: #2e7d32; }
.tool-icon-wrap.icon-fail { background: #fce4ec; color: #c62828; }
.tool-name { font-weight: 500; color: var(--mc-text-primary); }
.tool-desc { font-size: 12px; color: var(--mc-text-tertiary); margin-top: 1px; max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Connection info */
.conn-info { display: flex; flex-direction: column; gap: 2px; }
.conn-host { background: var(--mc-bg-sunken); padding: 2px 8px; border-radius: 4px; font-size: 12px; color: var(--mc-text-primary); display: inline-block; }
.conn-db { font-size: 12px; color: var(--mc-text-tertiary); }

/* Type badge */
.type-badge { padding: 3px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.type-mysql { background: #e8f4fd; color: #1a73e8; }
.type-postgresql { background: #e8f0fe; color: #336791; }
.type-clickhouse { background: #fff8e1; color: #e6a817; }
.type-mariadb { background: #fce4ec; color: #c0392b; }

/* Status */
.status-cell { display: flex; align-items: center; gap: 8px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.dot-ok { background: #4caf50; box-shadow: 0 0 4px rgba(76,175,80,0.4); }
.dot-fail { background: #f44336; box-shadow: 0 0 4px rgba(244,67,54,0.4); }
.dot-unknown { background: var(--mc-border); }
.dot-disabled { background: var(--mc-border); opacity: 0.5; }

/* Toggle */
.toggle-switch { position: relative; display: inline-block; width: 36px; height: 20px; cursor: pointer; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; inset: 0; background: var(--mc-border); border-radius: 20px; transition: 0.2s; }
.toggle-slider::before { content: ''; position: absolute; width: 14px; height: 14px; left: 3px; top: 3px; background: var(--mc-bg-elevated); border-radius: 50%; transition: 0.2s; }
.toggle-switch input:checked + .toggle-slider { background: var(--mc-primary); }
.toggle-switch input:checked + .toggle-slider::before { transform: translateX(16px); }

/* Actions */
.row-actions { display: flex; gap: 4px; }
.row-btn { width: 28px; height: 28px; border: 1px solid var(--mc-border); background: var(--mc-bg-elevated); border-radius: 6px; cursor: pointer; display: flex; align-items: center; justify-content: center; color: var(--mc-text-secondary); transition: all 0.15s; }
.row-btn:hover { background: var(--mc-bg-sunken); }
.row-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.row-btn.danger:hover { background: var(--mc-danger-bg); border-color: var(--mc-danger); color: var(--mc-danger); }
.row-btn.test-btn:hover { border-color: var(--mc-primary); color: var(--mc-primary); }

/* Spinner */
.spinner { width: 12px; height: 12px; border: 2px solid var(--mc-border); border-top-color: var(--mc-primary); border-radius: 50%; animation: spin 0.6s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Empty */
.empty-row { padding: 40px !important; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 8px; color: var(--mc-text-tertiary); }
.empty-icon { font-size: 32px; }
.empty-state p { font-size: 14px; margin: 0; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 20px; }
.modal { background: var(--mc-bg-elevated); border: 1px solid var(--mc-border); border-radius: 16px; width: 100%; max-width: 580px; max-height: 90vh; display: flex; flex-direction: column; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid var(--mc-border-light); }
.modal-header h2 { font-size: 18px; font-weight: 600; color: var(--mc-text-primary); margin: 0; }
.modal-close { width: 32px; height: 32px; border: none; background: none; cursor: pointer; color: var(--mc-text-tertiary); display: flex; align-items: center; justify-content: center; border-radius: 6px; }
.modal-close:hover { background: var(--mc-bg-sunken); }
.modal-body { flex: 1; overflow-y: auto; padding: 20px 24px; }

/* Form sections */
.form-section-title { font-size: 13px; font-weight: 600; color: var(--mc-text-secondary); text-transform: uppercase; letter-spacing: 0.05em; margin: 20px 0 10px; padding-bottom: 6px; border-bottom: 1px solid var(--mc-border-light); }
.form-section-title:first-child { margin-top: 0; }
.advanced-toggle { cursor: pointer; display: flex; align-items: center; gap: 4px; user-select: none; }
.advanced-toggle svg { transition: transform 0.2s; }
.advanced-toggle svg.rotated { transform: rotate(180deg); }

/* Form */
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group.full-width { grid-column: 1 / -1; }
.form-label { font-size: 13px; font-weight: 500; color: var(--mc-text-secondary); }
.form-input { padding: 8px 12px; border: 1px solid var(--mc-border); border-radius: 8px; font-size: 14px; color: var(--mc-text-primary); outline: none; background: var(--mc-bg-sunken); width: 100%; }
.form-input:focus { border-color: var(--mc-primary); box-shadow: 0 0 0 2px rgba(217,119,87,0.1); }
.form-hint { font-size: 11px; color: var(--mc-text-tertiary); margin-top: 2px; }

/* Password field */
.password-wrap { position: relative; }
.password-wrap .form-input { padding-right: 36px; }
.password-toggle { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); border: none; background: none; cursor: pointer; color: var(--mc-text-tertiary); padding: 4px; display: flex; align-items: center; justify-content: center; }
.password-toggle:hover { color: var(--mc-text-secondary); }

/* Test result in modal */
.test-result { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: 8px; font-size: 13px; font-weight: 500; margin-top: 16px; }
.test-ok { background: #e8f5e9; color: #2e7d32; }
.test-fail { background: #fce4ec; color: #c62828; }

/* Footer */
.modal-footer { display: flex; align-items: center; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--mc-border-light); }
</style>
