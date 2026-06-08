import { wendaRequest } from './request'

export interface PageResp<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
  sort: string
  order: 'asc' | 'desc'
}

export interface School {
  id: string
  schoolCode: string
  name: string
  shortName?: string
  status: string
  contactEmail?: string
  contactPhone?: string
  address?: string
  description?: string
  tenantId: string
  version: number
}

export interface College {
  id: string
  schoolId: string
  collegeCode: string
  name: string
  shortName?: string
  description?: string
  status: string
  version: number
}

export const schoolApi = {
  current: () => wendaRequest<School>({ method: 'GET', url: '/schools/current' }),
  create: (body: { schoolCode: string; name: string; shortName?: string;
                   contactEmail?: string; contactPhone?: string;
                   address?: string; description?: string }) =>
    wendaRequest<School>({ method: 'POST', url: '/schools', data: body }),
  update: (schoolId: string, body: Partial<School>, ifMatch: number) =>
    wendaRequest<School>({
      method: 'PATCH', url: `/schools/${schoolId}`, data: body,
      headers: { 'If-Match': String(ifMatch) }
    })
}

export const collegeApi = {
  list: (page = 1, pageSize = 20) =>
    wendaRequest<PageResp<College>>({
      method: 'GET', url: '/colleges', params: { page, pageSize }
    }),
  create: (body: { collegeCode: string; name: string; shortName?: string; description?: string }) =>
    wendaRequest<College>({ method: 'POST', url: '/colleges', data: body }),
  update: (collegeId: string, body: Partial<College>, ifMatch: number) =>
    wendaRequest<College>({
      method: 'PATCH', url: `/colleges/${collegeId}`, data: body,
      headers: { 'If-Match': String(ifMatch) }
    })
}
