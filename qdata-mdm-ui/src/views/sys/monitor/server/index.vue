<!--
  Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.

  This file is part of qData-mdm Master Data Platform (Open Source Edition).

  qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
  You may use qData-mdm for commercial purposes, but you may not remove, hide,
  modify, or replace the qData-mdm logo, copyright notices, license notices,
  or attribution information without a separate commercial license.

  White-label use, OEM distribution, rebranding, or presenting qData-mdm as
  another product requires separate commercial authorization from
  Jiangsu Qiantong Technology Co., Ltd.

  Business License: https://community.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div class="app-container">
    <el-row :gutter="15">
      <el-col :span="12" class="card-box">
        <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
          <section class="sys-panel">
            <div class="sys-panel-heading">
              <cpu style="width: 1em; height: 1em; vertical-align: middle" />
              <span style="vertical-align: middle">CPU</span>
            </div>
            <div class="el-table el-table--enable-row-hover el-table--medium">
              <table cellspacing="0" style="width: 100%">
                <thead>
                  <tr>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.property') }}</div>
                    </th>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.value') }}</div>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.cores') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.cpu">{{ server.cpu.cpuNum }}</div>
                    </td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.userUsage') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.cpu">{{ server.cpu.used }}%</div>
                    </td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.systemUsage') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.cpu">{{ server.cpu.sys }}%</div>
                    </td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.idleRate') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.cpu">{{ server.cpu.free }}%</div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </qt-wrap>
      </el-col>
      <el-col :span="12" class="card-box">
        <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
          <section class="sys-panel">
            <div class="sys-panel-heading">
              <tickets style="width: 1em; height: 1em; vertical-align: middle" />
              <span style="vertical-align: middle">{{ td('sys.monitor.server.memory') }}</span>
            </div>
            <div class="el-table el-table--enable-row-hover el-table--medium">
              <table cellspacing="0" style="width: 100%">
                <thead>
                  <tr>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.property') }}</div>
                    </th>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.memory') }}</div>
                    </th>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">JVM</div>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.totalMemory') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.mem">{{ server.mem.total }}G</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.jvm">{{ server.jvm.total }}M</div>
                    </td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.usedMemory') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.mem">{{ server.mem.used }}G</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.jvm">{{ server.jvm.used }}M</div>
                    </td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.freeMemory') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.mem">{{ server.mem.free }}G</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.jvm">{{ server.jvm.free }}M</div>
                    </td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.usageRate') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div
                        class="cell"
                        v-if="server.mem"
                        :class="{ 'text-danger': server.mem.usage > 80 }"
                      >
                        {{ server.mem.usage }}%
                      </div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div
                        class="cell"
                        v-if="server.jvm"
                        :class="{ 'text-danger': server.jvm.usage > 80 }"
                      >
                        {{ server.jvm.usage }}%
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </qt-wrap>
      </el-col>
      <el-col :span="24" class="card-box">
        <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
          <section class="sys-panel">
            <div class="sys-panel-heading">
              <monitor style="width: 1em; height: 1em; vertical-align: middle" />
              <span style="vertical-align: middle">{{ td('sys.monitor.server.serverInfo') }}</span>
            </div>
            <div class="el-table el-table--enable-row-hover el-table--medium">
              <table cellspacing="0" style="width: 100%">
                <tbody>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.serverName') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.sys">{{ server.sys.computerName }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.os') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.sys">{{ server.sys.osName }}</div>
                    </td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.serverIp') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.sys">{{ server.sys.computerIp }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.systemArch') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.sys">{{ server.sys.osArch }}</div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </qt-wrap>
      </el-col>
      <el-col :span="24" class="card-box">
        <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
          <section class="sys-panel">
            <div class="sys-panel-heading">
              <coffee-cup style="width: 1em; height: 1em; vertical-align: middle" />
              <span style="vertical-align: middle">{{ td('sys.monitor.server.jvmInfo') }}</span>
            </div>
            <div class="el-table el-table--enable-row-hover el-table--medium">
              <table cellspacing="0" style="width: 100%; table-layout: fixed">
                <tbody>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.javaName') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.jvm">{{ server.jvm.name }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.javaVersion') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.jvm">{{ server.jvm.version }}</div>
                    </td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.startTime') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.jvm">{{ server.jvm.startTime }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.runTime') }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.jvm">{{ server.jvm.runTime }}</div>
                    </td>
                  </tr>
                  <tr>
                    <td colspan="1" class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.installPath') }}</div>
                    </td>
                    <td colspan="3" class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.jvm">{{ server.jvm.home }}</div>
                    </td>
                  </tr>
                  <tr>
                    <td colspan="1" class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.projectPath') }}</div>
                    </td>
                    <td colspan="3" class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.sys">{{ server.sys.userDir }}</div>
                    </td>
                  </tr>
                  <tr>
                    <td colspan="1" class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.runParam') }}</div>
                    </td>
                    <td colspan="3" class="el-table__cell is-leaf">
                      <div class="cell" v-if="server.jvm">{{ server.jvm.inputArgs }}</div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </qt-wrap>
      </el-col>
      <el-col :span="24" class="card-box bottom">
        <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
          <section class="sys-panel">
            <div class="sys-panel-heading">
              <message-box style="width: 1em; height: 1em; vertical-align: middle" />
              <span style="vertical-align: middle">{{ td('sys.monitor.server.diskStatus') }}</span>
            </div>
            <div class="el-table el-table--enable-row-hover el-table--medium">
              <table cellspacing="0" style="width: 100%">
                <thead>
                  <tr>
                    <th class="el-table__cell el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.diskPath') }}</div>
                    </th>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.fileSystem') }}</div>
                    </th>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.diskType') }}</div>
                    </th>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.totalSize') }}</div>
                    </th>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.availableSize') }}</div>
                    </th>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.usedSize') }}</div>
                    </th>
                    <th class="el-table__cell is-leaf">
                      <div class="cell">{{ td('sys.monitor.server.usedPercent') }}</div>
                    </th>
                  </tr>
                </thead>
                <tbody v-if="server.sysFiles">
                  <tr v-for="(sysFile, index) in server.sysFiles" :key="index">
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ sysFile.dirName }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ sysFile.sysTypeName }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ sysFile.typeName }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ sysFile.total }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ sysFile.free }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell">{{ sysFile.used }}</div>
                    </td>
                    <td class="el-table__cell is-leaf">
                      <div class="cell" :class="{ 'text-danger': sysFile.usage > 80 }">
                        {{ sysFile.usage }}%
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </qt-wrap>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { getServer } from '@/api/system/monitor/server.js';
import useDefaultLang from '@/composables/useDefaultLang';

const { td } = useDefaultLang();
const server = ref([]);
const { proxy } = getCurrentInstance();

function getList() {
  proxy.$modal.loading(td('sys.monitor.server.loadingMsg'));
  getServer().then((response) => {
    server.value = response.data;
    proxy.$modal.closeLoading();
  });
}

getList();
</script>
<style lang="scss" scoped>
.bottom {
  margin-bottom: 0;
}
</style>

<style scoped lang="scss">
.sys-dialog-host {
  margin: 0;
  padding: 0;
  min-height: 0;
}
.column-form {
  display: flex;
  flex-direction: column;
}
.row-full {
  width: 100%;
  margin-right: 0;
}
.sys-panel {
  padding: 15px;
  background: var(--qt-surface-background);
  border-radius: var(--qt-radius-small);
}
.sys-panel-heading {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 15px;
}
</style>
