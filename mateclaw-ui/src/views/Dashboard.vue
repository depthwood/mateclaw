<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">{{ t('dashboard.title') }}</h1>
      <p class="page-desc">{{ t('dashboard.desc') }}</p>
    </div>

    <!-- Overview Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon">💬</div>
        <div class="stat-body">
          <div class="stat-value">{{ todayStats.conversations }}</div>
          <div class="stat-label">{{ t('dashboard.conversations') }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📝</div>
        <div class="stat-body">
          <div class="stat-value">{{ todayStats.messages }}</div>
          <div class="stat-label">{{ t('dashboard.messages') }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🎯</div>
        <div class="stat-body">
          <div class="stat-value">{{ formatTokens(todayStats.totalTokens) }}</div>
          <div class="stat-label">{{ t('dashboard.tokens') }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🔧</div>
        <div class="stat-body">
          <div class="stat-value">{{ todayStats.toolCalls }}</div>
          <div class="stat-label">{{ t('dashboard.toolCalls') }}</div>
        </div>
      </div>
    </div>

    <!-- Period Comparison -->
    <div class="comparison-section">
      <h2 class="section-title">{{ t('dashboard.periodComparison') }}</h2>
      <div class="comparison-grid">
        <div class="comparison-card" v-for="(period, key) in overview" :key="key">
          <h3 class="comparison-title">{{ t('dashboard.periods.' + key) }}</h3>
          <div class="comparison-row">
            <span class="comparison-label">{{ t('dashboard.conversations') }}</span>
            <span class="comparison-value">{{ period.conversations }}</span>
          </div>
          <div class="comparison-row">
            <span class="comparison-label">{{ t('dashboard.messages') }}</span>
            <span class="comparison-value">{{ period.messages }}</span>
          </div>
          <div class="comparison-row">
            <span class="comparison-label">{{ t('dashboard.tokens') }}</span>
            <span class="comparison-value">{{ formatTokens(period.totalTokens) }}</span>
          </div>
          <div class="comparison-row">
            <span class="comparison-label">{{ t('dashboard.toolCalls') }}</span>
            <span class="comparison-value">{{ period.toolCalls }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent CronJob Runs -->
    <div class="runs-section">
      <h2 class="section-title">{{ t('dashboard.recentRuns') }}</h2>
      <div class="runs-table-wrapper">
        <table v-if="recentRuns.length" class="runs-table">
          <thead>
            <tr>
              <th>{{ t('dashboard.runColumns.time') }}</th>
              <th>{{ t('dashboard.runColumns.job') }}</th>
              <th>{{ t('dashboard.runColumns.status') }}</th>
              <th>{{ t('dashboard.runColumns.trigger') }}</th>
              <th>{{ t('dashboard.runColumns.duration') }}</th>
              <th>{{ t('dashboard.runColumns.tokens') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="run in recentRuns" :key="run.id">
              <td class="cell-time">{{ formatTime(run.startedAt) }}</td>
              <td class="cell-job">#{{ run.cronJobId }}</td>
              <td>
                <span class="status-badge" :class="'status-' + run.status">{{ run.status }}</span>
              </td>
              <td class="cell-trigger">{{ run.triggerType }}</td>
              <td class="cell-duration">{{ calcDuration(run) }}</td>
              <td class="cell-tokens">{{ run.tokenUsage || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">{{ t('dashboard.noRuns') }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardApi } from '@/api'

const { t } = useI18n()

const overview = ref<Record<string, any>>({})
const recentRuns = ref<any[]>([])

const todayStats = reactive({
  conversations: 0,
  messages: 0,
  totalTokens: 0,
  toolCalls: 0,
  errors: 0,
})

onMounted(async () => {
  try {
    const [overviewRes, runsRes] = await Promise.all([
      dashboardApi.overview(),
      dashboardApi.recentRuns(10),
    ])
    overview.value = (overviewRes as any).data || {}
    const today = overview.value.today || {}
    Object.assign(todayStats, today)
    recentRuns.value = (runsRes as any).data || []
  } catch {
    // Dashboard data is non-critical
  }
})

function formatTokens(n: number): string {
  if (!n) return '0'
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M'
  if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K'
  return String(n)
}

function formatTime(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

function calcDuration(run: any): string {
  if (!run.startedAt || !run.finishedAt) return '-'
  const ms = new Date(run.finishedAt).getTime() - new Date(run.startedAt).getTime()
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}
</script>

<style scoped>
.page-container { height: 100%; overflow-y: auto; padding: 24px; background: var(--mc-bg); }

.page-header { margin-bottom: 24px; }
.page-title { font-size: 24px; font-weight: 700; color: var(--mc-text-primary); margin: 0 0 4px; }
.page-desc { font-size: 14px; color: var(--mc-text-tertiary); margin: 0; }

/* Stats Grid */
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 32px; }
.stat-card {
  display: flex; align-items: center; gap: 14px;
  background: var(--mc-bg-elevated); border: 1px solid var(--mc-border-light);
  border-radius: 12px; padding: 20px;
}
.stat-icon { font-size: 28px; }
.stat-body { display: flex; flex-direction: column; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--mc-text-primary); line-height: 1.2; }
.stat-label { font-size: 12px; color: var(--mc-text-tertiary); margin-top: 2px; }

/* Period Comparison */
.section-title { font-size: 16px; font-weight: 600; color: var(--mc-text-primary); margin: 0 0 16px; }
.comparison-section { margin-bottom: 32px; }
.comparison-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.comparison-card {
  background: var(--mc-bg-elevated); border: 1px solid var(--mc-border-light);
  border-radius: 10px; padding: 16px;
}
.comparison-title { font-size: 13px; font-weight: 600; color: var(--mc-text-secondary); margin: 0 0 12px; text-transform: uppercase; letter-spacing: 0.03em; }
.comparison-row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid var(--mc-border-light); }
.comparison-row:last-child { border-bottom: none; }
.comparison-label { font-size: 13px; color: var(--mc-text-tertiary); }
.comparison-value { font-size: 13px; font-weight: 600; color: var(--mc-text-primary); }

/* Runs Section */
.runs-section { margin-bottom: 32px; }
.runs-table-wrapper { border: 1px solid var(--mc-border-light); border-radius: 10px; overflow: hidden; }
.runs-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.runs-table th {
  padding: 10px 14px; text-align: left; font-weight: 600; font-size: 12px;
  color: var(--mc-text-tertiary); text-transform: uppercase; letter-spacing: 0.03em;
  background: var(--mc-bg-sunken); border-bottom: 1px solid var(--mc-border-light);
}
.runs-table td { padding: 10px 14px; border-bottom: 1px solid var(--mc-border-light); color: var(--mc-text-primary); }
.runs-table tr:last-child td { border-bottom: none; }

.cell-time { font-size: 12px; color: var(--mc-text-tertiary); white-space: nowrap; }
.cell-job { font-family: 'SF Mono', monospace; font-size: 12px; color: var(--mc-text-secondary); }
.cell-trigger { font-size: 12px; color: var(--mc-text-tertiary); }
.cell-duration { font-family: 'SF Mono', monospace; font-size: 12px; }
.cell-tokens { font-family: 'SF Mono', monospace; font-size: 12px; }

.status-badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.status-running { background: rgba(59, 130, 246, 0.12); color: #3b82f6; }
.status-completed { background: rgba(16, 185, 129, 0.12); color: #10b981; }
.status-failed { background: rgba(239, 68, 68, 0.12); color: #ef4444; }

.empty-state { padding: 40px; text-align: center; color: var(--mc-text-tertiary); font-size: 14px; }

@media (max-width: 768px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .comparison-grid { grid-template-columns: 1fr; }
}
</style>
