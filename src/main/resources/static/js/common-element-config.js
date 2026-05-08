/**
 * Element Plus 公共配置
 * 用于所有 iframe 子页面统一配置 Element Plus
 */

/**
 * 初始化 Element Plus（包含中文语言包和图标注册）
 * @param {Object} app - Vue 应用实例
 * @returns {Promise}
 */
async function initElementPlus(app) {
    // 配置 Element Plus，使用中文语言包（带容错）
    app.use(ElementPlus, { 
        locale: typeof ElementPlusLocaleZhCn !== 'undefined' ? ElementPlusLocaleZhCn : undefined 
    });
    
    // 注册图标（使用本地图标库）
    // 本地图标库已将图标挂载到 ElementPlusIconsVue 对象
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
        // 从 ElementPlusIconsVue 中查找图标
        if (window.ElementPlusIconsVue && window.ElementPlusIconsVue[iconName]) {
            app.component(iconName, window.ElementPlusIconsVue[iconName]);
        }
    }
}

// 暴露到全局
window.initElementPlus = initElementPlus;
