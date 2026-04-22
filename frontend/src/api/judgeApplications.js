import request from "../utils/request";
import axios from "axios";

export function createJudgeApplicationApi(data) {
  return request({
    url: "/api/v1/judge-applications",
    method: "post",
    data
  });
}

export function judgeApplicationsMineApi(params) {
  return request({
    url: "/api/v1/judge-applications/mine",
    method: "get",
    params
  });
}

export function judgeApplicationsPageApi(params) {
  return request({
    url: "/api/v1/judge-applications",
    method: "get",
    params
  });
}

export function approveJudgeApplicationApi(id, data) {
  return request({
    url: `/api/v1/judge-applications/${id}/approve`,
    method: "put",
    data
  });
}

export function rejectJudgeApplicationApi(id, data) {
  return request({
    url: `/api/v1/judge-applications/${id}/reject`,
    method: "put",
    data
  });
}

export function uploadJudgeApplicationAttachmentApi(id, file) {
  const formData = new FormData();
  formData.append("file", file);
  return request({
    url: `/api/v1/judge-applications/${id}/attachments`,
    method: "post",
    data: formData,
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
}

export function listJudgeApplicationAttachmentsApi(id) {
  return request({
    url: `/api/v1/judge-applications/${id}/attachments`,
    method: "get"
  });
}

export async function downloadJudgeApplicationAttachmentApi(attachmentId) {
  const token = localStorage.getItem("resident_mgmt_token");
  return axios({
    url: `/api/v1/judge-applications/attachments/${attachmentId}/download`,
    method: "get",
    responseType: "blob",
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  });
}
