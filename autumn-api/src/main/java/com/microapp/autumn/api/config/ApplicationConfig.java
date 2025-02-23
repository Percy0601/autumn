package com.microapp.autumn.api.config;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.Objects;
import java.util.Properties;

import org.apache.thrift.utils.StringUtils;

import com.microapp.autumn.api.util.CommonUtil;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/5/8
 */
@Slf4j
@Getter
@ToString
public class ApplicationConfig {
    private static volatile ApplicationConfig instance;

    private ApplicationConfig() {

    }
    private String name;
    private String registryType;
    private String multicastIp;
    private Integer multicastPort;
    private String jvm;
    private String arch;
    public static ApplicationConfig getInstance() {
        if(Objects.isNull(instance)) {
            synchronized (ApplicationConfig.class) {
                if(Objects.isNull(instance)) {
                    instance = new ApplicationConfig();
                    String appName = System.getProperty("spring.application.name");
                    if(Objects.isNull(appName) || appName.length() < 1) {
                        appName = System.getenv("spring.application.name");
                    }
                    Properties properties = CommonUtil.readClasspath("application.properties");
                    if(Objects.isNull(properties)) {
                        log.info("application start with params: -Dspring.application.name=xxx and ensure path exist config file: /opt/autumn/xxx.properties");
                        System.exit(0);
                        return null;
                    }
                    log.info("application name is: {}", appName);
                    instance.name = appName;

                    Properties configProperties = CommonUtil.readClasspath(appName.concat(".properties"));
                    if(Objects.isNull(properties)) {
                        log.info("application start config file: /opt/autumn/[app].properties");
                        System.exit(0);
                        return null;
                    }

                    instance.init(configProperties);
                }
            }
        }
        return instance;
    }

    public void init(Properties properties) {

        String registry_type = properties.getProperty("application.registry-type");
        if(Objects.isNull(registryType) || registryType.length() < 1) {
            registry_type = "multicast";
        }
        this.registryType = registry_type;

        String multicast_ip = properties.getProperty("application.multicast.ip");
        if(Objects.isNull(multicast_ip) || multicast_ip.length() < 1) {
            multicast_ip = "224.0.0.1";
        }
        this.multicastIp = multicast_ip;

        String multicast_port = properties.getProperty("application.multicast.port");
        if(Objects.isNull(multicast_port) || multicast_port.length() < 1) {
            multicast_port = "5555";
        }
        this.multicastPort = Integer.valueOf(multicast_port);
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String jvm = runtime.getVmName();
        this.jvm = jvm;

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        this.arch = os.getArch();
    }



}
