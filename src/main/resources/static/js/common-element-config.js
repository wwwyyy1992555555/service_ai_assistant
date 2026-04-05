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
    if (typeof ElementPlusIconsVue !== 'undefined') {
        for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
            app.component(key, component);
        }
    }
}

// 暴露到全局
window.initElementPlus = initElementPlus;
