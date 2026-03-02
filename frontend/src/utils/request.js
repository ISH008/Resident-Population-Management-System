import axios from "axios";
import { ElMessage } from "element-plus";

const service = axios.create({
  baseURL: "/",
  timeout: 10000
});

service.interceptors.request.use((config) => {
  const token = localStorage.getItem("resident_mgmt_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

service.interceptors.response.use(
  (response) => {
    const payload = response.data;
    if (payload.code !== 0) {
      ElMessage.error(payload.message || "请求失败");
      return Promise.reject(payload);
    }
    return payload;
  },
  (error) => {
    if (error.response?.status === 401) {
      ElMessage.error("登录已过期，请重新登录");
    } else {
      ElMessage.error(error.response?.data?.message || error.message || "网络异常");
    }
    return Promise.reject(error);
  }
);

export default service;
