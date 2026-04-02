package test_platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiDefinition {
    private Integer id;
    private String name;
    private String url;
    private String method;
    private String paramSchema;
    private LocalDateTime createTime;
}
