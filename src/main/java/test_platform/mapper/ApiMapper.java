package test_platform.mapper;

import org.apache.ibatis.annotations.*;
import test_platform.entity.ApiDefinition;
import java.util.List;

@Mapper
public interface ApiMapper {
    @Insert("INSERT INTO api_definition(name, url, method, param_schema) " +
            "VALUES(#{name}, #{url}, #{method}, #{paramSchema})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApiDefinition api);

    @Select("SELECT * FROM api_definition")
    List<ApiDefinition> findAll();

    @Select("SELECT * FROM api_definition WHERE id = #{id}")
    ApiDefinition findById(Integer id);
}