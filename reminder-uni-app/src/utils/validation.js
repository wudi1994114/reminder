/**
 * 表单验证相关工具函数
 */

// 验证邮箱格式
export const isValidEmail = (email) => {
    if (!email) return false;
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

// 验证手机号格式
export const isValidPhone = (phone) => {
    if (!phone) return false;
    return /^1[3-9]\d{9}$/.test(phone);
};

// 验证密码强度
export const isStrongPassword = (password) => {
    if (!password) return false;
    return password.length >= 8 && 
           /[A-Z]/.test(password) && 
           /[a-z]/.test(password) && 
           /[0-9]/.test(password);
};

// 验证用户名格式
export const isValidUsername = (username) => {
    if (!username) return false;
    return /^[a-zA-Z0-9_]{3,20}$/.test(username);
};

// 验证必填字段
export const isRequired = (value) => {
    if (value === null || value === undefined) return false;
    if (typeof value === 'string') return value.trim().length > 0;
    if (Array.isArray(value)) return value.length > 0;
    return Boolean(value);
};

// 验证字符串长度
export const isValidLength = (value, min = 0, max = Infinity) => {
    if (!value) return min === 0;
    const length = typeof value === 'string' ? value.length : 0;
    return length >= min && length <= max;
};

// 验证数字范围
export const isValidRange = (value, min = -Infinity, max = Infinity) => {
    const num = Number(value);
    if (isNaN(num)) return false;
    return num >= min && num <= max;
};

// 验证URL格式
export const isValidUrl = (url) => {
    if (!url) return false;
    try {
        new URL(url);
        return true;
    } catch {
        return false;
    }
};

// 验证日期格式
export const isValidDate = (date) => {
    if (!date) return false;
    const parsedDate = new Date(date);
    return !isNaN(parsedDate.getTime());
};

// 验证未来日期
export const isFutureDate = (date) => {
    if (!isValidDate(date)) return false;
    return new Date(date) > new Date();
};

// 表单验证规则配置
export const validationRules = {
    email: {
        required: true,
        validator: isValidEmail,
        message: '请输入有效的邮箱地址'
    },
    phone: {
        validator: isValidPhone,
        message: '请输入有效的手机号码'
    },
    password: {
        required: true,
        validator: isStrongPassword,
        message: '密码至少8位，包含大小写字母和数字'
    },
    username: {
        required: true,
        validator: isValidUsername,
        message: '用户名只能包含字母、数字和下划线，3-20位'
    },
    required: {
        required: true,
        message: '此字段为必填项'
    }
};

// 通用表单验证函数
export const validateField = (value, rules) => {
    const errors = [];
    
    for (const rule of rules) {
        if (rule.required && !isRequired(value)) {
            errors.push(rule.message || '此字段为必填项');
            continue;
        }
        
        if (rule.min !== undefined || rule.max !== undefined) {
            if (!isValidLength(value, rule.min, rule.max)) {
                errors.push(rule.message || `长度应在${rule.min}-${rule.max}之间`);
                continue;
            }
        }
        
        if (rule.pattern && value) {
            if (!rule.pattern.test(value)) {
                errors.push(rule.message || '格式不正确');
                continue;
            }
        }
        
        if (rule.validator && value) {
            if (!rule.validator(value)) {
                errors.push(rule.message || '验证失败');
                continue;
            }
        }
    }
    
    return errors;
};

// 验证整个表单
export const validateForm = (formData, validationConfig) => {
    const errors = {};
    let isValid = true;
    
    for (const [fieldName, rules] of Object.entries(validationConfig)) {
        const fieldValue = formData[fieldName];
        const fieldErrors = validateField(fieldValue, rules);
        
        if (fieldErrors.length > 0) {
            errors[fieldName] = fieldErrors[0]; // 只显示第一个错误
            isValid = false;
        }
    }
    
    return { isValid, errors };
};

// 实时验证工具
export const createValidator = (rules) => {
    return (value) => {
        const errors = validateField(value, rules);
        return {
            isValid: errors.length === 0,
            error: errors[0] || null
        };
    };
}; 