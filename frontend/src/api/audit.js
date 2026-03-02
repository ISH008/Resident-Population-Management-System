import request from "../utils/request";

export function auditLogsPageApi(params) {
  return request({
    url: "/api/v1/audit-logs",
    method: "get",
    params
  });
}
