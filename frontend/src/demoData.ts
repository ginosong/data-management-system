import type {
  AccessOverview,
  OverviewData,
  ReportDetail,
  ReportFormState,
  ReportSummary,
  StatisticsData,
  StatisticsUnitItem,
  TemplateData,
} from './types'

export const fallbackOverview: OverviewData = {
  systemName: '重大科技设施运行数据管理系统',
  latestMonth: '2026-04',
  auditPolicy: '已预留审核流程，当前填报数据默认审核通过。',
  defaultAdmin: {
    username: 'admin',
    password: 'admin123',
  },
  queryHighlights: [
    '支持按月份、统计单位、技术中心、关键字组合查询',
    '支持固定模板填报、列表展示、统计汇总和 Excel 导出',
    '数据权限按技术中心控制，页面权限按角色控制',
  ],
  cards: [
    { label: '统计单位', value: 3, unit: '个', note: '按模板初始化' },
    { label: '技术中心', value: 25, unit: '个', note: '支持后台维护' },
    { label: '账号数', value: 2, unit: '个', note: '内置默认 admin' },
    { label: '已填报月报', value: 3, unit: '份', note: '含样例数据' },
    { label: '审核通过', value: 3, unit: '份', note: '当前默认审核通过' },
  ],
}

export const fallbackStatistics: StatisticsData = {
  month: '2026-04',
  cards: [
    { label: '本月填报中心', value: 2, unit: '个', note: '已填报技术中心' },
    { label: '填报覆盖率', value: '8.0', unit: '%', note: '按启用技术中心统计' },
    { label: '运行机时合计', value: 310, unit: '小时', note: '运行机时' },
    { label: '服务机时合计', value: 223, unit: '小时', note: '服务机时' },
    { label: '开放机时合计', value: 166, unit: '小时', note: '对外开放机时' },
    { label: '培训课时合计', value: 68, unit: '小时', note: '三类培训合计' },
  ],
  unitBreakdown: [
    {
      unitName: '转化医学研究院',
      reportCount: 2,
      runHours: 310,
      serviceHours: 223,
      openHours: 166,
      trainingHours: 68,
    },
  ],
  centerHighlights: [
    {
      centerName: '生物医学制造与检测（装备制造与认证）技术中心',
      unitName: '转化医学研究院',
      runHours: 168,
      serviceHours: 120,
      openHours: 92,
    },
    {
      centerName: '生物信息（数字医学）技术中心',
      unitName: '转化医学研究院',
      runHours: 142,
      serviceHours: 103,
      openHours: 74,
    },
  ],
}

export const fallbackUnits: StatisticsUnitItem[] = [
  {
    id: 1,
    code: 'UNIT_TMRI',
    name: '转化医学研究院',
    centers: [
      { id: 1, code: 'CENTER_001', name: '生物医学制造与检测（装备制造与认证）技术中心', unitId: 1, unitName: '转化医学研究院' },
      { id: 2, code: 'CENTER_002', name: '生物信息（数字医学）技术中心', unitId: 1, unitName: '转化医学研究院' },
      { id: 3, code: 'CENTER_003', name: '生物信息（临床统计与数据服务）技术中心', unitId: 1, unitName: '转化医学研究院' },
    ],
  },
  {
    id: 2,
    code: 'UNIT_ATC_TMRI',
    name: '分析测试中心/转化医学研究院',
    centers: [
      { id: 17, code: 'CENTER_017', name: '新药创制（安评与工艺）技术中心', unitId: 2, unitName: '分析测试中心/转化医学研究院' },
      { id: 18, code: 'CENTER_018', name: '生物医学影像（分子、细胞、组织、小动物活体多模态成像）技术中心', unitId: 2, unitName: '分析测试中心/转化医学研究院' },
    ],
  },
  {
    id: 3,
    code: 'UNIT_RUIJIN',
    name: '瑞金',
    centers: [
      { id: 21, code: 'CENTER_021', name: '质谱平台', unitId: 3, unitName: '瑞金' },
      { id: 22, code: 'CENTER_022', name: '测序平台', unitId: 3, unitName: '瑞金' },
    ],
  },
]

export const fallbackTemplate: TemplateData = {
  id: 1,
  code: 'DMS_MONTHLY_TEMPLATE',
  name: '数据统计月报模板',
  description: '依据 Excel 模板整理的固定字段结构。',
  groups: [
    {
      name: '基本运行情况',
      sections: [
        {
          name: '基本运行情况',
          fields: [
            { id: 1, key: 'run_hours', label: '运行机时', groupName: '基本运行情况', excelColumn: 'C', valueType: 'DECIMAL' },
            { id: 2, key: 'service_hours', label: '服务机时', groupName: '基本运行情况', excelColumn: 'D', valueType: 'DECIMAL', helperText: '不得大于运行机时', required: true },
            { id: 3, key: 'user_count', label: '用户数量', groupName: '基本运行情况', excelColumn: 'E', valueType: 'DECIMAL' },
            { id: 4, key: 'enterprise_user_count', label: '企业用户数量', groupName: '基本运行情况', excelColumn: 'F', valueType: 'DECIMAL' },
          ],
        },
      ],
    },
    {
      name: '对外开放情况',
      sections: [
        {
          name: '对外开放机时',
          fields: [
            { id: 6, key: 'open_hours_total', label: '总数', groupName: '对外开放情况', subGroupName: '对外开放机时', excelColumn: 'H', valueType: 'DECIMAL', readOnly: true, formulaExpression: 'open_hours_international + open_hours_domestic', helperText: '系统自动汇总国际与国内开放机时' },
            { id: 7, key: 'open_hours_international', label: '国际用户机时数', groupName: '对外开放情况', subGroupName: '对外开放机时', excelColumn: 'I', valueType: 'DECIMAL', required: true },
            { id: 8, key: 'open_hours_domestic', label: '国内用户机时数', groupName: '对外开放情况', subGroupName: '对外开放机时', excelColumn: 'J', valueType: 'DECIMAL', required: true },
          ],
        },
        {
          name: '外部用户数量',
          fields: [
            { id: 11, key: 'external_user_total', label: '总数', groupName: '对外开放情况', subGroupName: '外部用户数量', excelColumn: 'M', valueType: 'DECIMAL', readOnly: true, formulaExpression: 'external_user_international + external_user_domestic', helperText: '系统自动汇总国际与国内用户数量' },
            { id: 12, key: 'external_user_international', label: '国际用户数量', groupName: '对外开放情况', subGroupName: '外部用户数量', excelColumn: 'N', valueType: 'DECIMAL', required: true },
            { id: 13, key: 'external_user_domestic', label: '国内用户数量', groupName: '对外开放情况', subGroupName: '外部用户数量', excelColumn: 'O', valueType: 'DECIMAL', required: true },
          ],
        },
      ],
    },
    {
      name: '服务企业成效',
      sections: [
        {
          name: '服务企业数量/金额',
          fields: [
            { id: 16, key: 'active_enterprise_service_count_amount', label: '前期已服务且本月仍在服务的企业数/金额', groupName: '服务企业成效', subGroupName: '服务企业数量/金额', excelColumn: 'R', valueType: 'TEXT' },
            { id: 17, key: 'new_enterprise_service_count_fee', label: '本月新增服务企业数/经费', groupName: '服务企业成效', subGroupName: '服务企业数量/金额', excelColumn: 'S', valueType: 'TEXT' },
          ],
        },
      ],
    },
    {
      name: '服务科研',
      sections: [
        {
          name: '承担课题（项目）数量/经费',
          fields: [
            { id: 22, key: 'active_project_count_funding', label: '前期课题（项目）本月仍在承担数/经费', groupName: '服务科研', subGroupName: '承担课题（项目）数量/经费', excelColumn: 'X', valueType: 'TEXT' },
          ],
        },
        {
          name: '服务科研',
          fields: [
            { id: 30, key: 'patent_count', label: '专利数量', groupName: '服务科研', excelColumn: 'AF', valueType: 'DECIMAL' },
            { id: 33, key: 'paper_count', label: '发表论文数量', groupName: '服务科研', excelColumn: 'AI', valueType: 'DECIMAL' },
          ],
        },
      ],
    },
    {
      name: '服务科普工作',
      sections: [
        {
          name: '服务科普工作',
          fields: [
            { id: 38, key: 'science_activity_count', label: '组织科普活动次数', groupName: '服务科普工作', excelColumn: 'AN', valueType: 'DECIMAL' },
            { id: 39, key: 'science_participant_count', label: '参加人（次）数', groupName: '服务科普工作', excelColumn: 'AO', valueType: 'DECIMAL' },
          ],
        },
      ],
    },
    {
      name: '技术培训课时数',
      sections: [
        {
          name: '技术培训课时数',
          fields: [
            { id: 40, key: 'training_hours', label: '课时数', groupName: '技术培训课时数', excelColumn: 'AP', valueType: 'DECIMAL' },
          ],
        },
      ],
    },
    {
      name: '开展企业技术培训课时数',
      sections: [
        {
          name: '开展企业技术培训课时数',
          fields: [
            { id: 41, key: 'enterprise_training_hours', label: '课时数', groupName: '开展企业技术培训课时数', excelColumn: 'AQ', valueType: 'DECIMAL' },
          ],
        },
      ],
    },
    {
      name: '安全培训课时数',
      sections: [
        {
          name: '安全培训课时数',
          fields: [
            { id: 42, key: 'safety_training_hours', label: '课时数', groupName: '安全培训课时数', excelColumn: 'AR', valueType: 'DECIMAL' },
          ],
        },
      ],
    },
  ],
  fields: [],
}

fallbackTemplate.fields = fallbackTemplate.groups.flatMap((group) => group.sections.flatMap((section) => section.fields))

export const fallbackReports: ReportSummary[] = [
  {
    id: 1,
    reportMonth: '2026-04',
    unitId: 1,
    unitName: '转化医学研究院',
    centerId: 1,
    centerName: '生物医学制造与检测（装备制造与认证）技术中心',
    submitStatus: 'SUBMITTED',
    auditStatus: 'APPROVED',
    updatedAt: '2026-04-30T10:00:00',
    metrics: {
      runHours: 168,
      serviceHours: 120,
      openHours: 92,
      trainingHours: 42,
      enterpriseUserHours: 58,
      shanghaiUserHours: 36,
    },
  },
  {
    id: 2,
    reportMonth: '2026-04',
    unitId: 1,
    unitName: '转化医学研究院',
    centerId: 2,
    centerName: '生物信息（数字医学）技术中心',
    submitStatus: 'SUBMITTED',
    auditStatus: 'APPROVED',
    updatedAt: '2026-04-30T09:30:00',
    metrics: {
      runHours: 142,
      serviceHours: 103,
      openHours: 74,
      trainingHours: 26,
      enterpriseUserHours: 45,
      shanghaiUserHours: 31,
    },
  },
]

export const fallbackReportDetail: ReportDetail = {
  id: 1,
  reportMonth: '2026-04',
  statisticsUnitId: 1,
  technicalCenterId: 1,
  submitStatus: 'SUBMITTED',
  auditStatus: 'APPROVED',
  values: {
    run_hours: '168',
    service_hours: '120',
    user_count: '54',
    enterprise_user_count: '19',
    open_hours_total: '92',
    open_hours_domestic: '88',
    external_user_total: '31',
    active_enterprise_service_count_amount: '6 / 45.8',
    new_enterprise_service_count_fee: '2 / 16.4',
    active_project_count_funding: '4 / 128.0',
    patent_count: '3',
    paper_count: '6',
    science_activity_count: '2',
    science_participant_count: '160',
    training_hours: '24',
    enterprise_training_hours: '10',
    safety_training_hours: '8',
  },
}

export const fallbackAccessOverview: AccessOverview = {
  defaultAdmin: {
    username: 'admin',
    password: 'admin123',
  },
  dataRuleNote: '张三当前被授权维护转化医学研究院下的两个技术中心。',
  users: [
    {
      id: 1,
      username: 'admin',
      displayName: '默认管理员',
      admin: true,
      enabled: true,
      roleIds: [1],
      roles: ['系统管理员'],
      centerIds: [],
      centers: [],
    },
    {
      id: 2,
      username: 'zhangsan',
      displayName: '张三',
      admin: false,
      enabled: true,
      roleIds: [2],
      roles: ['报表填报员'],
      centerIds: [1, 2],
      centers: ['生物医学制造与检测（装备制造与认证）技术中心', '生物信息（数字医学）技术中心'],
    },
  ],
  roles: [
    {
      id: 1,
      code: 'ADMIN',
      name: '系统管理员',
      enabled: true,
      permissionCodes: ['dashboard:view', 'reports:view', 'reports:edit', 'reports:export', 'master-data:view', 'master-data:edit', 'system:user:view', 'system:user:edit'],
      permissions: ['查看工作台', '查看月报列表', '录入和修改月报', '导出月报', '查看技术中心主数据', '维护技术中心主数据', '查看账号角色', '维护账号角色'],
    },
    {
      id: 2,
      code: 'REPORT_EDITOR',
      name: '报表填报员',
      enabled: true,
      permissionCodes: ['dashboard:view', 'reports:view', 'reports:edit', 'reports:export', 'master-data:view'],
      permissions: ['查看工作台', '查看月报列表', '录入和修改月报', '导出月报', '查看技术中心主数据'],
    },
  ],
  permissionCatalog: [
    { id: 1, code: 'dashboard:view', name: '查看工作台', permissionType: 'PAGE', routePath: '/dashboard' },
    { id: 2, code: 'reports:view', name: '查看月报列表', permissionType: 'PAGE', routePath: '/reports' },
    { id: 3, code: 'reports:edit', name: '录入和修改月报', permissionType: 'ACTION', routePath: '/reports/edit' },
    { id: 4, code: 'reports:export', name: '导出月报', permissionType: 'ACTION', routePath: '/reports/export' },
  ],
}

export const emptyFormState: ReportFormState = {
  reportMonth: '2026-04',
  statisticsUnitId: 1,
  technicalCenterId: 1,
  submitStatus: 'SUBMITTED',
  values: { ...fallbackReportDetail.values },
}
