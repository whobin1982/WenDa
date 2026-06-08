import { wendaRequest } from './request'

export const settingsApi = {
  getQuality: () => wendaRequest<unknown>({ method: 'GET', url: '/school-settings/quality-rules' }),
  updateQuality: (body: Record<string, unknown>, ifMatch: number) =>
    wendaRequest<unknown>({
      method: 'PATCH', url: '/school-settings/quality-rules', data: body,
      headers: { 'If-Match': String(ifMatch) }
    }),
  getCourseCode: () => wendaRequest<unknown>({ method: 'GET', url: '/school-settings/course-code-policy' }),
  updateCourseCode: (body: Record<string, unknown>, ifMatch: number) =>
    wendaRequest<unknown>({
      method: 'PATCH', url: '/school-settings/course-code-policy', data: body,
      headers: { 'If-Match': String(ifMatch) }
    }),
  getAI: () => wendaRequest<unknown>({ method: 'GET', url: '/school-settings/ai' }),
  updateAI: (body: Record<string, unknown>, ifMatch: number) =>
    wendaRequest<unknown>({
      method: 'PATCH', url: '/school-settings/ai', data: body,
      headers: { 'If-Match': String(ifMatch) }
    }),
  getAbilityLevels: () => wendaRequest<unknown>({ method: 'GET', url: '/school-settings/ability-levels' }),
  updateAbilityLevels: (body: { levelsJson: string }, ifMatch: number) =>
    wendaRequest<unknown>({
      method: 'PUT', url: '/school-settings/ability-levels', data: body,
      headers: { 'If-Match': String(ifMatch) }
    }),
  getWarningRules: () => wendaRequest<unknown>({ method: 'GET', url: '/school-settings/growth-warning-rules' }),
  updateWarningRules: (body: Record<string, unknown>, ifMatch: number) =>
    wendaRequest<unknown>({
      method: 'PATCH', url: '/school-settings/growth-warning-rules', data: body,
      headers: { 'If-Match': String(ifMatch) }
    }),
  getAIPolicy: () => wendaRequest<Record<string, unknown>>({ method: 'GET', url: '/school/ai-policy' }),
  updateAIPolicy: (body: Record<string, unknown>) =>
    wendaRequest<Record<string, unknown>>({ method: 'PUT', url: '/school/ai-policy', data: body })
}

export const auditApi = {
  search: (params: Record<string, unknown> = {}) =>
    wendaRequest<{
      items: Array<Record<string, unknown>>
      page: number
      pageSize: number
      total: number
      totalPages: number
    }>({ method: 'GET', url: '/audit-logs', params })
}

export const dashboardApi = {
  school: () => wendaRequest<{ scope: string; scopeId: string;
                                metrics: Record<string, number>;
                                todoSummary: Record<string, number> }>(
      { method: 'GET', url: '/dashboards/school' }),
  college: (collegeId: string) => wendaRequest<{ scope: string; scopeId: string;
                                                  metrics: Record<string, number>;
                                                  todoSummary: Record<string, number> }>(
      { method: 'GET', url: `/dashboards/colleges/${collegeId}` }),
  major: (majorId: string) => wendaRequest<{ scope: string; scopeId: string;
                                              metrics: Record<string, number>;
                                              todoSummary: Record<string, number>;
                                              note?: string }>(
      { method: 'GET', url: `/dashboards/majors/${majorId}` })
}
