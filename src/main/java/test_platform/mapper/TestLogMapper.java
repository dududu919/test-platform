package test_platform.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import test_platform.entity.TestResult;
import java.util.List;

@Mapper
public interface TestLogMapper {

    // 插入运行日志
    @Insert("INSERT INTO test_log (case_id, http_status, business_code, cost, response_body, success, run_time) " +
            "VALUES (#{caseId}, #{httpStatus}, #{businessCode}, #{cost}, #{responseBody}, #{success}, #{runTime})")
    void insertLog(TestResult result);

    // 获取最近 5 条记录
    @Select("SELECT * FROM test_log WHERE case_id = #{caseId} ORDER BY id DESC LIMIT 5")
    List<TestResult> findRecentLogsByCaseId(Integer caseId);
}