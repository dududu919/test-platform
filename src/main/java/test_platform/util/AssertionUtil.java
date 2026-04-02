package test_platform.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

public class AssertionUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static boolean compareJson(String expectedStr, String actualStr) {
        if (expectedStr == null || expectedStr.trim().isEmpty() || "{}".equals(expectedStr.trim())) {
            return true;
        }
        try {
            JsonNode expectedNode = mapper.readTree(expectedStr);
            JsonNode actualNode = mapper.readTree(actualStr);
            return isMatch(expectedNode, actualNode);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isMatch(JsonNode expected, JsonNode actual) {
        // 1. 对象类型：执行 Subset匹配
        if (expected.isObject()) {
            if (!actual.isObject()) return false;
            Iterator<Map.Entry<String, JsonNode>> fields = expected.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                if ("_http_status".equals(key)) continue; // 跳过协议标记位

                if (!actual.has(key) || !isMatch(entry.getValue(), actual.get(key))) {
                    return false;
                }
            }
            return true;
        }

        // 2. 文本类型：模式匹配
        if (expected.isTextual()) {
            String expText = expected.asText();
            String actText = (actual == null || actual.isNull()) ? "" : actual.asText();

            if ("[not_null]".equals(expText)) return !actText.trim().isEmpty();

            if (expText.startsWith("[range:")) return checkRange(expText, actual);

            if (expText.startsWith("[regex:")) {
                String regex = expText.substring(7, expText.length() - 1);
                return Pattern.compile(regex).matcher(actText).find();
            }

            if (expText.startsWith("[contains:")) {
                String sub = expText.substring(10, expText.length() - 1);
                return actText.contains(sub);
            }

            return expText.equals(actText);
        }

        // 3. 基本类型：直接比对
        return expected.equals(actual);
    }

    private static boolean checkRange(String rangeStr, JsonNode actualNode) {
        try {
            double val = actualNode.isNumber() ? actualNode.asDouble() : Double.parseDouble(actualNode.asText());
            String[] parts = rangeStr.replace("[range:", "").replace("]", "").split(",");
            double min = Double.parseDouble(parts[0].trim());
            double max = Double.parseDouble(parts[1].trim());
            return val >= min && val <= max;
        } catch (Exception e) { return false; }
    }
}