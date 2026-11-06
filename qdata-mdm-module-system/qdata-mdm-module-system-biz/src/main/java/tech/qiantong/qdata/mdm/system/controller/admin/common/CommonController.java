/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.system.controller.admin.common;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.qiantong.qdata.mdm.common.config.AniviaConfig;
import tech.qiantong.qdata.mdm.common.constant.Constants;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.file.FileUploadUtils;
import tech.qiantong.qdata.mdm.common.utils.file.FileUtils;
import tech.qiantong.qdata.mdm.config.ServerConfig;
import tech.qiantong.qdata.mdm.system.domain.vo.GetResponseColumnReqVO;

/**
 * 通用请求处理
 *
 * @author qdata
 */
@RestController
@RequestMapping("/common")
public class CommonController {
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    @Autowired
    private ServerConfig serverConfig;

//    @Value("${ds.resource_url}")
//    private String resourceUrl;
//
//    @Value("${ds.hdfs.url}")
//    private String hdfsUrl;

    private static final String FILE_DELIMETER = ",";

    /**
     * 通用下载请求
     *
     * @param fileName 文件名称
     * @param delete   是否删除
     */
    @GetMapping("/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request) {
        try {
            if (!FileUtils.checkAllowDownload(fileName)) {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = AniviaConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete) {
                FileUtils.deleteFile(filePath);
            }
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求（单个）
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(MultipartFile file) throws Exception {
        try {
            // 上传文件路径
            String filePath = AniviaConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 通用上传请求（多个）
     */
    @PostMapping("/uploads")
    public AjaxResult uploadFiles(List<MultipartFile> files) throws Exception {
        try {
            // 上传文件路径
            String filePath = AniviaConfig.getUploadPath();
            List<String> urls = new ArrayList<String>();
            List<String> fileNames = new ArrayList<String>();
            List<String> newFileNames = new ArrayList<String>();
            List<String> originalFilenames = new ArrayList<String>();
            for (MultipartFile file : files) {
                // 上传并返回新文件名称
                String fileName = FileUploadUtils.upload(filePath, file);
                String url = serverConfig.getUrl() + fileName;
                urls.add(url);
                fileNames.add(fileName);
                newFileNames.add(FileUtils.getName(fileName));
                originalFilenames.add(file.getOriginalFilename());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls", StringUtils.join(urls, FILE_DELIMETER));
            ajax.put("fileNames", StringUtils.join(fileNames, FILE_DELIMETER));
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMETER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMETER));
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 本地资源通用下载
     */
    @GetMapping("/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            if (!FileUtils.checkAllowDownload(resource)) {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 本地资源路径
            String localPath = AniviaConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + StringUtils.substringAfter(resource, Constants.RESOURCE_PREFIX);
            // 下载名称
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 获取excel列名并转换为csv
     *
     * @return
     */
/*    @PostMapping("/getExcelColumn")
    public AjaxResult getExcelColumn(@RequestBody ExcelColumnReqVO excelColumnReqVO) {
        String excelFile = excelColumnReqVO.getExcelFile();
        excelFile = AniviaConfig.getProfile() + excelFile.replace(Constants.RESOURCE_PREFIX + "/", "");
        excelFile = excelFile.replace("/", File.separator);
        Integer startColumn = excelColumnReqVO.getStartColumn();
        Integer startData = excelColumnReqVO.getStartData();
        String fileName = UUID.randomUUID().toString().replace("-", "") + ".csv";
        String csvFile = resourceUrl + "csv" + File.separator + fileName;
        List<String> columnList = ExcelToCsvUtil.convertExcelToCsv(excelFile, csvFile, startColumn, startData);
        if (columnList.size() > 0) {
            if (!ExcelToCsvUtil.verifyColumn(columnList)) {
                return AjaxResult.error("附件中列名格式有误，请检查!");
            }
        }
        String hdfsPath = "/tmp/etl";
        uploadHdfs(hdfsUrl, hdfsPath, csvFile, fileName);
        return AjaxResult.success(ColumnRespVO.builder()
                .csvFile(hdfsUrl + "/" + hdfsPath + "/" + fileName)
                .columnList(columnList)
                .build());
    }*/

    /**
     * 获取excel列名并转换为csv
     *
     * @return
     */
/*    @PostMapping("/getCsvColumn")
    public AjaxResult getCsvColumn(@RequestBody CsvColumnReqVO csvColumnReqVO) {
        String file = csvColumnReqVO.getFile();
        file = AniviaConfig.getProfile() + file.replace(Constants.RESOURCE_PREFIX + "/", "");
        file = file.replace("/", File.separator);
        String fileName = UUID.randomUUID().toString().replace("-", "") + ".csv";
        String csvFile = resourceUrl + "csv" + File.separator + fileName;
        List<String> columnList = ExcelToCsvUtil.parseCsv(file, csvFile);
        if (columnList.size() > 0) {
            if (!ExcelToCsvUtil.verifyColumn(columnList)) {
                return AjaxResult.error("附件中列名格式有误，请检查!");
            }
        }
        String hdfsPath = "/tmp/etl";
        uploadHdfs(hdfsUrl, hdfsPath, csvFile, fileName);
        return AjaxResult.success(ColumnRespVO.builder()
                .csvFile(hdfsUrl + "/" + hdfsPath + "/" + fileName)
                .columnList(columnList)
                .build());
    }*/

    /**
     * 获取JSONL列名并上传至HDFS。
     *
     * @param jsonlColumnReqVO JSONL文件信息
     * @return HDFS文件路径和字段列表
     */
/*
    @PostMapping("/getJsonlColumn")
    public AjaxResult getJsonlColumn(@RequestBody JsonlColumnReqVO jsonlColumnReqVO) {
        String file = jsonlColumnReqVO == null ? null : jsonlColumnReqVO.getFile();
        if (StringUtils.isBlank(file)) {
            throw new ServiceException("JSONL文件路径不能为空");
        }
        file = AniviaConfig.getProfile() + file.replace(Constants.RESOURCE_PREFIX + "/", "");
        file = file.replace("/", File.separator);

        String fileName = UUID.randomUUID().toString().replace("-", "") + ".jsonl";
        List<String> columnList = JsonlUtil.parseJsonl(file);

        String hdfsPath = "/tmp/etl";
        uploadJsonlToHdfs(hdfsUrl, hdfsPath, file, fileName);
        return AjaxResult.success(JsonlColumnRespVO.builder()
                .jsonlFile(hdfsUrl + "/" + hdfsPath + "/" + fileName)
                .columnList(columnList)
                .build());
    }
*/

    /**
     * 解析响应数据字段接口
     *
     * @return
     */
    @PostMapping("/getResponseColumnReqVO")
    public AjaxResult getResponseColumnReqVO(@RequestBody GetResponseColumnReqVO getResponseColumnReqVO) {
        List<JSONObject> result = new ArrayList<>();

        String apiUrl = getResponseColumnReqVO.getApiUrl();
        String apiMethod = getResponseColumnReqVO.getApiMethod();
        Integer returnFormat = getResponseColumnReqVO.getReturnFormat();
        List<String> returnDataLine = getResponseColumnReqVO.getReturnDataLine();
        Integer pageFlag = getResponseColumnReqVO.getPageFlag();
        GetResponseColumnReqVO.Page page = getResponseColumnReqVO.getPage();
        //分页参数key，需在参数中用${pageNo}进行占位，pageNo为当前参数的值
        String pageNoKey = null;
        //当前页数
        Integer pageNo = null;
        if (page != null) {
            pageNoKey = page.getPageNoKey();
            pageNo = 1;
        }
        GetResponseColumnReqVO.InParams inParams = getResponseColumnReqVO.getInParams();
        Integer type = inParams.getType();
        List<Map<String, Object>> urlParams = inParams.getUrlParams();
        List<JSONObject> apiHeaders = getResponseColumnReqVO.getApiHeaders();

        //封装请求接口对象
        HttpRequest request = HttpUtil.createRequest(Method.valueOf(apiMethod), apiUrl);

        while (true) {
            if (type == 1) {
                if (urlParams.size() > 0) {
                    for (int i = 0; i < urlParams.size(); i++) {
                        Map<String, Object> map = urlParams.get(i);
                        String value = String.valueOf(map.get("value"));
                        if (pageFlag == 1 && org.apache.commons.lang3.StringUtils.equals(value, "${" + pageNoKey + "}")) {
                            value = String.valueOf(pageNo);
                        }
                        request.form(String.valueOf(map.get("name")), value);
                    }
                }
            } else {
                String bodyParams = inParams.getBodyParams();
                if (org.apache.commons.lang3.StringUtils.isNotBlank(bodyParams)) {
                    if (pageFlag == 1 && org.apache.commons.lang3.StringUtils.indexOf(bodyParams, pageNoKey) > -1) {
                        bodyParams = org.apache.commons.lang3.StringUtils.replace(bodyParams, "${" + pageNoKey + "}", String.valueOf(pageNo));
                    }
                    request.body(bodyParams);
                }
            }

            //设置请求头
            if (apiHeaders.size() > 0) {
                Map<String, String> headers = apiHeaders.stream()
                        .collect(Collectors.toMap(key -> key.getString("name"), value -> value.getString("value")));
                request.addHeaders(headers);
            }

            //请求接口
            String responseBody = null;
            try {
                responseBody = request.execute().body();
            } catch (Exception e) {
                log.error("API URL 不可访问，请检查地址、网络或接口服务状态。", e);
                return AjaxResult.error("API URL 不可访问，请检查地址、网络或接口服务状态。");
            }
            if (StringUtils.isBlank(responseBody)) {
                return AjaxResult.error("API URL 不可访问，请检查地址、网络或接口服务状态。");
            }

            //解析数据
            try {
                Object response = JSON.parse(responseBody);
                Object data = null;
                if (returnFormat == 1 && response instanceof JSONObject) {
                    for (int i = 0; i < returnDataLine.size(); i++) {
                        String jsonPath = returnDataLine.get(i);
                        if (i + 1 < returnDataLine.size()) {
                            responseBody = JsonPath.read(responseBody, jsonPath);
                            continue;
                        }
                        data = JSON.parse(JSON.toJSONString(JsonPath.read(responseBody, jsonPath), JSONWriter.Feature.WriteMapNullValue));
                    }
                }
                if (returnFormat == 2 && response instanceof JSONArray) {
                    data = JSON.parse(JSON.toJSONString(JsonPath.read(responseBody, "$.[*]"), JSONWriter.Feature.WriteMapNullValue));
                }

                JSONObject obj = null;
                if (data instanceof JSONArray) {
                    if (((JSONArray) data).size() > 0) {
                        obj = (JSONObject) ((JSONArray) data).get(0);
                    }
                } else if (data instanceof JSONObject) {
                    obj = (JSONObject) data;
                }

                if (obj != null) {
                    for (String key : obj.keySet()) {
                        Object value = obj.get(key);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("name", key);
                        if (value == null) {
                            jsonObject.put("type", null);
                        } else if (value instanceof String) {
                            jsonObject.put("type", "STRING");
                        } else if (value instanceof Long) {
                            jsonObject.put("type", "LONG");
                        } else if (value instanceof Integer) {
                            jsonObject.put("type", "INTEGER");
                        } else if (value instanceof BigDecimal) {
                            jsonObject.put("type", "DOUBLE");
                        } else {
                            continue;
                        }
                        result.add(jsonObject);
                    }
                    return AjaxResult.success(result);
                }
            } catch (Exception e) {
                return AjaxResult.error("返回数据行所在字段与响应数据不匹配。");
            }
        }

    }

    /**
     * 上传文件至hdfs
     *
     * @param hdfsUrl  hdfs地址
     * @param pathStr  上传的路径
     * @param file     文件路径
     * @param filename 文件名称
     */
    public void uploadHdfs(String hdfsUrl, String pathStr, String file, String filename) {
        try {
            doUploadHdfs(hdfsUrl, pathStr, file, filename);
        } catch (Exception e) {
            log.error("Operation failed", e);
        }
    }

    private void uploadJsonlToHdfs(String hdfsUrl, String pathStr, String file, String filename) {
        try {
            doUploadHdfs(hdfsUrl, pathStr, file, filename);
        } catch (Exception e) {
            log.error("JSONL文件上传HDFS失败，hdfsUrl: {}, targetPath: {}, localFile: {}",
                    hdfsUrl, resolvePath(pathStr, filename), file, e);
            throw new ServiceException("JSONL文件上传HDFS失败，请检查HDFS连接和目录权限")
                    .setDetailMessage(e.getMessage());
        }
    }

    private void doUploadHdfs(String hdfsUrl, String pathStr, String file, String filename) throws Exception {
        pathStr = pathStr == null ? "" : pathStr;
        pathStr = resolvePath(pathStr, filename);
        // 1. 创建 Hadoop 配置对象
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUrl);
        conf.set("dfs.client.use.datanode.hostname", "true");
        // 如果只有1台DN，确保副本数别比节点数大
        conf.set("dfs.replication", "1");
        Path path = new Path(pathStr);
        try (FileSystem fs = FileSystem.get(new URI(hdfsUrl), conf, "hadoop"); InputStream inputStream = new FileInputStream(file); FSDataOutputStream outputStream = fs.create(path)) {
            IOUtils.copy(inputStream, outputStream);
        }
    }

    private String resolvePath(String path, String filename) {
        String str = path + "/" + filename;
        return str.replaceAll("/+", "/");
    }
}
