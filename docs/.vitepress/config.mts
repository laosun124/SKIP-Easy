import { withMermaid } from "vitepress-plugin-mermaid";
import { defineConfig } from "vitepress";

export default defineConfig(withMermaid({
  title: "SKIP 易用版",
  description: "SKIP 易用版 3.1.8：开屏广告跳过、白名单和微信小程序专属优化",
  base: "/",
  lastUpdated: true,
  head: [["link", { rel: "icon", type: "image/x-icon", href: "/images/favicon.ico" }]],
  themeConfig: {
    logo: "/images/favicon.ico",
    outline: { level: "deep" },
    nav: [
      { text: "首页", link: "/" },
      { text: "指南", link: "/guide/intro/what-is-skip" },
      { text: "下载", link: "https://github.com/laosun124/SKIP-Easy/releases/latest" },
    ],
    sidebar: {
      "/guide/": [
        { text: "开始使用", items: [
          { text: "什么是 SKIP 易用版", link: "/guide/intro/what-is-skip" },
          { text: "下载 APP", link: "/guide/intro/download-app" },
          { text: "开始使用", link: "/guide/intro/getting-started" },
        ] },
        { text: "主要功能", items: [
          { text: "应用保活", link: "/guide/keep-alive/intro" },
          { text: "应用白名单与微信优化", link: "/guide/white-list/intro" },
          { text: "设置", link: "/guide/settings/intro" },
          { text: "关于", link: "/guide/about/intro" },
        ] },
      ],
    },
    socialLinks: [{ icon: "github", link: "https://github.com/laosun124/SKIP-Easy" }],
    footer: { message: "基于 GuoXiCheng/SKIP 开发 · GPL-3.0" },
  },
}));
