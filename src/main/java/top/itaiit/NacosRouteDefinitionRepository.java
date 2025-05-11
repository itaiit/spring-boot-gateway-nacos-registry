package top.itaiit;

import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * nacos储存路由信息
 */
public class NacosRouteDefinitionRepository implements RouteDefinitionLocator, ApplicationEventPublisherAware {

    private static final Logger log = LoggerFactory.getLogger(NacosRouteDefinitionRepository.class);

    private final Listener configListener;
    private ApplicationEventPublisher publisher;
    private ConfigService configService;

    private final String groupId;
    private final String dataId;
    private final Properties properties;

    private final ObjectMapper objectMapper;

    private final ExecutorService pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(1), new NameThreadFactory("nacos-route-config"),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    public NacosRouteDefinitionRepository(NacosConfigProperties nacosConfig, ObjectMapper objectMapper) {
        this.dataId = nacosConfig.getName();
        this.groupId = nacosConfig.getGroup();
        this.properties = nacosConfig.assembleConfigServiceProperties();
        this.objectMapper = objectMapper;
        // 创建nacos监听器
        this.configListener = new Listener() {
            @Override
            public Executor getExecutor() {
                return pool;
            }
            @Override
            public void receiveConfigInfo(String configInfo) {
                // 解析规则并将规则信息加载到gateway中
                publisher.publishEvent(new RefreshRoutesEvent(this)); // 触发重新加载路由
            }
        };
        // 初始化nacos监听器
        initNacosListener();
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        // 从nacos中获取配置信息
        try {
            String config = configService.getConfig(dataId, groupId, 5000);
            // 获取到的是json数据，需要转换为RouteDefinition对象
            try {
                List<RouteDefinition> routes = objectMapper.readerForListOf(RouteDefinition.class).readValue(config);
                return Flux.fromIterable(routes);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    private void initNacosListener() {
        try {
            this.configService = NacosFactory.createConfigService(this.properties);
            // Add config listener.
            configService.addListener(dataId, groupId, configListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
}
