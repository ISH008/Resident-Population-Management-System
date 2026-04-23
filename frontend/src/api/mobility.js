import request from "../utils/request";

export function listResidentMobilityLogsApi(residentId) {
  return request({
    url: `/api/v1/residents/${residentId}/mobility-logs`,
    method: "get"
  });
}

export function createResidentMobilityLogApi(residentId, data) {
  return request({
    url: `/api/v1/residents/${residentId}/mobility-logs`,
    method: "post",
    data
  });
}

export function deleteResidentMobilityLogApi(residentId, logId) {
  return request({
    url: `/api/v1/residents/${residentId}/mobility-logs/${logId}`,
    method: "delete"
  });
}

export function myMobilityLogsApi() {
  return request({
    url: "/api/v1/mobility-logs/me",
    method: "get"
  });
}

