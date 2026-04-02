package test_platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import test_platform.entity.TestCase;
import test_platform.mapper.TestCaseMapper;

@Service
public class ApiService {

    @Autowired
    private TestCaseMapper testCaseMapper;

    private final ObjectMapper mapper = new ObjectMapper();

    public String getFinalExpected(Integer caseId) {
        try {
            // 调用Mapper
            TestCase testCase = testCaseMapper.findByIdWithApi(caseId);

            String def = testCase.getDefaultAssertion();
            String cse = testCase.getExpectedJson();

            // 深度合并逻辑
            if (def == null || def.isEmpty()) return cse;
            if (cse == null || cse.isEmpty()) return def;

            JsonNode defNode = mapper.readTree(def);
            JsonNode cseNode = mapper.readTree(cse);

            if (defNode.isObject() && cseNode.isObject()) {
                ((ObjectNode) defNode).setAll((ObjectNode) cseNode);
            }

            return mapper.writeValueAsString(defNode);
        } catch (Exception e) {
            return "{}";
        }
    }
}
