import request from "../utils/request";

export function residentsPageApi(params) {
  return request({
    url: "/api/v1/residents",
    method: "get",
    params
  });
}

export function residentDetailApi(id) {
  return request({
    url: `/api/v1/residents/${id}`,
    method: "get"
  });
}

export function createResidentApi(data) {
  return request({
    url: "/api/v1/residents",
    method: "post",
    data
  });
}

export function updateResidentApi(id, data) {
  return request({
    url: `/api/v1/residents/${id}`,
    method: "put",
    data
  });
}

export function deleteResidentApi(id) {
  return request({
    url: `/api/v1/residents/${id}`,
    method: "delete"
  });
}

export function judgeResidentApi(id, data) {
  return request({
    url: `/api/v1/residents/${id}/judge`,
    method: "post",
    data
  });
}

export function residentJudgeLogsApi(id) {
  return request({
    url: `/api/v1/residents/${id}/judge-log`,
    method: "get"
  });
}
