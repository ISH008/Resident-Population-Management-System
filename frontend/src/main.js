import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import "./styles.css";

const locale = {
  ...zhCn,
  el: {
    ...zhCn.el,
    pagination: {
      ...zhCn.el.pagination,
      total: "总记录数 {total}",
      pagesize: "/页",
      pageClassifier: "页"
    }
  }
};

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.use(ElementPlus, { locale });
app.mount("#app");
