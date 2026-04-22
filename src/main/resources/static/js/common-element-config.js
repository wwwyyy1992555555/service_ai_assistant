/**
 * Element Plus 公共配置
 * 用于所有 iframe 子页面统一配置 Element Plus
 */

/**
 * 初始化 Element Plus（包含中文语言包和图标注册）
 * @param {Object} app - Vue 应用实例
 */
function initElementPlus(app) {
    // 配置 Element Plus，使用中文语言包（带容错）
    app.use(ElementPlus, { 
        locale: typeof ElementPlusLocaleZhCn !== 'undefined' ? ElementPlusLocaleZhCn : undefined 
    });
    
    // 注册所有图标（带容错）
    // 本地lib文件将图标直接挂载到window上，而非ElementPlusIconsVue对象
    const iconsToRegister = typeof ElementPlusIconsVue !== 'undefined' 
        ? ElementPlusIconsVue 
        : window; // 本地lib导出方式：图标直接在全局作用域
    
    // 常见图标名称列表（用于从window中筛选）
    const iconNames = [
        'Search', 'Plus', 'Delete', 'Edit', 'Close', 'Check', 'ArrowDown', 
        'ArrowUp', 'ArrowLeft', 'ArrowRight', 'User', 'Setting', 'Home',
        'Document', 'Folder', 'Upload', 'Download', 'Refresh', 'Loading',
        'Warning', 'InfoFilled', 'SuccessFilled', 'CircleClose', 'Bell',
        'ChatDotRound', 'ChatLineSquare', 'Calendar', 'Clock', 'Filter',
        'View', 'Hide', 'Lock', 'Unlock', 'Key', 'Phone', 'Message',
        'Avatar', 'Picture', 'Camera', 'VideoCamera', 'Microphone'
    ];
    
    for (const iconName of iconNames) {
        if (iconsToRegister[iconName]) {
            app.component(iconName, iconsToRegister[iconName]);
        }
    }
}

// 暴露到全局
window.initElementPlus = initElementPlus;
