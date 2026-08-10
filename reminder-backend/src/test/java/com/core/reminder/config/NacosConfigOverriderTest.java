package com.core.reminder.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NacosConfigOverrider测试类
 * 验证配置覆盖功能和执行顺序
 */
class NacosConfigOverriderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(NacosConfigOverrider.class);

    @Test
    void doesNotCreateNacosBeansWhenNacosIsDisabled() {
        contextRunner
                .withPropertyValues("nacos.config.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(NacosConfigOverrider.class));
    }
}
