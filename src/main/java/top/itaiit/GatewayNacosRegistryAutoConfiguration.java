package top.itaiit;

import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.gateway.config.nacos.enabled", havingValue = "true", matchIfMissing = false)
public class GatewayNacosRegistryAutoConfiguration {

    @Bean
    @ConditionalOnBean({GatewayAutoConfiguration.class, NacosConfigProperties.class})
    public RouteDefinitionLocator nacosRouteDefinitionLocator(NacosConfigProperties nacosConfig, ObjectMapper objectMapper) {
        return new NacosRouteDefinitionRepository(nacosConfig, objectMapper);
    }

}
