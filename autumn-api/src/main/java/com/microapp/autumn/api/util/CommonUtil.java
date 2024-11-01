package com.microapp.autumn.api.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonUtil {
    private static Map<String, Properties> mapping = null;
    private CommonUtil() {

    }
    static {
        mapping = new ConcurrentHashMap<>();
    }


    public static String getConnectionPath(String service, String ipPort) {
        String interfacePath = service.concat("/")
                .concat(ipPort);
        return interfacePath;
    }

    public static String getHostIpAddress() {
        NetworkInterface netInterface = getNetIf();
        Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress ip = addresses.nextElement();
            if (ip != null) {
                if (ip instanceof Inet4Address) {
                    return ip.getHostAddress();
                }
            }
        }
        return null;
    }

    public static NetworkInterface getNetIf() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface =  allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
                    continue;
                }

                if (!netInterface.getDisplayName().contains("Intel")
                        && !netInterface.getDisplayName().contains("Realtek")
                        && !netInterface.getDisplayName().contains("Atheros")
                        && !netInterface.getDisplayName().contains("Broadcom")) {
                    continue;
                }
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                if(!addresses.hasMoreElements()) {
                    continue;
                }
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip != null) {
                        if (ip instanceof Inet4Address) {
                            return netInterface;
                        }
                    }
                }
                break;
            }
        } catch (SocketException e) {
            e.getMessage();
        }
        return null;
    }

    /**
     * read config<br>
     * <pre>
     *     1、classpath
     *     2、/opt/config/
     * </pre>
     * @param filename
     * @return
     */
    public static Properties readClasspath(String filename) {
        if(mapping.containsKey(filename)) {
            Properties properties = mapping.get(filename);
            return properties;
        }

        Path path = null;
        URI uri = null;
        URL url = CommonUtil.class.getClassLoader()
                .getResource(filename);
        if(Objects.isNull(url)) {
            uri = new File("/opt/config/".concat(filename)).toURI();
        } else {
            try {
                uri = url.toURI();
            } catch (URISyntaxException e) {
                log.info("url to uri exception: ", e);
            }
        }
        path = Path.of(uri);
        String content = "";
        try {
            content = Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Reader reader = new StringReader(content);
        Properties properties = new Properties();
        try {
            properties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mapping.put(filename, properties);

        return properties;
    }
}
