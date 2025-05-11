# spring-boot-gateway-nacos-registry
Configure Gateway Routes Using Nacos

使用Nacos作为配置中心动态配置Gateway的路由信息

配置开启自动注入:
```yaml
spring:
  cloud:
    gateway:
      config:
          nacos:
            enabled: true
```
nacos配置信息复用NacosConfigProperties配置类的配置