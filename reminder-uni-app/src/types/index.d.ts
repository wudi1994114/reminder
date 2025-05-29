// API响应基础类型
export interface ApiResponse<T = any> {
    code: number;
    message: string;
    data: T;
}

// 用户相关类型
export interface User {
    id: number | string;
    username: string;
    email: string;
    phone?: string;
    avatar?: string;
    nickname?: string;
    createdAt?: string;
    updatedAt?: string;
}

export interface LoginCredentials {
    username: string;
    password: string;
    rememberMe?: boolean;
}

export interface RegisterData {
    username: string;
    email: string;
    password: string;
    confirmPassword: string;
    phone?: string;
    nickname?: string;
}

// 提醒相关类型
export interface SimpleReminder {
    id: number | string;
    title: string;
    description?: string;
    eventTime: string | Date;
    reminderType?: 'once' | 'daily' | 'weekly' | 'monthly';
    email?: string;
    isCompleted?: boolean;
    createdAt?: string;
    updatedAt?: string;
}

export interface ComplexReminder {
    id: number | string;
    title: string;
    description?: string;
    cronExpression: string;
    isActive: boolean;
    nextExecution?: string;
    lastExecution?: string;
    email?: string;
    createdAt?: string;
    updatedAt?: string;
}

// 日历相关类型
export interface CalendarEvent {
    id: number | string;
    title: string;
    start: Date;
    end: Date;
    description?: string;
    type: 'simple' | 'complex' | 'holiday';
    color?: string;
    allDay?: boolean;
}

export interface Holiday {
    date: string;
    name: string;
    type: 'holiday' | 'workday' | 'weekend';
    isOffDay: boolean;
}

// UI相关类型
export interface Notification {
    id: number;
    message: string;
    type: 'info' | 'success' | 'warning' | 'error';
    duration: number;
    timestamp?: Date;
}

export interface ModalState {
    isEventModalOpen: boolean;
    isProfileModalOpen: boolean;
    currentEditingEvent: SimpleReminder | ComplexReminder | null;
}

// 状态管理类型
export interface UserState {
    isAuthenticated: boolean;
    user: User | null;
    loading: boolean;
    error: string | null;
}

export interface ReminderState {
    simpleReminders: SimpleReminder[];
    complexReminders: ComplexReminder[];
    upcomingReminders: SimpleReminder[];
    loading: boolean;
    error: string | null;
    lastFetchTime: Date | null;
}

export interface UIState {
    isEventModalOpen: boolean;
    isProfileModalOpen: boolean;
    isLoadingOverlayVisible: boolean;
    currentEditingEvent: SimpleReminder | ComplexReminder | null;
    notifications: Notification[];
    theme: 'light' | 'dark';
    language: string;
}

// 请求相关类型
export interface RequestOptions {
    url: string;
    method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
    data?: any;
    header?: Record<string, string>;
    timeout?: number;
}

// 分页相关类型
export interface PaginationParams {
    page: number;
    pageSize: number;
    total?: number;
}

export interface PaginatedResponse<T> {
    items: T[];
    pagination: {
        page: number;
        pageSize: number;
        total: number;
        totalPages: number;
    };
}

// 表单验证类型
export interface ValidationRule {
    required?: boolean;
    min?: number;
    max?: number;
    pattern?: RegExp;
    message: string;
}

export interface FormField {
    value: any;
    rules: ValidationRule[];
    error?: string;
}

// 组件Props类型
export interface CalendarProps {
    events?: CalendarEvent[];
    selectedDate?: Date;
    onDateSelect?: (date: Date) => void;
    onEventClick?: (event: CalendarEvent) => void;
}

export interface ReminderFormProps {
    reminder?: SimpleReminder | ComplexReminder;
    isEdit?: boolean;
    onSubmit?: (reminder: SimpleReminder | ComplexReminder) => void;
    onCancel?: () => void;
} 