package test_platform.entity;

import lombok.Data;

@Data
public class TestResult {
    private Integer id;
    private Integer caseId;
    private Integer httpStatus = -1;
    private Integer businessCode;
    private String responseBody; // 接口返回的真实内容
    private Long cost;           // 耗时 (毫秒)
    private String error;        // 如果断网了，记录错误信息
    private Boolean success;
    private String runTime;
}
