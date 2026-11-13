#!/usr/bin/env bash

# qData MDM 前端镜像打包脚本
# 使用方式：在 qdata-mdm-ui 目录执行 ./build-image.sh

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
PLATFORMS=""
PUSH_IMAGE="true"
IMAGE_REPOSITORY="${QDATA_UI_REGISTRY:-}"
IMAGE_TAG="${QDATA_UI_TAG:-}"
IMAGE_NAME=""

print_help() {
  cat <<'HELP'
qData MDM 前端镜像打包工具

前置条件：
  1. 已在 qdata-mdm-ui 目录生成 dist/index.html
  2. Docker Desktop 或 Docker Engine 已启动
  3. 多架构推送需要先 docker login 到镜像仓库

前端脚本不会读取后端的 docker/.env。
镜像仓库、镜像名和目标架构从 package.json 的 docker 配置读取；
镜像版本从 package.json 的 version 读取。

用法：
  ./build-image.sh                         按 package.json 配置构建并推送镜像
  ./build-image.sh --no-push               只构建，不推送
  ./build-image.sh -r 仓库地址 -t 版本号     临时覆盖仓库和版本
  ./build-image.sh -p linux/amd64          临时指定单个架构
  ./build-image.sh -h                      查看帮助
HELP
}

fail() {
  echo "错误：$*" >&2
  exit 1
}

on_error() {
  local line="$1"
  echo "错误：脚本在第 ${line} 行执行失败。" >&2
  echo "建议：确认 Docker 已启动、已登录镜像仓库，并检查上方错误信息。" >&2
}
trap 'on_error "$LINENO"' ERR

while [[ $# -gt 0 ]]; do
  case "$1" in
    -r|--registry)
      [[ $# -ge 2 ]] || fail "${1} 后面必须提供镜像仓库地址。"
      IMAGE_REPOSITORY="$2"; shift 2 ;;
    -t|--tag)
      [[ $# -ge 2 ]] || fail "${1} 后面必须提供镜像版本。"
      IMAGE_TAG="$2"; shift 2 ;;
    -p|--platform|--platforms)
      [[ $# -ge 2 ]] || fail "${1} 后面必须提供平台列表。"
      PLATFORMS="$2"; shift 2 ;;
    --no-push)
      PUSH_IMAGE="false"; shift ;;
    -h|--help)
      print_help; exit 0 ;;
    *)
      fail "不认识的参数：$1。使用 -h 查看帮助。" ;;
  esac
done

PACKAGE_JSON="${SCRIPT_DIR}/package.json"
[[ -f "${PACKAGE_JSON}" ]] || fail "未找到 ${PACKAGE_JSON}。"
read_package_value() {
  local expression="$1"
  node --input-type=module -e "import fs from 'node:fs'; const p=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); const value=${expression}; if (value !== undefined && value !== null) process.stdout.write(String(value));" "${PACKAGE_JSON}"
}

PACKAGE_VERSION="$(read_package_value 'p.version')"
PACKAGE_REGISTRY="$(read_package_value 'p.docker?.registry')"
PACKAGE_IMAGE="$(read_package_value 'p.docker?.image')"
PACKAGE_PLATFORMS="$(read_package_value 'p.docker?.platforms')"
IMAGE_REPOSITORY="${IMAGE_REPOSITORY:-${PACKAGE_REGISTRY}}"
IMAGE_TAG="${IMAGE_TAG:-${PACKAGE_VERSION}}"
IMAGE_NAME="${IMAGE_NAME:-${PACKAGE_IMAGE:-qdata-mdm-ui}}"
PLATFORMS="${PLATFORMS:-${PACKAGE_PLATFORMS:-linux/amd64,linux/arm64}}"

[[ -n "${IMAGE_REPOSITORY}" ]] || fail "package.json 的 docker.registry 为空。请填写前端镜像仓库地址，或使用 -r 临时指定。"
[[ -n "${IMAGE_TAG}" ]] || fail "package.json 的 version 为空，请填写前端版本，或使用 -t 临时指定。"
[[ -f "${SCRIPT_DIR}/dist/index.html" ]] || fail "未找到 ${SCRIPT_DIR}/dist/index.html。请先在 qdata-mdm-ui 完成前端打包。"

command -v docker >/dev/null 2>&1 || fail "未找到 docker 命令。请安装 Docker Desktop/Engine 并重新执行。"
docker info >/dev/null 2>&1 || fail "Docker 服务不可用。请先启动 Docker Desktop 或 Docker Engine。"
docker buildx version >/dev/null 2>&1 || fail "当前 Docker 不支持 buildx。请升级 Docker 版本。"

IMAGE="${IMAGE_REPOSITORY}/${IMAGE_NAME}:${IMAGE_TAG}"
echo "即将构建前端镜像：${IMAGE}"
echo "目标架构：${PLATFORMS}"
echo "构建内容：${SCRIPT_DIR}/dist"

# 阿里云个人版镜像仓库不支持 BuildKit 默认生成的 OCI provenance 附加清单。
BUILD_ARGS=(buildx build --provenance=false --platform "${PLATFORMS}" --tag "${IMAGE}" --file "${SCRIPT_DIR}/Dockerfile")
if [[ "${PUSH_IMAGE}" == "true" ]]; then
  BUILD_ARGS+=(--push)
else
  echo "提示：已选择不推送，多架构镜像只会保留在 BuildKit 缓存中。"
  BUILD_ARGS+=(--progress=plain)
fi

docker "${BUILD_ARGS[@]}" "${SCRIPT_DIR}"
echo "前端镜像处理完成：${IMAGE}"
if [[ "${PUSH_IMAGE}" == "true" ]]; then
  echo "已推送多架构镜像，可在 Compose 中使用 ${IMAGE}。"
fi
