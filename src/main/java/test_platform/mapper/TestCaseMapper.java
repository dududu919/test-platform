package test_platform.mapper;

import org.apache.ibatis.annotations.*;
import test_platform.entity.TestCase;
import java.util.List;

@Mapper
public interface TestCaseMapper {

    @Insert("INSERT INTO test_case(api_id, case_name, request_params) " +
            "VALUES(#{apiId}, #{caseName}, #{requestParams})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestCase testCase);

    @Select("SELECT * FROM test_case WHERE api_id = #{apiId}")
    List<TestCase> findByApiId(Integer apiId);

    @Select("SELECT * FROM test_case WHERE id = #{id}")
    TestCase findByCaseId(Integer id);

    @Select("SELECT t.*, a.default_assertion " +
            "FROM test_case t " +
            "LEFT JOIN api_definition a ON t.api_id = a.id " +
            "WHERE t.id = #{id}")
    TestCase findByIdWithApi(Integer id);
}