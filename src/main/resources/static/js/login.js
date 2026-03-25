// 创建 Vue 应用
const { createApp } = Vue;

// 检查 Vue 是否加载成功
if (typeof Vue === 'undefined') {
    console.error('❌ Vue 未加载！请检查网络连接或 CDN 链接');
    document.getElementById('app').innerHTML = `
        <div style="padding: 50px; text-align: center; font-family: Arial;">
            <h2 style="color: #f56c6c;">❌ 资源加载失败</h2>
            <p>请检查网络连接，然后刷新页面重试</p>
            <button onclick="location.reload()" style="padding: 10px 30px; margin-top: 20px; cursor: pointer;">刷新页面</button>
        </div>
    `;
} else {
    console.log('✅ Vue 加载成功，版本:', Vue.version);
}

const app = createApp({
    setup() {
        // 登录表单数据
        const loginForm = Vue.reactive({
            username: '',
            password: ''
        });

        // 表单验证规则
        const rules = {
            username: [
                { required: true, message: '请输入用户名', trigger: 'blur' }
            ],
            password: [
                { required: true, message: '请输入密码', trigger: 'blur' },
                { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
            ]
        };

        // 加载状态
        const loading = Vue.ref(false);

        // 表单引用
        const loginFormRef = Vue.ref(null);

        /**
         * 处理输入变化（防止 UI 变动）
         */
        const handleInputChange = () => {
            // 清空错误提示，但不触发验证
            if (loginFormRef.value) {
                loginFormRef.value.clearValidate();
            }
        };

        /**
         * 处理登录
         */
        const handleLogin = async () => {
            if (!loginFormRef.value) return;

            await loginFormRef.value.validate(async (valid) => {
                if (valid) {
                    loading.value = true;

                    try {
                        // 调用登录 API
                        const response = await fetch('/api/auth/login', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                username: loginForm.username,
                                password: loginForm.password,
                                tenantId: 1 // 默认租户 ID
                            })
                        });

                        const result = await response.json();

                        if (result.code === 200 && result.data) {
                            // 登录成功，保存用户信息
                            const userData = result.data;
                            localStorage.setItem('user', JSON.stringify(userData));
                            localStorage.setItem('token', userData.token);

                            ElementPlus.ElMessage.success('登录成功！');

                            // 跳转到管理后台
                            setTimeout(() => {
                                window.location.href = '/admin.html';
                            }, 500);
                        } else {
                            ElementPlus.ElMessage.error(result.message || '登录失败');
                        }
                    } catch (error) {
                        console.error('登录失败:', error);
                        ElementPlus.ElMessage.error('网络错误，请稍后重试');
                    } finally {
                        loading.value = false;
                    }
                }
            });
        };

        return {
            loginForm,
            rules,
            loading,
            loginFormRef,
            handleLogin,
            handleInputChange
        };
    }
});

// 使用 Element Plus
app.use(ElementPlus);

// 挂载应用（确保在 DOM 加载后执行）
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        app.mount('#app');
    });
} else {
    app.mount('#app');
}
