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

<!-- 复杂详情路由模板
    {
        path: '/tech/qiantong/qdata/mdm/quality/qualityTaskInstance',
        component: Layout,
        redirect: 'qualityTaskInstance',
        hidden: true,
        children: [
            {
                path: 'qualitytaskinstanceDetail',
                component: () => import('@/views/tech/qiantong/qdata/mdm/quality/qualityTaskInstance/detail/index.vue'),
                name: 'tree',
                meta: { title: '主数据质量任务执行实例详情', activeMenu: '/quality/qualitytaskinstance'  }
            }
        ]
    }
 -->



<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch" style="padding-bottom:15px">
      <div class="infotop" >
        <div class="infotop-title mb15">
              {{ qualitytaskinstanceDetail.id }}
        </div>
        <el-row :gutter="20">
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">主键ID</div>
                <div class="infotop-row-value">{{ qualitytaskinstanceDetail.id }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">质量任务ID</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.qualityTaskId || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">质量任务编码</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.taskCode || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">主数据模型ID</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.modelId || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">主数据模型版本</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.modelVersion || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">实例编号</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.executionNo || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">负责人ID</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.personCharge || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">负责人电话</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.contactNumber || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">触发方式：1人工执行、2定时执行</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.triggerType || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">执行结果：1执行中、2失败、3部分成功、4成功</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.status || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">本次执行开始时间</div>
                <div class="infotop-row-value">{{ parseTime(qualitytaskinstanceDetail.startTime, '{y}-{m}-{d}') }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">本次执行结束时间</div>
                <div class="infotop-row-value">{{ parseTime(qualitytaskinstanceDetail.endTime, '{y}-{m}-{d}') }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">检查数据记录数</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.totalCount || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">通过数据记录数</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.passCount || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">问题数据记录数</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.failCount || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">执行失败说明</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.errorMessage || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">是否有效；0：无效，1：有效</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.validFlag || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">创建人</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.createBy || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">创建时间</div>
                <div class="infotop-row-value">{{ parseTime(qualitytaskinstanceDetail.createTime, '{y}-{m}-{d}') }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">更新人</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.updateBy || '-' }}
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">更新时间</div>
                <div class="infotop-row-value">{{ parseTime(qualitytaskinstanceDetail.updateTime, '{y}-{m}-{d}') }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="infotop-row border-top">
                <div class="infotop-row-lable">备注</div>
                <div class="infotop-row-value">
                  {{ qualitytaskinstanceDetail.remark || '-' }}
                </div>
              </div>
            </el-col>
        </el-row>

      </div>
    </div>

    <div  class="pagecont-bottom">
      <el-tabs v-model="activeName" class="demo-tabs" @tab-click="handleClick">
        <el-tab-pane label="组件一" name="1">
          <component-one ></component-one>
        </el-tab-pane>
        <el-tab-pane label="组件二" name="2">
          <component-two ></component-two>
        </el-tab-pane>
      </el-tabs>
    </div>


  </div>
</template>

<script setup name="Qualitytaskinstance">
  import {getQualitytaskinstance } from "@/api/quality/qualityTaskInstance/qualitytaskinstance";
  import { useRoute } from 'vue-router';
  const { proxy } = getCurrentInstance();

  const activeName = ref('1')

  const handleClick = (tab, event) => {
    console.log(tab, event)
  }

  const showSearch = ref(true);
  const route = useRoute();
  let id = route.query.id || 1;
  // 监听 id 变化
  watch(
          () => route.query.id,
          (newId) => {
            id = newId || 1;  // 如果 id 为空，使用默认值 1
            getQualitytaskinstanceDetailById();

          },
          { immediate: true }  // `immediate` 为 true 表示页面加载时也会立即执行一次 watch
  );
  const data = reactive({
      qualitytaskinstanceDetail: {
    },
    form: {},
  });

  const {  qualitytaskinstanceDetail, rules } = toRefs(data);

  /** 复杂详情页面上方表单查询 */
  function getQualitytaskinstanceDetailById() {
        const _id = id ;
    getQualitytaskinstance(_id).then(response => {
        qualitytaskinstanceDetail.value = response.data;
    });
  }

  getQualitytaskinstanceDetailById();

</script>
