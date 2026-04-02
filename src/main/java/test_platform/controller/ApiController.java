package test_platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
import test_platform.entity.ApiDefinition;
import test_platform.entity.TestCase;
import test_platform.entity.TestResult;
import test_platform.mapper.ApiMapper;
import test_platform.mapper.TestCaseMapper;
import test_platform.mapper.TestLogMapper;
import test_platform.service.ApiService;
import test_platform.util.AssertionUtil;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private ApiMapper apiMapper;
    @Autowired private TestCaseMapper testCaseMapper;
    @Autowired private TestLogMapper testLogMapper;
    @Autowired private ApiService apiService;

    // ================== 核心执行接口 ==================

    @GetMapping("/case/run")
    public TestResult runTestCase(@RequestParam Integer caseId) {
        try {
            TestCase testCase = testCaseMapper.findByCaseId(caseId);
            if (testCase == null) return createErrorResult("用例不存在", 422);

            ApiDefinition api = apiMapper.findById(testCase.getApiId());
            if (api == null) return createErrorResult("接口定义不存在", 422);

            TestResult result = executeRequest(testCase, api);
            result.setCaseId(caseId);
            testLogMapper.insertLog(result);
            return result;
        } catch (Exception e) {
            return createErrorResult("执行失败: " + e.getMessage(), 500);
        }
    }

    private TestResult executeRequest(TestCase testCase, ApiDefinition api) {
        TestResult result = new TestResult();
        long start = System.currentTimeMillis();
        String rawBody = "";

        try {
            HttpUrl baseHttpUrl = HttpUrl.parse(api.getUrl());
            if (baseHttpUrl == null) return createErrorResult("URL格式非法", 422);

            HttpUrl.Builder urlBuilder = baseHttpUrl.newBuilder();
            // 解析请求参数
            if (testCase.getRequestParams() != null && !testCase.getRequestParams().isEmpty()) {
                JsonNode jsonNode = objectMapper.readTree(testCase.getRequestParams());
                jsonNode.fields().forEachRemaining(entry ->
                        urlBuilder.addQueryParameter(entry.getKey(), entry.getValue().asText())
                );
            }

            Request request = new Request.Builder().url(urlBuilder.build()).build();

            try (Response response = client.newCall(request).execute()) {
                int realStatus = response.code();
                result.setHttpStatus(realStatus);
                rawBody = (response.body() != null) ? response.body().string() : "";
                result.setResponseBody(formatJson(rawBody));

                if (rawBody.trim().startsWith("<")) {
                    return markFail(result, realStatus, "响应内容为HTML而非JSON");
                }

                String finalExpected = apiService.getFinalExpected(testCase.getId());
                JsonNode expectedNode = objectMapper.readTree(finalExpected != null ? finalExpected : "{}");

                int expectedStatus = expectedNode.has("_http_status") ? expectedNode.get("_http_status").asInt() : -1;
                boolean isStatusValid = (expectedStatus != -1) ? (realStatus == expectedStatus) : (realStatus >= 200 && realStatus < 400);
                boolean isBodyMatch = AssertionUtil.compareJson(finalExpected, rawBody);

                if (isStatusValid && isBodyMatch) {
                    result.setSuccess(true);
                    result.setBusinessCode(200);
                } else {
                    result.setSuccess(false);
                    result.setBusinessCode(!isStatusValid ? realStatus : 400);
                    result.setError(!isStatusValid ? "状态码不匹配" : "业务断言失败");
                }
            }
        } catch (SocketTimeoutException e) {
            result.setHttpStatus(504);
            // 检查预期是否就是超时
            String exp = testCase.getExpectedJson();
            if (exp != null && exp.contains("\"_http_status\": 504")) {
                result.setSuccess(true);
                result.setBusinessCode(200);
            } else {
                result.setSuccess(false);
                result.setBusinessCode(504);
                result.setError("接口响应超时");
            }
        } catch (Exception e) {
            result.setHttpStatus(-1);
            result.setBusinessCode(500);
            result.setError("执行异常: " + e.getMessage());
        } finally {
            result.setCost(System.currentTimeMillis() - start);
        }
        return result;
    }

    // ================== 管理类接口 ==================

    @GetMapping("/list")
    public List<ApiDefinition> getList() {
        return apiMapper.findAll();
    }

    @GetMapping("/case/list")
    public List<TestCase> getCaseList(@RequestParam Integer apiId) {
        return testCaseMapper.findByApiId(apiId);
    }

    @GetMapping("/log/recent")
    public List<TestResult> getRecentLogs(@RequestParam Integer caseId) {
        return testLogMapper.findRecentLogsByCaseId(caseId);
    }

    @PostMapping("/add")
    public String addApi(@RequestBody ApiDefinition api) {
        apiMapper.insert(api);
        return "保存成功";
    }

    // ================== 辅助工具 ==================

    private TestResult createErrorResult(String msg, int bizCode) {
        TestResult r = new TestResult();
        r.setSuccess(false);
        r.setError(msg);
        r.setHttpStatus(-1);
        r.setBusinessCode(bizCode);
        r.setCost(0L);
        return r;
    }

    private TestResult markFail(TestResult r, int bizCode, String msg) {
        r.setSuccess(false);
        r.setBusinessCode(bizCode);
        r.setError(msg);
        return r;
    }

    private String formatJson(String json) {
        try {
            Object obj = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) { return json; }
    }
}