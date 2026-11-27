#!/bin/bash
# Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
#
# This file is part of qData-mdm Master Data Platform (Open Source Edition).
#
# qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
# You may use qData-mdm for commercial purposes, but you may not remove, hide,
# modify, or replace the qData-mdm logo, copyright notices, license notices,
# or attribution information without a separate commercial license.
#
# White-label use, OEM distribution, rebranding, or presenting qData-mdm as
# another product requires separate commercial authorization from
# Jiangsu Qiantong Technology Co., Ltd.
#
# Business License: https://docs.qdata.tech/business/policy.html
# See the LICENSE file in the project root for full license information.

# Create builder (key step)
docker buildx create \
  --name qdata-mdm-builder \
  --driver docker-container \
  --use

# Start builder (load QEMU, etc.)
docker buildx inspect --bootstrap

# Build AMD64 (x86_64) version
cd /mnt/c/Users/Ming/Desktop/qData/qdata-mdm-server-ce # Change the path to your own path

docker buildx build \
  --platform linux/amd64 \
  --no-cache \
  -t crpi-kf13onfj0v8f6jax.cn-shanghai.personal.cr.aliyuncs.com/qiantongkeji/qdata-mdm-server-ce:1.6.5 \
  --file=docker/Dockerfile \
  --load \
  /mnt/c/Users/Ming/Desktop/qData/qdata-mdm-server-ce # Change the context path to your own path

# Build ARM64 (adapted to Kunpeng, Feiteng, Raspberry Pi and other ARM servers)
docker buildx build \
  --platform linux/arm64 \
  --no-cache \
  -t crpi-kf13onfj0v8f6jax.cn-shanghai.personal.cr.aliyuncs.com/qiantongkeji/qdata-mdm-server-ce:1.6.5 \
  --file=docker/Dockerfile \
  --load \
  /mnt/c/Users/Ming/Desktop/qData/qdata-mdm-server-ce # Change the context path to your own path

# Check if ARM64 is supported
docker inspect crpi-kf13onfj0v8f6jax.cn-shanghai.personal.cr.aliyuncs.com/qiantongkeji/qdata-mdm-server-ce:1.6.5 --format '{{.Architecture}}'

# Delete the previously built builder (optional but recommended, keep it clean)
docker buildx rm qdata-mdm-builder

# Start new container
docker run -d \
  --name qdata-mdm-server-ce \
  -p 8080:8080 \
  crpi-kf13onfj0v8f6jax.cn-shanghai.personal.cr.aliyuncs.com/qiantongkeji/qdata-mdm-server-ce:1.6.5
