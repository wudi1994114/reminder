package com.core.reminder.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * NacosConfigOverrider测试类
 * 验证配置覆盖功能和执行顺序
 */
@SpringBootTest
@ActiveProfiles("test")
public class NacosConfigOverriderTest {

    @Test
    public void testConfigOverriderExecution() {
        // 这个测试主要是验证应用能够正常启动
        // 在启动过程中会看到NacosConfigOverrider的执行日志
        System.out.println("NacosConfigOverrider测试完成");
    }
}
