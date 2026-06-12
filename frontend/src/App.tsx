import { useEffect, useRef, useState, type FormEvent } from 'react'
import './App.css'
import {
  emptyFormState,
  fallbackAccessOverview,
  fallbackOverview,
  fallbackReportDetail,
  fallbackReports,
  fallbackStatistics,
  fallbackTemplate,
  fallbackUnits,
} from './demoData'
import type {
  AccessOverview,
  AccessRole,
  AccessUser,
  AuthPayload,
  AuthSession,
  AuthUser,
  NoticeState,
  NavKey,
  OverviewData,
  PermissionCatalogItem,
  ReportDetail,
  ReportFormState,
  ReportSummary,
  StatisticsData,
  StatisticsUnitItem,
  TemplateData,
  TemplateField,
} from './types'

const SESSION_STORAGE_KEY = 'dms-auth-session'

const navIcons: Record<string, React.ReactNode> = {
  dashboard: (
    <svg className="nav-icon" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <rect x="1" y="9" width="4" height="8" rx="1" fill="currentColor" opacity="0.6"/>
      <rect x="7" y="5" width="4" height="12" rx="1" fill="currentColor" opacity="0.8"/>
      <rect x="13" y="1" width="4" height="16" rx="1" fill="currentColor"/>
      <polyline points="3,8 9,4 15,1" stroke="rgba(120,220,200,0.9)" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
    </svg>
  ),
  entry: (
    <svg className="nav-icon" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <rect x="2" y="2" width="14" height="14" rx="2" stroke="currentColor" strokeWidth="1.4" fill="none" opacity="0.7"/>
      <line x1="5" y1="6" x2="13" y2="6" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
      <line x1="5" y1="9" x2="13" y2="9" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
      <line x1="5" y1="12" x2="9" y2="12" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
      <path d="M12 11l2 2-2 2" stroke="rgba(120,220,200,0.9)" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  reports: (
    <svg className="nav-icon" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path d="M3 2h8l4 4v10a1 1 0 01-1 1H3a1 1 0 01-1-1V3a1 1 0 011-1z" stroke="currentColor" strokeWidth="1.4" fill="none" opacity="0.7"/>
      <path d="M11 2v4h4" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" fill="none" opacity="0.5"/>
      <line x1="5" y1="9" x2="13" y2="9" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="5" y1="12" x2="10" y2="12" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
    </svg>
  ),
  master: (
    <svg className="nav-icon" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <circle cx="9" cy="5" r="2.5" stroke="currentColor" strokeWidth="1.4" fill="none" opacity="0.8"/>
      <circle cx="3.5" cy="13" r="2" stroke="currentColor" strokeWidth="1.3" fill="none" opacity="0.6"/>
      <circle cx="14.5" cy="13" r="2" stroke="currentColor" strokeWidth="1.3" fill="none" opacity="0.6"/>
      <line x1="9" y1="7.5" x2="3.5" y2="11" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" opacity="0.5"/>
      <line x1="9" y1="7.5" x2="14.5" y2="11" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" opacity="0.5"/>
    </svg>
  ),
  users: (
    <svg className="nav-icon" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <circle cx="7" cy="6" r="3" stroke="currentColor" strokeWidth="1.4" fill="none" opacity="0.8"/>
      <path d="M1 16c0-3.314 2.686-6 6-6s6 2.686 6 6" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" fill="none" opacity="0.7"/>
      <line x1="13" y1="5" x2="17" y2="5" stroke="rgba(120,220,200,0.9)" strokeWidth="1.4" strokeLinecap="round"/>
      <line x1="15" y1="3" x2="15" y2="7" stroke="rgba(120,220,200,0.9)" strokeWidth="1.4" strokeLinecap="round"/>
    </svg>
  ),
  roles: (
    <svg className="nav-icon" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path d="M9 1l2.1 4.3 4.7.7-3.4 3.3.8 4.7L9 11.5l-4.2 2.5.8-4.7-3.4-3.3 4.7-.7z" stroke="currentColor" strokeWidth="1.3" fill="none" opacity="0.8" strokeLinejoin="round"/>
    </svg>
  ),
  'reset-password': (
    <svg className="nav-icon" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <rect x="4" y="8" width="10" height="8" rx="1.5" stroke="currentColor" strokeWidth="1.4" fill="none" opacity="0.7"/>
      <path d="M6 8V6a3 3 0 016 0v2" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" fill="none" opacity="0.8"/>
      <circle cx="9" cy="12" r="1.2" fill="currentColor" opacity="0.8"/>
    </svg>
  ),
}

const navItems: Array<{ key: NavKey; label: string; description: string; permission: string }> = [
  { key: 'dashboard', label: '工作台', description: '总览与统计', permission: 'dashboard:view' },
  { key: 'entry', label: '月报填报', description: '模板录入与导入', permission: 'reports:edit' },
  { key: 'reports', label: '月报列表', description: '查询、导出、删除', permission: 'reports:view' },
  { key: 'master', label: '主数据管理', description: '统计单位与技术中心', permission: 'master-data:view' },
  { key: 'users', label: '账号管理', description: '用户、数据权限', permission: 'system:user:view' },
  { key: 'roles', label: '角色管理', description: '角色、权限授权', permission: 'system:user:view' },
  { key: 'reset-password', label: '重置密码', description: '修改账号密码', permission: 'system:user:view' },
]

type LoginDraft = {
  username: string
  password: string
}

type UserDraft = {
  username: string
  displayName: string
  password: string
  admin: boolean
  enabled: boolean
  roleIds: number[]
  centerIds: number[]
}

type RoleDraft = {
  code: string
  name: string
  enabled: boolean
  permissionCodes: string[]
}

const emptyUserDraft: UserDraft = {
  username: '',
  displayName: '',
  password: '',
  admin: false,
  enabled: true,
  roleIds: [],
  centerIds: [],
}

const emptyRoleDraft: RoleDraft = {
  code: '',
  name: '',
  enabled: true,
  permissionCodes: [],
}

function App() {
  const [session, setSession] = useState<AuthSession | null>(() => readStoredSession())
  const sessionRef = useRef<AuthSession | null>(session)
  const refreshPromiseRef = useRef<Promise<AuthSession | null> | null>(null)
  const userFormRef = useRef<HTMLFormElement>(null)
  const roleFormRef = useRef<HTMLFormElement>(null)

  const [activeView, setActiveView] = useState<NavKey>('dashboard')
  const [_layoutMode, _setLayoutMode] = useState<'cards' | 'spreadsheet'>('cards')
  const [overview, setOverview] = useState<OverviewData>(fallbackOverview)
  const [statistics, setStatistics] = useState<StatisticsData>(fallbackStatistics)
  const [units, setUnits] = useState<StatisticsUnitItem[]>(fallbackUnits)
  const [template, setTemplate] = useState<TemplateData>(fallbackTemplate)
  const [reports, setReports] = useState<ReportSummary[]>(fallbackReports)
  const [accessOverview, setAccessOverview] = useState<AccessOverview>(fallbackAccessOverview)
  const [formState, setFormState] = useState<ReportFormState>(emptyFormState)
  const [editingReportId, setEditingReportId] = useState<number | null>(fallbackReportDetail.id)
  const [entryMonth, setEntryMonth] = useState<string>(fallbackOverview.latestMonth)
  const [multiValues, setMultiValues] = useState<Record<number, Record<string, string>>>({})
  const [reportIdMap, setReportIdMap] = useState<Record<number, number | null>>({})
  const [loadingEntry, setLoadingEntry] = useState(false)
  const [savingMulti, setSavingMulti] = useState(false)
  const [exportingEntry, setExportingEntry] = useState(false)
  const [entryFullscreen, setEntryFullscreen] = useState(false)
  const [reportFilters, setReportFilters] = useState({
    month: fallbackOverview.latestMonth,
    unitId: '',
    centerId: '',
    keyword: '',
    fieldKey: '',
    fieldOp: '=',
    fieldValA: '',
    fieldValB: '',
  })
  const [unitDraft, setUnitDraft] = useState({ code: '', name: '' })
  const [centerDraft, setCenterDraft] = useState({ unitId: '1', code: '', name: '' })
  const [loginDraft, setLoginDraft] = useState<LoginDraft>({ username: 'admin', password: 'admin123' })
  const [userDraft, setUserDraft] = useState<UserDraft>(emptyUserDraft)
  const [roleDraft, setRoleDraft] = useState<RoleDraft>(emptyRoleDraft)
  const [editingUserId, setEditingUserId] = useState<number | null>(null)
  const [editingRoleId, setEditingRoleId] = useState<number | null>(null)
  const [resetPassword, setResetPassword] = useState('')
  const [notice, setNotice] = useState<NoticeState | null>(null)
  const [loading, setLoading] = useState(Boolean(session))
  const [authInitializing, setAuthInitializing] = useState(Boolean(session))
  const [authSubmitting, setAuthSubmitting] = useState(false)
  const [managing, setManaging] = useState(false)
  const [_downloading, _setDownloading] = useState(false)

  useEffect(() => {
    sessionRef.current = session
  }, [session])

  useEffect(() => {
    if (!sessionRef.current) {
      setAuthInitializing(false)
      setLoading(false)
      return
    }

    let disposed = false
    void (async () => {
      try {
        const me = await fetchWithSession<{ user: AuthUser }>('/api/auth/me', sessionRef.current as AuthSession)
        if (disposed) {
          return
        }
        const nextSession = { ...(sessionRef.current as AuthSession), user: me.user }
        persistSession(nextSession)
        setSession(nextSession)
        setActiveView(resolveDefaultView(me.user))
        await loadWorkspaceData(nextSession.user, fallbackOverview.latestMonth)
      } catch (error) {
        if (disposed) {
          return
        }
        clearSession('登录状态已失效，请重新登录。')
        setNotice({ type: 'error', text: getErrorMessage(error) })
      } finally {
        if (!disposed) {
          setAuthInitializing(false)
        }
      }
    })()

    return () => {
      disposed = true
    }
  }, [])

  // Auto-load entry data when navigating to the entry page
  useEffect(() => {
    if (activeView === 'entry' && units.length > 0) {
      void loadEntryReports(entryMonth, units)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeView, entryMonth])

  useEffect(() => {
    if (!entryFullscreen) return
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') setEntryFullscreen(false) }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [entryFullscreen])

  const currentUser = session?.user ?? null
  const visibleNavItems = currentUser ? navItems.filter((item) => canUser(currentUser, item.permission)) : []
  const selectedUnit = units.find((item) => item.id === formState.statisticsUnitId) ?? units[0]
  void selectedUnit
  const resolvedFormValues = resolveTemplateValues(template.fields, formState.values)
  void buildEntrySummary(resolvedFormValues)

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setAuthSubmitting(true)
    try {
      const payload = await requestPublicJson<AuthPayload>('/api/auth/login', 'POST', loginDraft)
      const nextSession = toSession(payload)
      persistSession(nextSession)
      setSession(nextSession)
      setActiveView(resolveDefaultView(nextSession.user))
      setNotice({ type: 'success', text: `欢迎回来，${nextSession.user.displayName}。` })
      await loadWorkspaceData(nextSession.user, fallbackOverview.latestMonth)
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setAuthSubmitting(false)
      setAuthInitializing(false)
    }
  }

  async function handleLogout(triggerNotice = true) {
    try {
      if (sessionRef.current) {
        await authorizedJson<void>('/api/auth/logout', 'POST', { refreshToken: sessionRef.current.refreshToken })
      }
    } catch {
      // noop
    } finally {
      clearSession(triggerNotice ? '已退出当前登录。' : undefined)
    }
  }

  async function loadWorkspaceData(user: AuthUser, targetMonth: string) {
    setLoading(true)
    try {
      const nextOverview = canUser(user, 'dashboard:view')
        ? await authorizedGet<OverviewData>('/api/dashboard/overview')
        : overview
      const effectiveMonth = targetMonth || nextOverview.latestMonth || overview.latestMonth || fallbackOverview.latestMonth

      const [nextStatistics, nextUnits, nextTemplate, nextReports, nextAccess] = await Promise.all([
        canUser(user, 'dashboard:view')
          ? authorizedGet<StatisticsData>(`/api/dashboard/statistics?month=${encodeURIComponent(effectiveMonth)}`)
          : Promise.resolve({ ...fallbackStatistics, month: effectiveMonth }),
        canUser(user, 'master-data:view')
          ? authorizedGet<StatisticsUnitItem[]>('/api/master-data/units')
          : Promise.resolve(buildUnitsFromCenters(user.centers)),
        canAnyPermission(user, ['reports:view', 'reports:edit', 'reports:export'])
          ? authorizedGet<TemplateData>('/api/report-templates/default')
          : Promise.resolve(fallbackTemplate),
        canUser(user, 'reports:view')
          ? authorizedGet<ReportSummary[]>(buildReportQuery(effectiveMonth, reportFilters.unitId, reportFilters.centerId, reportFilters.keyword))
          : Promise.resolve([]),
        canUser(user, 'system:user:view')
          ? authorizedGet<AccessOverview>('/api/system/access-overview')
          : Promise.resolve(fallbackAccessOverview),
      ])

      const normalizedUnits = nextUnits.length > 0 ? nextUnits : buildUnitsFromCenters(user.centers)
      const defaultUnit = normalizedUnits.find((item) => item.centers.length > 0) ?? normalizedUnits[0]
      const defaultCenter = defaultUnit?.centers[0]

      setOverview(nextOverview)
      setStatistics(nextStatistics)
      setUnits(normalizedUnits)
      setTemplate(nextTemplate)
      setReports(nextReports)
      setAccessOverview(nextAccess)
      setReportFilters((current) => ({
        ...current,
        month: effectiveMonth,
        unitId: normalizedUnits.some((item) => String(item.id) === current.unitId) ? current.unitId : '',
        centerId: resolveFilterCenters(normalizedUnits, current.unitId).some((item) => String(item.id) === current.centerId) ? current.centerId : '',
      }))
      setFormState((current) => ({
        ...current,
        reportMonth: current.reportMonth || effectiveMonth,
        statisticsUnitId: current.statisticsUnitId && normalizedUnits.some((item) => item.id === current.statisticsUnitId)
          ? current.statisticsUnitId
          : defaultUnit?.id ?? null,
        technicalCenterId: resolveCenterSelection(normalizedUnits, current.statisticsUnitId, current.technicalCenterId, defaultCenter?.id ?? null),
      }))
      setCenterDraft((current) => ({
        ...current,
        unitId: current.unitId && normalizedUnits.some((item) => String(item.id) === current.unitId)
          ? current.unitId
          : String(defaultUnit?.id ?? ''),
      }))
      resetUserForm(nextAccess, normalizedUnits)
      resetRoleForm()
      if (!visibleNavItems.some((item) => item.key === activeView)) {
        setActiveView(resolveDefaultView(user))
      }
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setLoading(false)
    }
  }

  async function refreshReportsAndStatistics(targetMonth: string) {
    try {
      const results = await Promise.all([
        currentUser && canUser(currentUser, 'dashboard:view')
          ? authorizedGet<StatisticsData>(`/api/dashboard/statistics?month=${encodeURIComponent(targetMonth)}`)
          : Promise.resolve(statistics),
        currentUser && canUser(currentUser, 'reports:view')
          ? authorizedGet<ReportSummary[]>(buildReportQuery(targetMonth, reportFilters.unitId, reportFilters.centerId, reportFilters.keyword))
          : Promise.resolve(reports),
      ])
      setStatistics(results[0])
      setReports(results[1])
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    }
  }

  async function handleReportQuery() {
    try {
      if (currentUser && canUser(currentUser, 'reports:view')) {
        const reportsRes = await authorizedGet<ReportSummary[]>(buildReportQuery(reportFilters.month, reportFilters.unitId, reportFilters.centerId, reportFilters.keyword))
        const { fieldKey, fieldOp, fieldValA, fieldValB } = reportFilters
        if (fieldKey && fieldValA !== '' && !METRIC_KEY_MAP[fieldKey]) {
          // 非 metrics 汇总字段：并行拉取完整详情，用 values[fieldKey] 过滤
          const details = await Promise.all(
            reportsRes.map((r) => authorizedGet<ReportDetail>(`/api/reports/${r.id}`))
          )
          const a = parseFloat(fieldValA)
          const b = parseFloat(fieldValB)
          const filtered = reportsRes.filter((_, i) => {
            const raw = details[i].values[fieldKey]
            if (raw === undefined || raw === null || raw === '') return false
            const v = parseFloat(raw)
            if (!isNaN(v) && !isNaN(a)) {
              if (fieldOp === '=')       return v === a
              if (fieldOp === '>=')      return v >= a
              if (fieldOp === '>')       return v > a
              if (fieldOp === '<=')      return v <= a
              if (fieldOp === '<')       return v < a
              if (fieldOp === 'between') return !isNaN(b) && v >= a && v <= b
            }
            // TEXT 字段：用 = 做包含匹配
            if (fieldOp === '=') return String(raw).includes(fieldValA)
            return false
          })
          setReports(filtered)
        } else {
          setReports(reportsRes)
        }
      }
      if (currentUser && canUser(currentUser, 'dashboard:view')) {
        const statisticsRes = await authorizedGet<StatisticsData>(`/api/dashboard/statistics?month=${encodeURIComponent(reportFilters.month || overview.latestMonth)}`)
        setStatistics(statisticsRes)
      }
      setNotice({ type: 'info', text: '已根据筛选条件刷新月报列表和统计结果。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    }
  }

  async function handleLoadReport(reportId: number) {
    try {
      const detail = await authorizedGet<ReportDetail>(`/api/reports/${reportId}`)
      setEditingReportId(detail.id)
      setFormState({
        reportMonth: detail.reportMonth,
        statisticsUnitId: detail.statisticsUnitId,
        technicalCenterId: detail.technicalCenterId,
        submitStatus: detail.submitStatus,
        values: { ...detail.values },
      })
      setActiveView('entry')
      setNotice({ type: 'info', text: '已加载选中的月报，可以继续修改。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    }
  }

  function handleNewReport() {
    const defaultUnit = units.find((item) => item.centers.length > 0) ?? units[0]
    const defaultCenter = defaultUnit?.centers[0]
    setEditingReportId(null)
    setFormState({
      reportMonth: reportFilters.month || overview.latestMonth,
      statisticsUnitId: defaultUnit?.id ?? null,
      technicalCenterId: defaultCenter?.id ?? null,
      submitStatus: 'SUBMITTED',
      values: {},
    })
    setActiveView('entry')
    setNotice({ type: 'info', text: '已切换到新建月报模式。' })
  }

  async function loadEntryReports(month: string, currentUnits?: typeof units) {
    const theUnits = currentUnits ?? units
    setLoadingEntry(true)
    try {
      const allCenterIds = theUnits.flatMap((u) => u.centers.map((c) => c.id))
      const newIdMap: Record<number, number | null> = {}
      const newValMap: Record<number, Record<string, string>> = {}
      allCenterIds.forEach((id) => { newIdMap[id] = null; newValMap[id] = {} })

      const allReports = await authorizedGet<ReportSummary[]>(`/api/reports?month=${encodeURIComponent(month)}&pageSize=500`)
      await Promise.all(
        allReports
          .filter((r) => allCenterIds.includes(r.centerId))
          .map(async (r) => {
            const detail = await authorizedGet<ReportDetail>(`/api/reports/${r.id}`)
            newIdMap[r.centerId] = r.id
            newValMap[r.centerId] = { ...detail.values }
          })
      )
      setReportIdMap(newIdMap)
      setMultiValues(newValMap)
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setLoadingEntry(false)
    }
  }

  async function handleSaveAllRows() {
    setSavingMulti(true)
    let savedCount = 0
    const failErrors: string[] = []
    const newIdMap = { ...reportIdMap }
    const allRows = units.flatMap((u) => u.centers.map((c) => ({ center: c, unit: u })))
    for (const { center, unit } of allRows) {
      const centerValues = multiValues[center.id] ?? {}
      const reportId = newIdMap[center.id]
      if (!reportId && Object.values(centerValues).every((v) => !v)) continue
      try {
        const payload = {
          reportMonth: entryMonth,
          statisticsUnitId: unit.id,
          technicalCenterId: center.id,
          submitStatus: 'SUBMITTED',
          values: centerValues,
        }
        if (reportId) {
          try {
            await authorizedJson<ReportDetail>(`/api/reports/${reportId}`, 'PUT', payload)
          } catch (putErr) {
            // If the report no longer exists (e.g. after server restart), fall back to creating a new one
            if (getErrorMessage(putErr).includes('月报不存在')) {
              newIdMap[center.id] = null
              const detail = await authorizedJson<ReportDetail>('/api/reports', 'POST', payload)
              newIdMap[center.id] = detail.id
            } else {
              throw putErr
            }
          }
        } else {
          const detail = await authorizedJson<ReportDetail>('/api/reports', 'POST', payload)
          newIdMap[center.id] = detail.id
        }
        savedCount++
      } catch (err) {
        failErrors.push(`[${center.name}] ${getErrorMessage(err)}`)
      }
    }
    setReportIdMap(newIdMap)
    await refreshReportsAndStatistics(entryMonth)
    setSavingMulti(false)
    if (failErrors.length > 0) {
      setNotice({
        type: 'error',
        text: `已保存 ${savedCount} 份，${failErrors.length} 份失败：${failErrors.join('；')}`,
      })
    } else {
      setNotice({ type: 'success', text: `已保存 ${savedCount} 份月报。` })
    }
  }

  async function handleExportEntryMonth() {
    if (!entryMonth) {
      setNotice({ type: 'error', text: '请先选择填报月份。' })
      return
    }
    setExportingEntry(true)
    try {
      await authorizedDownload(
        `/api/reports/export/monthly-table?month=${encodeURIComponent(entryMonth)}`,
        `monthly-report-table-${entryMonth}.xlsx`
      )
      setNotice({ type: 'success', text: '月报已导出。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setExportingEntry(false)
    }
  }

  async function handleDeleteReport(reportId: number) {
    if (!window.confirm('确认删除这份月报吗？删除后需要重新录入。')) {
      return
    }
    try {
      await authorizedJson<void>(`/api/reports/${reportId}`, 'DELETE')
      if (editingReportId === reportId) {
        handleNewReport()
      }
      await refreshReportsAndStatistics(reportFilters.month || overview.latestMonth)
      setNotice({ type: 'success', text: '月报已删除。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    }
  }

  async function handleCreateUnit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      await authorizedJson('/api/master-data/units', 'POST', unitDraft)
      setUnitDraft({ code: '', name: '' })
      const unitsRes = await authorizedGet<StatisticsUnitItem[]>('/api/master-data/units')
      setUnits(unitsRes)
      setNotice({ type: 'success', text: '统计单位已新增。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    }
  }

  async function handleCreateCenter(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      await authorizedJson('/api/master-data/centers', 'POST', {
        unitId: Number(centerDraft.unitId),
        code: centerDraft.code,
        name: centerDraft.name,
      })
      setCenterDraft((current) => ({ ...current, code: '', name: '' }))
      const unitsRes = await authorizedGet<StatisticsUnitItem[]>('/api/master-data/units')
      setUnits(unitsRes)
      setNotice({ type: 'success', text: '技术中心已新增。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    }
  }

  async function handleDisableCenter(centerId: number) {
    if (!window.confirm('确认停用该技术中心吗？')) {
      return
    }
    try {
      await authorizedJson<void>(`/api/master-data/centers/${centerId}`, 'DELETE')
      const unitsRes = await authorizedGet<StatisticsUnitItem[]>('/api/master-data/units')
      setUnits(unitsRes)
      setNotice({ type: 'success', text: '技术中心已停用。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    }
  }

  async function refreshAccessOverviewData() {
    const accessRes = await authorizedGet<AccessOverview>('/api/system/access-overview')
    setAccessOverview(accessRes)
    return accessRes
  }

  async function handleSaveUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setManaging(true)
    try {
      const payload = {
        username: userDraft.username,
        displayName: userDraft.displayName,
        password: userDraft.password,
        admin: userDraft.admin,
        enabled: userDraft.enabled,
        roleIds: userDraft.roleIds,
        centerIds: userDraft.admin ? [] : userDraft.centerIds,
      }
      if (editingUserId) {
        await authorizedJson(`/api/system/users/${editingUserId}`, 'PUT', payload)
      } else {
        await authorizedJson('/api/system/users', 'POST', payload)
      }
      const accessRes = await refreshAccessOverviewData()
      resetUserForm(accessRes, units)
      setNotice({ type: 'success', text: editingUserId ? '账号已更新。' : '账号已创建。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setManaging(false)
    }
  }

  async function handleResetPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!editingUserId) {
      setNotice({ type: 'error', text: '请先选择要重置密码的账号。' })
      return
    }
    setManaging(true)
    try {
      await authorizedJson(`/api/system/users/${editingUserId}/reset-password`, 'POST', { password: resetPassword })
      setResetPassword('')
      setNotice({ type: 'success', text: '密码已重置。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setManaging(false)
    }
  }

  async function handleDisableUser(userId: number) {
    if (!window.confirm('确认停用该账号吗？')) {
      return
    }
    setManaging(true)
    try {
      await authorizedJson<void>(`/api/system/users/${userId}`, 'DELETE')
      const accessRes = await refreshAccessOverviewData()
      if (editingUserId === userId) {
        resetUserForm(accessRes, units)
      }
      setNotice({ type: 'success', text: '账号已停用。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setManaging(false)
    }
  }

  async function handleSaveRole(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setManaging(true)
    try {
      const payload = {
        code: roleDraft.code,
        name: roleDraft.name,
        enabled: roleDraft.enabled,
        permissionCodes: roleDraft.permissionCodes,
      }
      if (editingRoleId) {
        await authorizedJson(`/api/system/roles/${editingRoleId}`, 'PUT', payload)
      } else {
        await authorizedJson('/api/system/roles', 'POST', payload)
      }
      await refreshAccessOverviewData()
      resetRoleForm()
      setNotice({ type: 'success', text: editingRoleId ? '角色已更新。' : '角色已创建。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setManaging(false)
    }
  }

  async function handleDisableRole(roleId: number) {
    if (!window.confirm('确认停用该角色吗？')) {
      return
    }
    setManaging(true)
    try {
      await authorizedJson<void>(`/api/system/roles/${roleId}`, 'DELETE')
      await refreshAccessOverviewData()
      if (editingRoleId === roleId) {
        resetRoleForm()
      }
      setNotice({ type: 'success', text: '角色已停用。' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setManaging(false)
    }
  }

  function handleEditUser(user: AccessUser) {
    setEditingUserId(user.id)
    setUserDraft({
      username: user.username,
      displayName: user.displayName,
      password: '',
      admin: user.admin,
      enabled: user.enabled ?? true,
      roleIds: user.roleIds ?? [],
      centerIds: user.centerIds ?? [],
    })
    setResetPassword('')
    setTimeout(() => userFormRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 0)
  }

  function handleEditRole(role: AccessRole) {
    setEditingRoleId(role.id)
    setRoleDraft({
      code: role.code,
      name: role.name,
      enabled: role.enabled ?? true,
      permissionCodes: role.permissionCodes ?? [],
    })
    setTimeout(() => roleFormRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 0)
  }

  function resetUserForm(accessData = accessOverview, availableUnits = units) {
    const defaultRoleId = accessData.roles[0]?.id
    const defaultCenterId = resolveAllCenterIds(availableUnits)[0]
    setEditingUserId(null)
    setUserDraft({
      ...emptyUserDraft,
      enabled: true,
      roleIds: defaultRoleId ? [defaultRoleId] : [],
      centerIds: defaultCenterId ? [defaultCenterId] : [],
    })
    setResetPassword('')
  }

  function resetRoleForm() {
    setEditingRoleId(null)
    setRoleDraft(emptyRoleDraft)
  }

  function persistSession(nextSession: AuthSession) {
    sessionRef.current = nextSession
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(nextSession))
  }

  function clearSession(message?: string) {
    sessionRef.current = null
    setSession(null)
    localStorage.removeItem(SESSION_STORAGE_KEY)
    setLoading(false)
    setOverview(fallbackOverview)
    setStatistics(fallbackStatistics)
    setUnits(fallbackUnits)
    setTemplate(fallbackTemplate)
    setReports(fallbackReports)
    setAccessOverview(fallbackAccessOverview)
    setActiveView('dashboard')
    if (message) {
      setNotice({ type: 'info', text: message })
    }
  }

  async function refreshSession(): Promise<AuthSession | null> {
    if (refreshPromiseRef.current) {
      return refreshPromiseRef.current
    }
    const current = sessionRef.current
    if (!current) {
      return null
    }
    refreshPromiseRef.current = (async () => {
      try {
        const payload = await requestPublicJson<AuthPayload>('/api/auth/refresh', 'POST', { refreshToken: current.refreshToken })
        const nextSession = toSession(payload)
        persistSession(nextSession)
        setSession(nextSession)
        return nextSession
      } catch {
        clearSession('登录状态已失效，请重新登录。')
        return null
      } finally {
        refreshPromiseRef.current = null
      }
    })()
    return refreshPromiseRef.current
  }

  async function fetchWithSession<T>(url: string, sessionValue: AuthSession, init?: RequestInit) {
    const response = await fetch(url, withAuthorization(init, sessionValue.accessToken))
    if (!response.ok) {
      throw await createResponseError(response)
    }
    return (await response.json()) as T
  }

  async function authorizedResponse(url: string, init?: RequestInit, allowRefresh = true): Promise<Response> {
    const current = sessionRef.current
    if (!current) {
      throw new Error('请先登录')
    }

    let response = await fetch(url, withAuthorization(init, current.accessToken))
    if (response.status !== 401 || !allowRefresh) {
      return response
    }

    const refreshed = await refreshSession()
    if (!refreshed) {
      throw new Error('登录状态已失效，请重新登录')
    }

    response = await fetch(url, withAuthorization(init, refreshed.accessToken))
    return response
  }

  async function authorizedGet<T>(url: string) {
    const response = await authorizedResponse(url)
    if (!response.ok) {
      throw await createResponseError(response)
    }
    return (await response.json()) as T
  }

  async function authorizedJson<T>(url: string, method: 'POST' | 'PUT' | 'DELETE', body?: unknown) {
    const response = await authorizedResponse(url, buildRequestInit(method, body))
    if (!response.ok) {
      throw await createResponseError(response)
    }
    if (response.status === 204) {
      return undefined as T
    }
    return (await response.json()) as T
  }

  async function authorizedDownload(url: string, fileName: string) {
    const response = await authorizedResponse(url)
    if (!response.ok) {
      throw await createResponseError(response)
    }
    const blob = await response.blob()
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(objectUrl)
  }

  if (!session) {
    return (
      <div className="auth-shell">
        <div className="auth-hero">
          <p className="eyebrow">DATA MANAGEMENT SYSTEM</p>
          <div className="auth-hero-heading">
            <h1>重大科技设施运行数据管理系统</h1>
          </div>
          <div className="auth-hero-divider" />
          <p className="auth-hero-caption">统一登录入口</p>
        </div>

        <form className="login-card" onSubmit={(event) => void handleLogin(event)}>
          <div>
            <p className="eyebrow">登录</p>
            <p className="panel-copy">请输入账号和密码。</p>
          </div>
          <label>
            <span>账号</span>
            <input
              type="text"
              value={loginDraft.username}
              onChange={(event) => setLoginDraft((current) => ({ ...current, username: event.target.value }))}
            />
          </label>
          <label>
            <span>密码</span>
            <input
              type="password"
              value={loginDraft.password}
              onChange={(event) => setLoginDraft((current) => ({ ...current, password: event.target.value }))}
            />
          </label>
          {notice ? <div className={`notice ${notice.type}`}>{notice.text}</div> : null}
          <button className="primary-button wide-button" type="submit" disabled={authSubmitting || authInitializing}>
            {authSubmitting || authInitializing ? '登录中...' : '登录并进入系统'}
          </button>
        </form>
      </div>
    )
  }

  function renderDashboard() {
    return (
      <div className="page-grid">

        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">建设进度</p>
              <h2>基础能力</h2>
            </div>
            <span className="badge">权限与规则已接入</span>
          </div>
          <div className="metric-grid">
            {overview.cards.map((card) => (
              <article className="metric-card" key={card.label}>
                <span>{card.label}</span>
                <strong>
                  {formatMetric(card.value)}
                  {card.unit ? <em>{card.unit}</em> : null}
                </strong>
                <p>{card.note}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">数据查询/统计</p>
              <h2>本月指标</h2>
            </div>
            <button className="secondary-button" onClick={() => void refreshReportsAndStatistics(reportFilters.month || overview.latestMonth)}>
              刷新统计
            </button>
          </div>
          <div className="metric-grid compact">
            {statistics.cards.map((card) => (
              <article className="metric-card compact" key={card.label}>
                <span>{card.label}</span>
                <strong>
                  {formatMetric(card.value)}
                  {card.unit ? <em>{card.unit}</em> : null}
                </strong>
                <p>{card.note}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="panel split-panel">
          <div className="table-card">
            <div className="panel-head">
              <div>
                <p className="eyebrow">统计维度</p>
                <h2>按统计单位汇总</h2>
              </div>
            </div>
            <table>
              <thead>
                <tr>
                  <th>统计单位</th>
                  <th>月报数</th>
                  <th>服务机时</th>
                  <th>开放机时</th>
                  <th>培训课时</th>
                </tr>
              </thead>
              <tbody>
                {statistics.unitBreakdown.map((row) => (
                  <tr key={row.unitName}>
                    <td>{row.unitName}</td>
                    <td>{row.reportCount}</td>
                    <td>{formatMetric(row.serviceHours)}</td>
                    <td>{formatMetric(row.openHours)}</td>
                    <td>{formatMetric(row.trainingHours)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="table-card">
            <div className="panel-head">
              <div>
                <p className="eyebrow">中心表现</p>
                <h2>服务机时靠前技术中心</h2>
              </div>
            </div>
            <table>
              <thead>
                <tr>
                  <th>技术中心</th>
                  <th>统计单位</th>
                  <th>服务机时</th>
                  <th>运行机时</th>
                </tr>
              </thead>
              <tbody>
                {statistics.centerHighlights.map((row) => (
                  <tr key={`${row.unitName}-${row.centerName}`}>
                    <td>{row.centerName}</td>
                    <td>{row.unitName}</td>
                    <td>{formatMetric(row.serviceHours)}</td>
                    <td>{formatMetric(row.runHours)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel split-panel">
          <div className="note-card">
            <p className="eyebrow">查询亮点</p>
            <h2>便于财务统计和月度追踪</h2>
            <ul className="plain-list">
              {overview.queryHighlights.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </div>
          <div className="note-card accent">
            <p className="eyebrow">权限规则</p>
            <h2>页面权限 + 数据权限</h2>
            <p>{accessOverview.dataRuleNote}</p>
            {canUser(session!.user, 'system:user:view') ? (
              <button className="primary-button" onClick={() => setActiveView('users')}>
                查看账号管理
              </button>
            ) : null}
          </div>
        </section>
      </div>
    )
  }

  function renderReportEntry() {
    // Build header spans from template groups
    const redHeaderGroups = new Set(['基本运行情况', '对外开放情况'])
    const groupSpans: { name: string; colSpan: number; hasSections: boolean; gi: number; redHeader: boolean }[] = []
    const sectionSpans: { name: string; colSpan: number; isEmpty: boolean; gi: number; redHeader: boolean }[] = []
    const orderedFields: (import('./types').TemplateField & { gi: number })[] = []

    let gi = 0
    for (const group of template.groups) {
      const hasSections = group.sections.length > 1 || group.sections.some((s) => s.name !== group.name)
      let groupCols = 0
      const redHeader = redHeaderGroups.has(group.name)
      for (const section of group.sections) {
        groupCols += section.fields.length
        sectionSpans.push({ name: section.name, colSpan: section.fields.length, isEmpty: !hasSections, gi, redHeader })
        for (const field of section.fields) {
          orderedFields.push({ ...field, gi })
        }
      }
      groupSpans.push({ name: group.name, colSpan: groupCols, hasSections, gi, redHeader })
      gi++
    }

    // All center rows grouped by unit
    const centerRows = units.flatMap((unit) =>
      unit.centers.map((center, idx) => ({
        center,
        unit,
        isFirstInUnit: idx === 0,
        unitCenterCount: unit.centers.length,
      }))
    )

    return (
      <div className="page-grid">
        {/* Top controls */}
        <section className="panel entry-controls-bar">
          <div className="panel-head">
            <div className="entry-controls-title">
              <p className="eyebrow">Excel 风格录入</p>
              <h2>月报填报</h2>
            </div>
            <div className="toolbar-actions wrap-actions">
              <label className="entry-month-label">
                <span>填报月份</span>
                <input
                  type="month"
                  value={entryMonth}
                  onChange={(event) => setEntryMonth(event.target.value)}
                />
              </label>
              <button className="primary-button" disabled={savingMulti} onClick={() => void handleSaveAllRows()}>
                {savingMulti ? '保存中...' : '保存全部月报'}
              </button>
              {canUser(session!.user, 'reports:export') ? (
                <button className="secondary-button" disabled={exportingEntry} onClick={() => void handleExportEntryMonth()}>
                  {exportingEntry ? '导出中...' : '导出月报'}
                </button>
              ) : null}
            </div>
          </div>

        </section>

        {/* Excel table */}
        <section className={`panel entry-table-panel${entryFullscreen ? ' entry-fullscreen' : ''}`}>
          <button
            className="fullscreen-toggle-btn"
            title={entryFullscreen ? '退出全屏' : '全屏查看'}
            onClick={() => setEntryFullscreen((v) => !v)}
          >
            {entryFullscreen ? '⊡' : '⊞'}
            <span>{entryFullscreen ? '退出全屏' : '全屏'}</span>
          </button>
          <div className="excel-table-wrap">
            <table className="excel-table">
              <thead>
                <tr className="header-row-group">
                  <th className="sticky-col sticky-unit col-header" rowSpan={3}>统计单位</th>
                  <th className="sticky-col sticky-center col-header" rowSpan={3}>技术平台/技术中心</th>
                  {groupSpans.map((g) => (
                    <th key={g.gi} className={`col-header group-header gc-${g.gi % 6}${g.redHeader ? ' red-header' : ''}`} colSpan={g.colSpan} rowSpan={g.hasSections ? 1 : 2}>
                      {g.name}
                    </th>
                  ))}
                </tr>
                <tr className="header-row-section">
                  {sectionSpans.map((s, si) =>
                    s.isEmpty ? null : (
                      <th key={si} className={`col-header section-header gc-${s.gi % 6}${s.redHeader ? ' red-header' : ''}`} colSpan={s.colSpan}>{s.name}</th>
                    )
                  )}
                </tr>
                <tr className="header-row-field">
                  {orderedFields.map((field) => (
                    <th key={field.key} className={`col-header field-header gc-${field.gi % 6}${redHeaderGroups.has(field.groupName) ? ' red-header' : ''}${field.readOnly ? ' readonly-col' : ''}`}>
                      <div className="fh-label">{field.label}</div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {centerRows.map(({ center, unit, isFirstInUnit, unitCenterCount }, rowIdx) => {
                  const rowValues = resolveTemplateValues(template.fields, multiValues[center.id] ?? {})
                  const hasReport = Boolean(reportIdMap[center.id])
                  return (
                    <tr key={center.id} className={hasReport ? 'row-has-report' : 'row-empty'}>
                      {isFirstInUnit ? (
                        <td className="sticky-col sticky-unit unit-cell" rowSpan={unitCenterCount}>
                          <div className="cell-wrap">{unit.name}</div>
                        </td>
                      ) : null}
                      <td className={`sticky-col sticky-center center-cell center-cell-${rowIdx % 2 === 0 ? 'even' : 'odd'}`}><div className="cell-wrap">{center.name}</div></td>
                      {orderedFields.map((field) => (
                        <td key={field.key} className={`data-cell${field.readOnly ? ' readonly-cell' : ''}`}>
                          <input
                            className="excel-input"
                            type={field.valueType === 'DECIMAL' ? 'number' : 'text'}
                            step={field.valueType === 'DECIMAL' ? 'any' : undefined}
                            value={rowValues[field.key] ?? ''}
                            disabled={field.readOnly}
                            onChange={(event) => {
                              const val = event.target.value
                              setMultiValues((prev) => ({
                                ...prev,
                                [center.id]: { ...(prev[center.id] ?? {}), [field.key]: val },
                              }))
                            }}
                          />
                        </td>
                      ))}
                    </tr>
                  )
                })}
              </tbody>
              <tfoot>
                <tr>
                  <td className="entry-table-note" colSpan={orderedFields.length + 2}>
                    注：医院按照企业类别统计
                  </td>
                </tr>
              </tfoot>
            </table>
            {loadingEntry ? <div className="entry-loading">加载数据中…</div> : null}
            {!loadingEntry && centerRows.length === 0 ? (
              <div className="entry-loading">当前账号没有可填报的技术中心。</div>
            ) : null}
          </div>
        </section>
      </div>
    )
  }

  function renderReportList() {
    return (
      <div className="page-grid">
        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">查询与列表</p>
              <h2>月报筛选</h2>
            </div>
            <div className="toolbar-actions wrap-actions">
              <button className="secondary-button" onClick={() => setReportFilters({ month: overview.latestMonth, unitId: '', centerId: '', keyword: '', fieldKey: '', fieldOp: '=', fieldValA: '', fieldValB: '' })}>
                重置条件
              </button>
              <button className="primary-button" onClick={() => void handleReportQuery()}>
                查询月报
              </button>
            </div>
          </div>
          <div className="filter-grid">
            <label className="filter-month">
              <span>月份</span>
              <input
                type="month"
                value={reportFilters.month}
                onChange={(event) => setReportFilters((current) => ({ ...current, month: event.target.value }))}
              />
            </label>
            <label>
              <span>统计单位</span>
              <select
                value={reportFilters.unitId}
                onChange={(event) => setReportFilters((current) => ({ ...current, unitId: event.target.value, centerId: '' }))}
              >
                <option value="">全部</option>
                {units.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>技术中心</span>
              <select
                value={reportFilters.centerId}
                onChange={(event) => setReportFilters((current) => ({ ...current, centerId: event.target.value }))}
              >
                <option value="">全部</option>
                {resolveFilterCenters(units, reportFilters.unitId).map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
            </label>
          </div>
          {/* 字段条件筛选行 */}
          <div className="field-filter-row">
            <label className="field-filter-label">
              <span>字段</span>
              <select
                value={reportFilters.fieldKey}
                onChange={(event) => setReportFilters((current) => ({ ...current, fieldKey: event.target.value }))}
              >
                <option value="">-- 选择字段 --</option>
                {template.groups.map((group) =>
                  group.sections.map((section) => {
                    const allFields = section.fields
                    if (allFields.length === 0) return null
                    // 同名时只显示一级标题，否则显示"一级 › 二级"路径
                    const optLabel =
                      section.name === group.name
                        ? group.name
                        : `${group.name} › ${section.name}`
                    return (
                      <optgroup key={`${group.name}-${section.name}`} label={optLabel}>
                        {allFields.map((f) => (
                          <option key={f.key} value={f.key}>
                            {f.label}
                          </option>
                        ))}
                      </optgroup>
                    )
                  })
                )}
              </select>
            </label>
            <label className="field-filter-label field-filter-op">
              <span>运算符</span>
              <select
                value={reportFilters.fieldOp}
                onChange={(event) => setReportFilters((current) => ({ ...current, fieldOp: event.target.value }))}
              >
                <option value="=">=&nbsp;&nbsp;等于</option>
                <option value=">=">&gt;= 大于等于</option>
                <option value=">">&gt;&nbsp;&nbsp;大于</option>
                <option value="<=">&lt;= 小于等于</option>
                <option value="<">&lt;&nbsp;&nbsp;小于</option>
                <option value="between">介于</option>
              </select>
            </label>
            <label className="field-filter-label field-filter-val">
              <span>{reportFilters.fieldOp === 'between' ? '最小值' : '数值'}</span>
              <input
                type="number"
                placeholder="输入数值"
                value={reportFilters.fieldValA}
                onChange={(event) => setReportFilters((current) => ({ ...current, fieldValA: event.target.value }))}
              />
            </label>
            {reportFilters.fieldOp === 'between' ? (
              <label className="field-filter-label field-filter-val">
                <span>最大值</span>
                <input
                  type="number"
                  placeholder="输入数值"
                  value={reportFilters.fieldValB}
                  onChange={(event) => setReportFilters((current) => ({ ...current, fieldValB: event.target.value }))}
                />
              </label>
            ) : null}
          </div>
        </section>

        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">结果列表</p>
              <h2>月报记录</h2>
            </div>
            {canUser(session!.user, 'reports:edit') ? (
              <button className="secondary-button" onClick={handleNewReport}>新增月报</button>
            ) : null}
          </div>
          <div className="table-card">
            <table className="report-list-table">
              <thead>
                <tr>
                  <th>月份</th>
                  <th>统计单位</th>
                  <th>技术中心</th>
                  <th>运行机时</th>
                  <th>企业用户机时</th>
                  <th>上海用户机时</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {applyFieldFilter(reports, reportFilters.fieldKey, reportFilters.fieldOp, reportFilters.fieldValA, reportFilters.fieldValB).map((item) => (
                  <tr key={item.id}>
                    <td>{item.reportMonth}</td>
                    <td>{item.unitName}</td>
                    <td>{item.centerName}</td>
                    <td>{formatMetric(item.metrics.runHours)}</td>
                    <td>{formatMetric(item.metrics.enterpriseUserHours)}</td>
                    <td>{formatMetric(item.metrics.shanghaiUserHours)}</td>
                    <td>
                      <span className={`status-pill ${item.auditStatus.toLowerCase()}`}>{item.auditStatus}</span>
                    </td>
                    <td>
                      <div className="row-actions wrap-actions">
                        {canUser(session!.user, 'reports:edit') ? (
                          <button className="inline-button" onClick={() => void handleLoadReport(item.id)}>编辑</button>
                        ) : null}
                        {canUser(session!.user, 'reports:export') ? (
                          null
                        ) : null}
                        {canUser(session!.user, 'reports:edit') ? (
                          <button className="inline-button danger" onClick={() => void handleDeleteReport(item.id)}>删除</button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    )
  }

  function renderMasterData() {
    return (
      <div className="page-grid">
        <section className="panel split-panel">
          <form className="note-card" onSubmit={(event) => void handleCreateUnit(event)}>
            <p className="eyebrow">主数据维护</p>
            <h2>新增统计单位</h2>
            <div className="stack-form">
              <input
                type="text"
                placeholder="编码，留空自动生成"
                value={unitDraft.code}
                onChange={(event) => setUnitDraft((current) => ({ ...current, code: event.target.value }))}
              />
              <input
                type="text"
                placeholder="统计单位名称"
                value={unitDraft.name}
                onChange={(event) => setUnitDraft((current) => ({ ...current, name: event.target.value }))}
              />
              <button className="primary-button" type="submit">新增统计单位</button>
            </div>
          </form>

          <form className="note-card accent" onSubmit={(event) => void handleCreateCenter(event)}>
            <p className="eyebrow">主数据维护</p>
            <h2>新增技术中心</h2>
            <div className="stack-form">
              <select
                value={centerDraft.unitId}
                onChange={(event) => setCenterDraft((current) => ({ ...current, unitId: event.target.value }))}
              >
                {units.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
              <input
                type="text"
                placeholder="编码，留空自动生成"
                value={centerDraft.code}
                onChange={(event) => setCenterDraft((current) => ({ ...current, code: event.target.value }))}
              />
              <input
                type="text"
                placeholder="技术中心名称"
                value={centerDraft.name}
                onChange={(event) => setCenterDraft((current) => ({ ...current, name: event.target.value }))}
              />
              <button className="primary-button" type="submit">新增技术中心</button>
            </div>
          </form>
        </section>

        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">当前主数据</p>
              <h2>统计单位 / 技术中心</h2>
            </div>
          </div>
          <div className="unit-grid">
            {units.map((unit) => (
              <article className="unit-card" key={unit.id}>
                <div className="unit-head">
                  <div>
                    <p>{unit.name}</p>
                    <span>{unit.code}</span>
                  </div>
                  <span className="badge">{unit.centers.length} 个中心</span>
                </div>
                <div className="center-list">
                  {unit.centers.map((center) => (
                    <div className="center-item" key={center.id}>
                      <div>
                        <strong>{center.name}</strong>
                        <span>{center.code}</span>
                      </div>
                      {canUser(session!.user, 'master-data:edit') ? (
                        <button className="inline-button danger" onClick={() => void handleDisableCenter(center.id)}>
                          停用
                        </button>
                      ) : null}
                    </div>
                  ))}
                </div>
              </article>
            ))}
          </div>
        </section>
      </div>
    )
  }

  function renderUserManagement() {
    return (
      <div className="page-grid">
        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">账号管理</p>
              <h2>{editingUserId ? '编辑账号' : '新建账号'}</h2>
            </div>
            {editingUserId ? (
              <button className="secondary-button" type="button" onClick={() => resetUserForm()}>取消编辑</button>
            ) : null}
          </div>

          <form ref={userFormRef} onSubmit={(event) => void handleSaveUser(event)}>
            <div className="user-form-grid">
              <div className="user-form-left">
                <p className="section-caption" style={{ marginBottom: 12 }}>基本信息</p>
                <div className="stack-form">
                  <label className="field-label">
                    <span>账号</span>
                    <input
                      type="text"
                      placeholder="登录账号"
                      value={userDraft.username}
                      onChange={(event) => setUserDraft((current) => ({ ...current, username: event.target.value }))}
                    />
                  </label>
                  <label className="field-label">
                    <span>姓名</span>
                    <input
                      type="text"
                      placeholder="显示名称"
                      value={userDraft.displayName}
                      onChange={(event) => setUserDraft((current) => ({ ...current, displayName: event.target.value }))}
                    />
                  </label>
                  {!editingUserId ? (
                    <label className="field-label">
                      <span>初始密码</span>
                      <input
                        type="password"
                        placeholder="首次登录密码"
                        value={userDraft.password}
                        onChange={(event) => setUserDraft((current) => ({ ...current, password: event.target.value }))}
                      />
                    </label>
                  ) : null}
                </div>

                <div className="check-list" style={{ marginTop: 20 }}>
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={userDraft.admin}
                      onChange={(event) => setUserDraft((current) => ({ ...current, admin: event.target.checked, centerIds: event.target.checked ? [] : current.centerIds }))}
                    />
                    <span>管理员账号（拥有全部权限）</span>
                  </label>
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={userDraft.enabled}
                      onChange={(event) => setUserDraft((current) => ({ ...current, enabled: event.target.checked }))}
                    />
                    <span>启用状态</span>
                  </label>
                </div>

                <div className="check-section">
                  <p className="section-caption">角色分配</p>
                  <div className="check-grid single-col">
                    {accessOverview.roles.map((role) => (
                      <label className="check-card" key={role.id}>
                        <input
                          type="checkbox"
                          checked={userDraft.roleIds.includes(role.id)}
                          onChange={(event) => setUserDraft((current) => ({
                            ...current,
                            roleIds: toggleNumberItem(current.roleIds, role.id, event.target.checked),
                          }))}
                        />
                        <span>{role.name}</span>
                        <em>{role.code}</em>
                      </label>
                    ))}
                  </div>
                </div>

                <div style={{ marginTop: 24 }}>
                  <button className="primary-button" type="submit" disabled={managing}>
                    {managing ? '保存中...' : editingUserId ? '保存账号' : '创建账号'}
                  </button>
                </div>
              </div>

              <div className="user-form-right">
                <p className="section-caption" style={{ marginBottom: 12 }}>中心级数据权限</p>
                {userDraft.admin ? (
                  <p className="form-hint">管理员账号自动拥有全部技术中心权限，无需单独配置。</p>
                ) : null}
                <div className="unit-check-grid" style={{ opacity: userDraft.admin ? 0.4 : 1, pointerEvents: userDraft.admin ? 'none' : undefined }}>
                  {units.map((unit) => (
                    <div className="unit-check-card" key={unit.id}>
                      <strong>{unit.name}</strong>
                      <div className="check-grid compact-grid">
                        {unit.centers.map((center) => (
                          <label className="check-card small" key={center.id}>
                            <input
                              type="checkbox"
                              checked={userDraft.admin ? false : userDraft.centerIds.includes(center.id)}
                              disabled={userDraft.admin}
                              onChange={(event) => setUserDraft((current) => ({
                                ...current,
                                centerIds: toggleNumberItem(current.centerIds, center.id, event.target.checked),
                              }))}
                            />
                            <span>{center.name}</span>
                          </label>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </form>
        </section>

        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">账号列表</p>
              <h2>用户与可操作中心</h2>
            </div>
          </div>
          <div className="table-card">
            <table className="compact-table">
              <thead>
                <tr>
                  <th>账号</th>
                  <th>姓名</th>
                  <th>角色</th>
                  <th>数据范围</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {accessOverview.users.map((user) => (
                  <tr key={user.id} className={user.enabled === false ? 'disabled-row' : ''}>
                    <td>{user.username}</td>
                    <td>{user.displayName}</td>
                    <td>{user.roles.join('、') || (user.admin ? '系统管理员' : '-')}</td>
                    <td>{user.admin ? '全部技术中心' : user.centers.join('、') || '未配置'}</td>
                    <td>{user.enabled === false ? '已停用' : '启用中'}</td>
                    <td>
                      <div className="row-actions wrap-actions">
                        <button className="inline-button" onClick={() => handleEditUser(user)}>编辑</button>
                        <button className="inline-button" onClick={() => { handleEditUser(user); setActiveView('reset-password') }}>重置密码</button>
                        <button className="inline-button danger" onClick={() => void handleDisableUser(user.id)}>停用</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    )
  }

  function renderResetPassword() {
    const selectedUser = editingUserId
      ? accessOverview.users.find((u) => u.id === editingUserId) ?? null
      : null

    return (
      <div className="page-grid">
        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">密码管理</p>
              <h2>重置账号密码</h2>
            </div>
            <button className="secondary-button" type="button" onClick={() => setActiveView('users')}>
              返回账号管理
            </button>
          </div>

          <div className="reset-password-layout">
            <div className="reset-password-pick">
              <p className="section-caption" style={{ marginBottom: 12 }}>选择账号</p>
              <div className="reset-user-list">
                {accessOverview.users.map((user) => (
                  <button
                    key={user.id}
                    type="button"
                    className={`reset-user-item ${editingUserId === user.id ? 'active' : ''}`}
                    onClick={() => handleEditUser(user)}
                  >
                    <strong>{user.displayName}</strong>
                    <span>{user.username}</span>
                    <em>{user.roles.join('、') || (user.admin ? '系统管理员' : '未分配角色')}</em>
                  </button>
                ))}
              </div>
            </div>

            <div className="reset-password-form">
              {selectedUser ? (
                <form onSubmit={(event) => void handleResetPassword(event)}>
                  <div className="reset-user-banner">
                    <div>
                      <p className="eyebrow">当前操作账号</p>
                      <h3>{selectedUser.displayName}</h3>
                      <span>{selectedUser.username}</span>
                    </div>
                    <span className={`status-pill ${selectedUser.enabled === false ? 'disabled' : 'active-pill'}`}>
                      {selectedUser.enabled === false ? '已停用' : '启用中'}
                    </span>
                  </div>
                  <div className="stack-form" style={{ marginTop: 24 }}>
                    <label className="field-label">
                      <span>新密码</span>
                      <input
                        type="password"
                        placeholder="输入新密码（至少 6 位）"
                        value={resetPassword}
                        onChange={(event) => setResetPassword(event.target.value)}
                      />
                    </label>
                  </div>
                  <div style={{ marginTop: 20, display: 'flex', gap: 12 }}>
                    <button className="primary-button" type="submit" disabled={!resetPassword || managing}>
                      {managing ? '重置中...' : '确认重置密码'}
                    </button>
                    <button className="secondary-button" type="button" onClick={() => { setEditingUserId(null); setResetPassword('') }}>
                      取消选择
                    </button>
                  </div>
                </form>
              ) : (
                <div className="reset-empty-state">
                  <p className="eyebrow">尚未选择账号</p>
                  <h3>请在左侧选择要重置密码的账号</h3>
                  <p>点击左侧账号列表中的任意一条记录即可开始操作。</p>
                </div>
              )}
            </div>
          </div>
        </section>
      </div>
    )
  }

  function renderRoleManagement() {
    return (
      <div className="page-grid">
        <section className="panel">
          <form className="role-form-inner" ref={roleFormRef} onSubmit={(event) => void handleSaveRole(event)}>
            <div className="panel-head">
              <div>
                <p className="eyebrow">角色管理</p>
                <h2>{editingRoleId ? '编辑角色' : '新建角色'}</h2>
              </div>
              {editingRoleId ? <button className="secondary-button" type="button" onClick={resetRoleForm}>取消编辑</button> : null}
            </div>
            <div className="role-form-row">
              <input
                type="text"
                placeholder="角色编码，如 REPORT_VIEWER"
                value={roleDraft.code}
                onChange={(event) => setRoleDraft((current) => ({ ...current, code: event.target.value.toUpperCase() }))}
              />
              <input
                type="text"
                placeholder="角色名称"
                value={roleDraft.name}
                onChange={(event) => setRoleDraft((current) => ({ ...current, name: event.target.value }))}
              />
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={roleDraft.enabled}
                  onChange={(event) => setRoleDraft((current) => ({ ...current, enabled: event.target.checked }))}
                />
                <span>启用</span>
              </label>
              <button className="primary-button" type="submit" disabled={managing}>{managing ? '保存中...' : editingRoleId ? '保存角色' : '创建角色'}</button>
            </div>
            <div className="check-section">
              <p className="section-caption">权限授权</p>
              <div className="check-grid">
                {accessOverview.permissionCatalog.map((permission) => (
                  <label className="check-card" key={permission.code}>
                    <input
                      type="checkbox"
                      checked={roleDraft.permissionCodes.includes(permission.code)}
                      onChange={(event) => setRoleDraft((current) => ({
                        ...current,
                        permissionCodes: toggleStringItem(current.permissionCodes, permission.code, event.target.checked),
                      }))}
                    />
                    <span>{permission.name}</span>
                    <em>{permission.code}</em>
                  </label>
                ))}
              </div>
            </div>
          </form>
        </section>

        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">角色列表</p>
              <h2>角色与页面权限</h2>
            </div>
          </div>
          <div className="table-card">
            <table className="compact-table">
              <thead>
                <tr>
                  <th>角色</th>
                  <th>编码</th>
                  <th>权限范围</th>
                  <th>状态</th>
                  <th className="actions-col">操作</th>
                </tr>
              </thead>
              <tbody>
                {accessOverview.roles.map((role) => (
                  <tr key={role.id} className={role.enabled === false ? 'disabled-row' : ''}>
                    <td>{role.name}</td>
                    <td>{role.code}</td>
                    <td className="perm-col">{role.permissions.join('、')}</td>
                    <td>{role.enabled === false ? '已停用' : '启用中'}</td>
                    <td className="actions-col">
                      <div className="row-actions">
                        <button className="inline-button" onClick={() => handleEditRole(role)}>编辑</button>
                        {role.code !== 'ADMIN' ? (
                          <button className="inline-button danger" onClick={() => void handleDisableRole(role.id)}>停用</button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">权限目录</p>
              <h2>页面与动作权限清单</h2>
            </div>
          </div>
          <div className="permission-grid">
            {accessOverview.permissionCatalog.map((permission: PermissionCatalogItem) => (
              <article className="permission-card" key={permission.code}>
                <span>{permission.permissionType}</span>
                <strong>{permission.name}</strong>
                <p>{permission.code}</p>
                <em>{permission.routePath}</em>
              </article>
            ))}
          </div>
        </section>
      </div>
    )
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-logo-row">
            <svg className="brand-logo" viewBox="0 0 44 44" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
              {/* 外圆背景 */}
              <circle cx="22" cy="22" r="21" fill="rgba(100,180,255,0.18)" stroke="rgba(255,255,255,0.28)" strokeWidth="1"/>
              {/* 柱状图 - 财务元素 */}
              <rect x="8" y="26" width="5" height="10" rx="1.5" fill="rgba(255,255,255,0.55)"/>
              <rect x="15.5" y="20" width="5" height="16" rx="1.5" fill="rgba(255,255,255,0.78)"/>
              <rect x="23" y="14" width="5" height="22" rx="1.5" fill="rgba(255,255,255,0.95)"/>
              <rect x="30.5" y="18" width="5" height="18" rx="1.5" fill="rgba(255,255,255,0.68)"/>
              {/* 折线趋势 */}
              <polyline points="10.5,25 18,19 25.5,13 33,17" stroke="rgba(120,220,200,0.9)" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
              {/* 趋势箭头点 */}
              <circle cx="33" cy="17" r="2" fill="rgba(120,220,200,1)"/>
            </svg>
            <div className="brand-text">
              <p>重大科技设施<br />运行数据管理系统</p>
            </div>
          </div>
        </div>

        <nav className="nav-list">
          {visibleNavItems.map((item) => (
            <button
              key={item.key}
              className={`nav-item ${activeView === item.key ? 'active' : ''}`}
              onClick={() => setActiveView(item.key)}
            >
              <span className="nav-item-inner">
                {navIcons[item.key] ?? null}
                <strong>{item.label}</strong>
              </span>
              <span className="nav-item-desc">{item.description}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <span>当前账号</span>
          <strong>{session!.user.displayName}</strong>
          <span>{session!.user.roles.join('、') || (session!.user.admin ? '系统管理员' : '未分配')}</span>
        </div>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div>
            <p className="eyebrow">独立项目工作区</p>
            <h1>重大科技设施运行数据管理系统</h1>
          </div>
          <div className="topbar-actions wrap-actions">
            <div className="topbar-meta">
              <span>{session!.user.username}</span>
              <strong>{overview.latestMonth}</strong>
            </div>
            <button className="secondary-button" onClick={() => void loadWorkspaceData(session!.user, reportFilters.month || overview.latestMonth)}>
              {loading ? '加载中...' : '刷新数据'}
            </button>
            {canUser(session!.user, 'reports:edit') ? (
              <button className="primary-button" onClick={handleNewReport}>快速新建月报</button>
            ) : null}
            <button className="secondary-button" onClick={() => void handleLogout()}>
              退出登录
            </button>
          </div>
        </header>

        {notice ? <div className={`notice ${notice.type}`}>{notice.text}</div> : null}

        <main className="content-shell">
          {loading || authInitializing ? <section className="panel loading-panel">正在加载当前账号可访问的数据...</section> : null}
          {!loading && !authInitializing && activeView === 'dashboard' ? renderDashboard() : null}
          {!loading && !authInitializing && activeView === 'entry' ? renderReportEntry() : null}
          {!loading && !authInitializing && activeView === 'reports' ? renderReportList() : null}
          {!loading && !authInitializing && activeView === 'master' ? renderMasterData() : null}
          {!loading && !authInitializing && activeView === 'users' ? renderUserManagement() : null}
          {!loading && !authInitializing && activeView === 'reset-password' ? renderResetPassword() : null}
          {!loading && !authInitializing && activeView === 'roles' ? renderRoleManagement() : null}
        </main>
      </div>
    </div>
  )
}

export default App

function readStoredSession(): AuthSession | null {
  try {
    const rawValue = localStorage.getItem(SESSION_STORAGE_KEY)
    if (!rawValue) {
      return null
    }
    return JSON.parse(rawValue) as AuthSession
  } catch {
    return null
  }
}

function toSession(payload: AuthPayload): AuthSession {
  return {
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken,
    accessTokenExpiresAt: payload.accessTokenExpiresAt,
    refreshTokenExpiresAt: payload.refreshTokenExpiresAt,
    user: payload.user,
  }
}

function buildRequestInit(method: 'POST' | 'PUT' | 'DELETE', body?: unknown): RequestInit {
  if (body instanceof FormData) {
    return { method, body }
  }
  return {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  }
}

function withAuthorization(init: RequestInit | undefined, accessToken: string): RequestInit {
  const headers = new Headers(init?.headers)
  headers.set('Authorization', `Bearer ${accessToken}`)
  return {
    ...init,
    headers,
  }
}

async function requestPublicJson<T>(url: string, method: 'POST' | 'PUT' | 'DELETE', body?: unknown) {
  const response = await fetch(url, buildRequestInit(method, body))
  if (!response.ok) {
    throw await createResponseError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function createResponseError(response: Response): Promise<Error> {
  try {
    const payload = (await response.json()) as { message?: string }
    return new Error(payload.message || response.statusText || '请求失败')
  } catch {
    return new Error(response.statusText || '请求失败')
  }
}

function buildReportQuery(month: string, unitId: string, centerId: string, keyword: string) {
  const params = new URLSearchParams()
  if (month) {
    params.set('month', month)
  }
  if (unitId) {
    params.set('unitId', unitId)
  }
  if (centerId) {
    params.set('centerId', centerId)
  }
  if (keyword) {
    params.set('keyword', keyword)
  }
  const query = params.toString()
  return query ? `/api/reports?${query}` : '/api/reports'
}

// 字段 key → ReportSummary.metrics 属性映射
const METRIC_KEY_MAP: Record<string, keyof import('./types').ReportSummary['metrics']> = {
  run_hours: 'runHours',
  service_hours: 'serviceHours',
  open_hours_total: 'openHours',
  enterprise_user_hours: 'enterpriseUserHours',
  open_hours_shanghai: 'shanghaiUserHours',
  training_hours: 'trainingHours',
  enterprise_training_hours: 'trainingHours',
  safety_training_hours: 'trainingHours',
}

function applyFieldFilter(
  items: import('./types').ReportSummary[],
  fieldKey: string,
  op: string,
  valA: string,
  valB: string,
): import('./types').ReportSummary[] {
  if (!fieldKey || !op || valA === '') return items
  const metricProp = METRIC_KEY_MAP[fieldKey]
  if (!metricProp) return items // 不在汇总字段范围内，暂不过滤
  const a = parseFloat(valA)
  if (isNaN(a)) return items
  return items.filter((r) => {
    const raw = r.metrics[metricProp]
    const v = typeof raw === 'string' ? parseFloat(raw) : (raw as number)
    if (isNaN(v)) return false
    if (op === '=')  return v === a
    if (op === '>=') return v >= a
    if (op === '>')  return v > a
    if (op === '<=') return v <= a
    if (op === '<')  return v < a
    if (op === 'between') {
      const b = parseFloat(valB)
      return !isNaN(b) && v >= a && v <= b
    }
    return true
  })
}

function resolveFilterCenters(units: StatisticsUnitItem[], unitId: string) {
  if (!unitId) {
    return units.flatMap((item) => item.centers)
  }
  return units.find((item) => String(item.id) === unitId)?.centers ?? []
}

function resolveCenterSelection(units: StatisticsUnitItem[], unitId: number | null, centerId: number | null, fallbackCenterId: number | null) {
  const unit = units.find((item) => item.id === unitId) ?? units.find((item) => item.centers.some((center) => center.id === centerId))
  const centers = unit?.centers ?? []
  if (centerId && centers.some((center) => center.id === centerId)) {
    return centerId
  }
  return fallbackCenterId
}

function formatMetric(value: number | string) {
  if (typeof value === 'number') {
    return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1 }).format(value)
  }
  const numericValue = Number(value)
  if (!Number.isNaN(numericValue) && value !== '') {
    return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1 }).format(numericValue)
  }
  return value
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return '请求失败，请稍后重试。'
}

function canUser(user: AuthUser | null, permission: string) {
  return Boolean(user && (user.admin || user.permissions.includes(permission)))
}

function canAnyPermission(user: AuthUser | null, permissions: string[]) {
  return Boolean(user && (user.admin || permissions.some((permission) => user.permissions.includes(permission))))
}

function resolveDefaultView(user: AuthUser) {
  return navItems.find((item) => canUser(user, item.permission))?.key ?? 'dashboard'
}


function buildUnitsFromCenters(centers: AuthUser['centers']): StatisticsUnitItem[] {
  const groups = new Map<number, StatisticsUnitItem>()
  centers.forEach((center) => {
    const existing = groups.get(center.unitId)
    if (existing) {
      existing.centers.push({ id: center.id, code: `CENTER_${center.id}`, name: center.name, unitId: center.unitId, unitName: center.unitName })
      return
    }
    groups.set(center.unitId, {
      id: center.unitId,
      code: `UNIT_${center.unitId}`,
      name: center.unitName,
      centers: [{ id: center.id, code: `CENTER_${center.id}`, name: center.name, unitId: center.unitId, unitName: center.unitName }],
    })
  })
  return Array.from(groups.values())
}

function resolveTemplateValues(fields: TemplateField[], values: Record<string, string>) {
  const nextValues: Record<string, string> = { ...values }
  fields.forEach((field) => {
    if (!field.formulaExpression) {
      return
    }
    nextValues[field.key] = evaluateFormula(field.formulaExpression, nextValues)
  })
  return nextValues
}

function evaluateFormula(expression: string, values: Record<string, string>) {
  const tokens = expression.trim().split(/\s+/)
  if (tokens.length === 0) {
    return '0'
  }
  let total = toNumber(values[tokens[0]])
  for (let index = 1; index < tokens.length; index += 2) {
    const operator = tokens[index]
    const operand = toNumber(values[tokens[index + 1]])
    total = operator === '-' ? total - operand : total + operand
  }
  return formatNumberString(Math.max(total, 0))
}

function toNumber(value: string | undefined) {
  const numericValue = Number(value ?? 0)
  return Number.isFinite(numericValue) ? numericValue : 0
}

function formatNumberString(value: number) {
  if (Number.isInteger(value)) {
    return String(value)
  }
  return value.toFixed(2).replace(/\.00$/, '').replace(/(\.\d*[1-9])0+$/, '$1')
}

function buildEntrySummary(values: Record<string, string>) {
  const trainingTotal = toNumber(values.training_hours) + toNumber(values.enterprise_training_hours) + toNumber(values.safety_training_hours)
  return [
    { label: '运行机时', value: values.run_hours ?? '0', unit: '小时', note: '不得小于服务机时' },
    { label: '服务机时', value: values.service_hours ?? '0', unit: '小时', note: '用于校验开放与企业服务机时' },
    { label: '对外开放机时', value: values.open_hours_total ?? '0', unit: '小时', note: '由国际与国内用户机时自动汇总' },
    { label: '培训课时合计', value: trainingTotal, unit: '小时', note: '三类培训课时自动求和展示' },
  ]
}

function toggleNumberItem(items: number[], value: number, checked: boolean) {
  if (checked) {
    return Array.from(new Set([...items, value]))
  }
  return items.filter((item) => item !== value)
}

function toggleStringItem(items: string[], value: string, checked: boolean) {
  if (checked) {
    return Array.from(new Set([...items, value]))
  }
  return items.filter((item) => item !== value)
}

function resolveAllCenterIds(units: StatisticsUnitItem[]) {
  return units.flatMap((unit) => unit.centers.map((center) => center.id))
}
