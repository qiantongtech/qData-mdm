/*
 * Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This file is part of qData-mdm Master Data Platform (Open Source Edition).
 *
 * qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
 * You may use qData-mdm for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData-mdm logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData-mdm as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

/**
 * Determine whether the url is http or https
 * @param {string} path
 * @returns {Boolean}
 */
export function isHttp(url) {
    return url.indexOf('http://') !== -1 || url.indexOf('https://') !== -1;
}

/**
 * Determine whether the path is an external link
 * @param {string} path
 * @returns {Boolean}
 */
export function isExternal(path) {
    return /^(https?:|mailto:|tel:)/.test(path);
}

/**
 * @param {string} str
 * @returns {Boolean}
 */
export function validUsername(str) {
    const valid_map = ['admin', 'editor'];
    return valid_map.indexOf(str.trim()) >= 0;
}

/**
 * @param {string} url
 * @returns {Boolean}
 */
export function validURL(url) {
    const reg =
        /^(https?|ftp):\/\/([a-zA-Z0-9.-]+(:[a-zA-Z0-9.&%$-]+)*@)*((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}|([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.(com|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|[a-zA-Z]{2}))(:[0-9]+)*(\/($|[a-zA-Z0-9.,?'\\+&%$#=~_-]+))*$/;
    return reg.test(url);
}

/**
 * @param {string} str
 * @returns {Boolean}
 */
export function validLowerCase(str) {
    const reg = /^[a-z]+$/;
    return reg.test(str);
}

/**
 * @param {string} str
 * @returns {Boolean}
 */
export function validUpperCase(str) {
    const reg = /^[A-Z]+$/;
    return reg.test(str);
}

/**
 * @param {string} str
 * @returns {Boolean}
 */
export function validAlphabets(str) {
    const reg = /^[A-Za-z]+$/;
    return reg.test(str);
}

/**
 * @param {string} email
 * @returns {Boolean}
 */
export function validEmail(email) {
    const reg =
        /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return reg.test(email);
}

/**
 * @param {string} str
 * @returns {Boolean}
 */
export function isString(str) {
    if (typeof str === 'string' || str instanceof String) {
        return true;
    }
    return false;
}

/**
 * @param {Array} arg
 * @returns {Boolean}
 */
export function isArray(arg) {
    if (typeof Array.isArray === 'undefined') {
        return Object.prototype.toString.call(arg) === '[object Array]';
    }
    return Array.isArray(arg);
}


/** 检查是否等于string */
export const trimText = (value) => {
    return typeof value === "string" ? value.trim() : value;
}

/**
 * 生成一个必填且去除前后空格的验证器，可选择包含最大长度校验。
 * @param {string} message - 校验失败的提示信息。
 * @param {number} [maxLength] - 可选的最大长度。
 * @returns {Function} Element Plus 兼容的验证器函数。
 */
export const validateTrimmedRequired = (message, maxLength) => (rule, value, callback) => {
    if (value === null || value === undefined || value === '') {
        callback(new Error(message));
    } else {
        const trimmedValue = String(value).trim();
        if (trimmedValue.length === 0) {
            callback(new Error(message));
        } else if (maxLength && trimmedValue.length > maxLength) {
            callback(new Error(`${message.replace('不能为空', '')}不能超过${maxLength}个字符`));
        } else {
            callback();
        }
    }
};

/**
 * 生成一个基于正则表达式的格式验证器。
 * @param {RegExp} pattern - 用于校验的正则表达式。
 * @param {string} message - 校验失败的提示信息。
 * @returns {Function} Element Plus 兼容的验证器函数。
 */
export const validatePattern = (pattern, message) => (rule, value, callback) => {
    if (value === null || value === undefined || value === '') {
        callback(); // 如果值为空，则不进行格式校验，由必填项校验处理
    } else if (!pattern.test(String(value))) {
        callback(new Error(message));
    } else {
        callback();
    }
};

/**
 * 生成一个只包含字母、数字和下划线的验证器。
 * @param {string} [message='只能包含字母、数字和下划线'] - 校验失败的提示信息。
 * @returns {Function} Element Plus 兼容的验证器函数。
 */
export const validateAlphaNumUnderscore = (message = '只能包含字母、数字和下划线') => (rule, value, callback) => {
    if (value === null || value === undefined || value === '') return callback();
    const reg = /^[a-zA-Z0-9_]+$/;
    if (!reg.test(String(value))) {
        callback(new Error(message));
    } else {
        callback();
    }
};
