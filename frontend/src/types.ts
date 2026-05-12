export type NavKey = 'dashboard' | 'entry' | 'reports' | 'master' | 'users' | 'roles' | 'reset-password'

export interface MetricCard {
  label: string
  value: number | string
  unit?: string
  note?: string
}

export interface OverviewData {
  systemName: string
  latestMonth: string
  auditPolicy: string
  defaultAdmin: {
    username: string
    password: string
  }
  queryHighlights: string[]
  cards: MetricCard[]
}

export interface StatisticsRow {
  unitName: string
  reportCount: number
  runHours: number | string
  serviceHours: number | string
  openHours: number | string
  trainingHours: number | string
}

export interface CenterHighlight {
  centerName: string
  unitName: string
  runHours: number | string
  serviceHours: number | string
  openHours: number | string
}

export interface StatisticsData {
  month: string
  cards: MetricCard[]
  unitBreakdown: StatisticsRow[]
  centerHighlights: CenterHighlight[]
}

export interface TechnicalCenterItem {
  id: number
  code: string
  name: string
  unitId?: number
  unitName?: string
}

export interface StatisticsUnitItem {
  id: number
  code: string
  name: string
  centers: TechnicalCenterItem[]
}

export interface TemplateField {
  id: number
  key: string
  label: string
  groupName: string
  subGroupName?: string | null
  excelColumn: string
  valueType: 'DECIMAL' | 'TEXT' | string
  required?: boolean
  readOnly?: boolean
  formulaExpression?: string | null
  helperText?: string | null
  minValue?: number | string | null
  maxValue?: number | string | null
  sortOrder?: number
}

export interface TemplateSection {
  name: string
  fields: TemplateField[]
}

export interface TemplateGroup {
  name: string
  sections: TemplateSection[]
}

export interface TemplateData {
  id: number
  code: string
  name: string
  description: string
  groups: TemplateGroup[]
  fields: TemplateField[]
}

export interface ReportSummary {
  id: number
  reportMonth: string
  unitId: number
  unitName: string
  centerId: number
  centerName: string
  submitStatus: string
  auditStatus: string
  updatedAt: string | null
  metrics: {
    runHours: number | string
    serviceHours: number | string
    openHours: number | string
    trainingHours: number | string
  }
}

export interface ReportDetail {
  id: number
  reportMonth: string
  statisticsUnitId: number
  statisticsUnitName?: string
  technicalCenterId: number
  technicalCenterName?: string
  submitStatus: string
  auditStatus: string
  values: Record<string, string>
}

export interface AccessUser {
  id: number
  username: string
  displayName: string
  admin: boolean
  enabled?: boolean
  roleIds?: number[]
  roles: string[]
  centerIds?: number[]
  centers: string[]
}

export interface AccessRole {
  id: number
  code: string
  name: string
  enabled?: boolean
  permissions: string[]
  permissionCodes?: string[]
}

export interface PermissionCatalogItem {
  id?: number
  code: string
  name: string
  permissionType: string
  routePath: string
}

export interface AccessOverview {
  defaultAdmin: {
    username: string
    password: string
  }
  dataRuleNote: string
  users: AccessUser[]
  roles: AccessRole[]
  permissionCatalog: PermissionCatalogItem[]
}

export interface AuthCenterSummary {
  id: number
  name: string
  unitId: number
  unitName: string
}

export interface AuthUser {
  id: number
  username: string
  displayName: string
  admin: boolean
  roles: string[]
  roleCodes: string[]
  permissions: string[]
  centerIds: number[]
  centers: AuthCenterSummary[]
}

export interface AuthPayload {
  accessToken: string
  refreshToken: string
  accessTokenExpiresAt: string
  refreshTokenExpiresAt: string
  user: AuthUser
}

export interface AuthSession {
  accessToken: string
  refreshToken: string
  accessTokenExpiresAt: string
  refreshTokenExpiresAt: string
  user: AuthUser
}

export interface ReportFormState {
  reportMonth: string
  statisticsUnitId: number | null
  technicalCenterId: number | null
  submitStatus: string
  values: Record<string, string>
}

export interface NoticeState {
  type: 'success' | 'error' | 'info'
  text: string
}
