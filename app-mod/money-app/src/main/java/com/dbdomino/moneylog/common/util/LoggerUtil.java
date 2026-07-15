package com.dbdomino.moneylog.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.dbdomino.moneylog.dto.response.RestResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 *  Request 헤더, 바디를, Response 바디를 json으로 가져오는 유틸
 */

@Component
@RequiredArgsConstructor
public class LoggerUtil {

    private final Gson gson;

    public String logRequestHeaders(HttpServletRequest request) { //  HTTP request 의 Header 정보를 JSON 형식으로 변환
        try {
           Enumeration<String> headerNames = request.getHeaderNames();
           Map<String,String> headerMap = new HashMap<>();

           while(headerNames.hasMoreElements()) {
               String headerName = headerNames.nextElement();
               String headerValue = request.getHeader(headerName);
               headerMap.put(headerName, headerValue);
           }

           JsonElement jsonElement = gson.toJsonTree(headerMap);
//           return jsonElement.toString(); 순서, 직렬화 없이 json 형식 String으로 변환
           return gson.toJson(jsonElement); // 순서 맞춰 json 형식 String으로 변환

        }catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public String getRequestBody(HttpServletRequest request) { // HTTP request의 Body를 JSON 형식으로 변환
        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
            StringBuilder requestBody = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                requestBody.append(line.strip());
            }

            JsonElement jsonElement = JsonParser.parseString(requestBody.toString());
            return gson.toJson(jsonElement);

        } catch (IOException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public String getResponseBody(Object returnRequest) { // HTTP response 의 Body가 RestResponseDto 형식인지 확인하고, 맞다면 Json 형식으로 변환
        try {
            if (returnRequest instanceof RestResponseDto<?> && ((ResponseEntity<?>) returnRequest).getBody() instanceof RestResponseDto<?>) {
                JsonElement jsonElement = JsonParser.parseString(
                        new JsonMapper().writeValueAsString(
                                ( (ResponseEntity<?>) returnRequest ).getBody()
                        )
                );
                return gson.toJson(jsonElement);

            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }

}
