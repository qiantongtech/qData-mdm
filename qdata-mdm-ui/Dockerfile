# syntax=docker/dockerfile:1

# 前端已经在宿主机完成构建，本镜像只负责封装现成的 dist 文件。
# 使用官方 Nginx 基础镜像即可同时支持 amd64、arm64 等常见架构。
FROM crpi-kf13onfj0v8f6jax.cn-shanghai.personal.cr.aliyuncs.com/qiantongkeji/nginx:1.24.0

# Compose 中的 Nginx 配置将 /usr/share/nginx 作为静态文件根目录。
COPY dist/ /usr/share/nginx/
