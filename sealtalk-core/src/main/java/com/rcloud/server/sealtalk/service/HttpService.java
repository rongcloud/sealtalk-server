package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
@Service
@Slf4j
public class HttpService implements InitializingBean {


    private HttpClient httpClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        httpClient = HttpClient.newHttpClient();
    }


    public String postJson(String url, String body) throws Exception {
        return postJson(url,body,null);
    }

    public String postJson(String url, String body, Map<String, String> header) throws Exception {
        log.info("HTTP REQUEST:【POST】【JSON】【{}】【{}】【{}】", url, header == null ? "" : JacksonUtil.toJson(header), body);
        HttpRequest.Builder postRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (header != null) {
            header.forEach(postRequest::setHeader);
        }
        postRequest.setHeader("Content-Type", "application/json");
        HttpResponse<String> postResponse = httpClient.send(postRequest.build(), HttpResponse.BodyHandlers.ofString());
        String responseBody = postResponse.body();
        log.info("HTTP RESPONSE:【{}】【{}】", postResponse.statusCode(), responseBody);
        return responseBody;
    }


    public String postForm(String url, MultiValueMap<String, String> formData) throws Exception {
        return postForm(url, formData, null);
    }

    public String postForm(String url, MultiValueMap<String, String> formData, Map<String, String> header) throws Exception {
        String formBody = formData.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(value -> entry.getKey() + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)))
                .collect(Collectors.joining("&"));
        log.info("HTTP REQUEST:【POST】【FORM】【{}】【{}】【{}】", url, header == null ? "" : JacksonUtil.toJson(header), formBody);
        HttpRequest.Builder postRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(formBody));
        if (header != null) {
            header.forEach(postRequest::setHeader);
        }
        postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
        HttpResponse<String> postResponse = httpClient.send(postRequest.build(), HttpResponse.BodyHandlers.ofString());
        String responseBody = postResponse.body();
        log.info("HTTP RESPONSE:【{}】【{}】", postResponse.statusCode(), responseBody);
        return responseBody;
    }



}
