package test_platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TestCase {
    private Integer id;
    private Integer apiId;       // 关联 api_definition 表的 id
    private String caseName;     // 用例名称
    private String requestParams; // 实际运行时的 JSON 参数
    private LocalDateTime createTime;
    private String expectedJson;
    private String defaultAssertion;
}
