package com.microapp.autumn.core.registry.consul.client.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/5/12
 */
@Slf4j
public class ConsulClient {
    public static CloseableHttpClient httpClient = null;

    private static volatile ConsulClient instance;
    private ConsulClient() {

    }
    public static ConsulClient getInstance() {
        if(Objects.isNull(instance)) {
            synchronized (ConsulClient.class) {
                if(Objects.isNull(instance)) {
                    instance = new ConsulClient();
                }
            }
        }
        return instance;
    }

    static {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(3000L))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(10000L))
                .setResponseTimeout(Timeout.ofMilliseconds(10000L))
                .build();
        try {
            httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .setConnectionManager(getHttpClientConnectionManager())
                    .build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            httpClient = HttpClients.createDefault();
            log.warn("http client config exception: {}", e);
        }
    }


    public static String doPut(String url, Map<String, String> uriParams, String json) {
        String result = null;
        HttpPut httpPut = new HttpPut(url);
        httpPut.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        if(Objects.nonNull(uriParams) && uriParams.size() > 0) {
            List<NameValuePair> nvps = new ArrayList<>();
            uriParams.forEach((k, v) -> {
                nvps.add(new BasicNameValuePair(k, v));
            });
            URI uri = null;
            try {
                uri = new URIBuilder(new URI(url))
                        .addParameters(nvps)
                        .build();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            httpPut.setUri(uri);
        }

        try {
            CloseableHttpResponse response = httpClient.execute(httpPut);
            result = EntityUtils.toString(response.getEntity());
        } catch (IOException | ParseException e) {
            if(log.isWarnEnabled()) {
                log.warn("http client do-put exception:{}", e);
            }
        }
        return result;
    }


    private static HttpClientConnectionManager getHttpClientConnectionManager() throws NoSuchAlgorithmException,
            KeyStoreException,
            KeyManagementException {
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(10)
                .setSSLSocketFactory(getSslConnectionSocketFactory())
                .build();
    }

    private static SSLConnectionSocketFactory getSslConnectionSocketFactory() throws NoSuchAlgorithmException,
            KeyStoreException,
            KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    public Boolean register(String consulUrl,
                            String instanceId,
                            String name,
                            List<String> tags,
                            String address,
                            Integer port,
                            Map<String, String> meta) {
        String body = handleRequestBody(instanceId, name, tags, address, port, meta);
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("replace-existing-check", "true");
        doPut(consulUrl, uriParams, body);
        return false;
    }

    public String handleRequestBody(String instanceId,
                                     String name,
                                     List<String> tags,
                                     String address,
                                     Integer port,
                                     Map<String, String> meta) {
        String tcp = address.concat(":").concat(port.toString());
        String tagString = "";

        if(Objects.nonNull(tags) && tags.size() > 0) {
            for (String it: tags) {
                tagString = tagString.concat("\"")
                        .concat(it)
                        .concat("\", ");
            }
            tagString = tagString.substring(0, tagString.length() - 2);
        } else {
            tagString = "";
        }

        String metaString = "";
        if(Objects.nonNull(meta) && meta.size() > 0) {
            for(Map.Entry<String, String> entry: meta.entrySet()) {
                metaString = metaString.concat("\"")
                        .concat(entry.getKey())
                        .concat("\": \"")
                        .concat(entry.getValue())
                        .concat("\", ");
            }
            metaString = metaString.substring(0, metaString.length() - 2);
        } else {
            metaString = "";
        }

        String json = """
                {
                    "ID": "%s",
                    "Name": "%s",
                    "Tags": [%s],
                    "Address": "%s",
                    "Port": %d,
                    "Meta": {%s},
                    "EnableTagOverride": true,
                    "Check": {
                        "DeregisterCriticalServiceAfter": "10m",
                        "TCP": "%s",
                        "Interval": "10s",
                        "Timeout": "5s"
                    },
                  "Weights": {
                    "Passing": 3,
                    "Warning": 1
                  }
                }
                """;
        String result = String.format(json, instanceId, name, tagString, address, port, metaString, tcp);
        return result;
    }

    public void list() {

    }

}
