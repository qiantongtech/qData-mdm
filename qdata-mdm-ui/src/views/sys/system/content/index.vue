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
  <div class="app-container dataBody">
    <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
      <template #actions-data>
        <el-button :loading="submitLoading" @click="confirm">{{
          td('common.button.save')
        }}</el-button>
      </template>
      <el-form :model="contentDetail" label-width="120px" class="column-form">
        <qt-form-item class="row-full" :label="td('sys.system.content.loginLogo')">
          <image-upload
            v-model="loginLogoModelValue"
            :limit="1"
            :fileSize="10"
            :isShowTip="true"
            @update:modelValue="loginLogoUpdate"
            :platForm="platForm"
          />
          <!-- Use components -->
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.system.content.systemLogo')">
          <image-upload
            v-model="logoModelValue"
            :limit="1"
            :fileSize="10"
            :isShowTip="true"
            @update:modelValue="logoUpdate"
            :platForm="platForm"
          />
          <!-- Use components -->
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.system.content.loginCarousel')">
          <image-upload
            v-model="carouselImageModelValue"
            :limit="3"
            :fileSize="10"
            :isShowTip="true"
            @update:modelValue="carouselImageUpdate"
            :platForm="platForm"
          />
          <!-- Use components -->
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.system.content.contactPhone')">
          <el-input
            v-model="contentDetail.contactNumber"
            :placeholder="td('sys.system.content.contactPhonePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.system.content.email')">
          <el-input
            v-model="contentDetail.email"
            :placeholder="td('sys.system.content.emailPlaceholder')"
          />
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.system.content.copyrightOwner')">
          <el-input
            v-model="contentDetail.copyright"
            :placeholder="td('sys.system.content.copyrightOwnerPlaceholder')"
          />
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.system.content.recordNo')">
          <el-input
            v-model="contentDetail.recordNumber"
            :placeholder="td('sys.system.content.recordNoPlaceholder')"
          />
        </qt-form-item>
      </el-form>
    </qt-wrap>
  </div>
</template>

<script setup>
import QtFormItem from '@/components/QtFormItem/index.vue';
import useDefaultLang from '@/composables/useDefaultLang';
import { ref } from 'vue';
import { getContent, listContent, updateContent } from '@/api/system/system/content';
import ImageUpload from '@/components/ImageUpload/index.vue';

const { td } = useDefaultLang();
const { proxy } = getCurrentInstance();
const submitLoading = ref(false);

const loginLogoModelValue = ref([]);
const logoModelValue = ref([]);
const carouselImageModelValue = ref([]);

//Storage platform name
const platForm = ref('aliyun-oss-qt');
//Store locally on the server
// const platForm = ref('')

const status = ref(true);
// Initialize contentDetail data
const contentDetail = ref({
  sysName: '',
  loginLogo: '',
  carouselImage: '',
  logo: '',
  contactNumber: '',
  email: '',
  copyright: '',
  recordNumber: ''
});

const loginLogoUpdate = (updatedFileList) => {
  contentDetail.value.loginLogo = updatedFileList;
};

const logoUpdate = (updatedFileList) => {
  contentDetail.value.logo = updatedFileList;
};

const carouselImageUpdate = (updatedFileList) => {
  contentDetail.value.carouselImage = updatedFileList;
};

// Use getContent to get data instead of redefining a getContent function
const fetchContent = async () => {
  try {
    contentDetail.value = {};
    // Call the getContent method you imported from the API
    const res = await getContent(1); // Assume that the request is for data with id 1
    if (res.code == 200) {
      const data = res.data;
      if (data.loginLogo) {
        const loginLogoList = data.loginLogo.splitd(',');
        const arr = [];
        loginLogoList.forEach((e) => {
          arr.push({ url: e });
        });
        loginLogoModelValue.value = arr;
      }
      if (data.logo) {
        const logoList = data.logo.splitd(',');
        const arr = [];
        logoList.forEach((e) => {
          arr.push({ url: e });
        });
        logoModelValue.value = arr;
      }
      if (data.carouselImage) {
        const carouselImageList = data.carouselImage.splitd(',');
        const arr = [];
        carouselImageList.forEach((e) => {
          arr.push({ url: e });
        });
        carouselImageModelValue.value = arr;
      }
      contentDetail.value = {
        id: data.id,
        sysName: data.sysName,
        loginLogo: data.loginLogo,
        logo: data.logo,
        carouselImage: data.carouselImage,
        contactNumber: data.contactNumber,
        email: data.email,
        copyright: data.copyright,
        recordNumber: data.recordNumber
      };
      // console.log('------contentDetail.value-------',contentDetail.value)
    }

    // this.$message.success('Content loaded successfully');
  } catch (error) {
    // Error handling
    // this.$message.error('Content loading failed');
    console.error(td('sys.system.content.dataLoadFailed'), error);
  }
};

// Automatically call fetchContent when the page loads
onMounted(() => {
  fetchContent();
});

// Update button click event
const update = () => {
  status.value = !status.value;
};

// Confirm button click event
const confirm = () => {
  if (submitLoading.value) return;
  submitLoading.value = true;
  proxy.$modal
    .confirm(td('sys.system.content.confirmSave'))
    .then(function () {})
    .then(async () => {
      status.value = !status.value;
      try {
        const item = contentDetail.value;
        const res = await updateContent(item);
        if (res.code == 200) {
          fetchContent();
          proxy.$modal.msgSuccess(td('sys.system.content.saveSuccess'));
        } else {
          // If the response code is not 200, the request failed
          proxy.$modal.msgError(td('sys.system.content.saveFailed'));
        }
      } catch (error) {
        // Capture network errors or failed requests
        console.error('Request failed:', error);
        proxy.$modal.msgError(td('sys.system.content.saveException') + error.message);
      } finally {
        submitLoading.value = false;
      }
    })
    .catch(() => {
      submitLoading.value = false;
    });
};
</script>

<style scoped>
.dataBody {
  min-height: calc(100vh - 115px);
  margin: 20px 30px;
}

.form-container {
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.form-item {
  margin-bottom: 20px;
  display: flex; /* Using Flexbox layout */
  align-items: center; /* vertical center alignment */
}

.form-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--qt-text-primary);
  width: 120px; /* label width */
  margin-right: 10px; /* spacing between label and input box */
}

.form-input {
  display: flex;
  align-items: center; /* Input box vertical center alignment */
  width: 100%; /* Make the input box fill the remaining width */
}
.form-input-i {
  display: flex;
  align-items: center; /* Input box vertical center alignment */
  width: 30%; /* Make the input box fill the remaining width */
}

.el-input {
  flex-grow: 1; /* Make the input box fill the remaining space */
  width: 30%; /* Make sure the input box occupies 100% of the width */
}

.upload-demo {
  display: inline-block;
  margin-top: 10px;
}

.uploaded-img img {
  margin-top: 10px;
  border-radius: 8px;
}

.el-button {
  background-color: var(--qt-primary);
  color: white;
}

.el-button:hover {
  background-color: #66b1ff;
}

@media (max-width: 768px) {
  .form-item {
    flex-direction: column; /* When the screen is smaller, labels and input boxes are arranged vertically */
    align-items: flex-start; /* left aligned */
  }

  .form-label {
    margin-right: 0;
    margin-bottom: 10px; /* Vertical spacing between label and input box */
  }
}
</style>

<style scoped lang="scss">
.column-form {
  display: flex;
  flex-direction: column;
}
.row-full {
  width: 100%;
  margin-right: 0;
}
</style>
