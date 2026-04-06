package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ExtractJsonTest {

    @Test
    void testExtractFromArray() throws Exception {
        String response = "[{\"id\":123,\"score\":7,\"reason\":\"test\"}]";
        String result = invokeExtract(response);
        assertEquals("[{\"id\":123,\"score\":7,\"reason\":\"test\"}]", result);
    }

    @Test
    void testExtractFromObject() throws Exception {
        String response = "{\"categoryId\":5,\"articleTopSummaryEn\":\"summary\"}";
        String result = invokeExtract(response);
        assertEquals("{\"categoryId\":5,\"articleTopSummaryEn\":\"summary\"}", result);
    }

    @Test
    void testExtractAfterThinkMore() throws Exception {
        String response = "<think_more>reasoning here</think_more>\n[{\"id\":123,\"score\":7,\"reason\":\"test\"}]";
        String result = invokeExtract(response);
        assertEquals("[{\"id\":123,\"score\":7,\"reason\":\"test\"}]", result);
    }

    @Test
    void testExtractFromJsonCodeBlock() throws Exception {
        String response = "```json\n[{\"id\":123,\"score\":7,\"reason\":\"test\"}]\n```";
        String result = invokeExtract(response);
        assertEquals("[{\"id\":123,\"score\":7,\"reason\":\"test\"}]", result);
    }

    @Test
    void testExtractMultiLineJsonFromCodeBlock() throws Exception {
        String response = "```json\n{\n  \"categoryId\": 5,\n  \"articleTopSummaryEn\": \"Summary text\",\n  \"articles\": []\n}\n```";
        String result = invokeExtract(response);
        assertTrue(result.startsWith("{"));
        assertTrue(result.contains("\"categoryId\": 5"));
        assertTrue(result.endsWith("}"));
        assertFalse(result.contains("```"));
    }

    @Test
    void testExtractSummaryWithThinkMoreAndCodeBlock() throws Exception {
        String response = "<think_more>Analyzing the article content...</think_more>\n\n```json\n{\n  \"categoryId\": 5,\n  \"articleTopSummaryEn\": \"Test summary\",\n  \"articleTopSummaryZh\": \"测试摘要\",\n  \"articles\": []\n}\n```";
        String result = invokeExtract(response);
        assertTrue(result.startsWith("{"));
        assertTrue(result.contains("\"categoryId\": 5"));
        assertTrue(result.contains("\"articleTopSummaryEn\": \"Test summary\""));
        assertFalse(result.contains("```"));
        assertFalse(result.contains("<think_more>"));
    }

    @Test
    void testExtractFromCodeBlock() throws Exception {
        String response = "```\n[{\"id\":123,\"score\":7,\"reason\":\"test\"}]\n```";
        String result = invokeExtract(response);
        assertEquals("[{\"id\":123,\"score\":7,\"reason\":\"test\"}]", result);
    }

    @Test
    void testExtractFromOutputTag() throws Exception {
        String response = "<output>[{\"id\":123,\"score\":7,\"reason\":\"test\"}]</output>";
        String result = invokeExtract(response);
        assertEquals("[{\"id\":123,\"score\":7,\"reason\":\"test\"}]", result);
    }

    @Test
    void testExtractWithReasoningAndJson() throws Exception {
        String response = "<think_more>This article is about CPU+GPU coordination which fits the category well.</think_more>\n\n[{\"id\":1775061199095495,\"score\":10,\"reason\":\"Article discusses CPU+GPU coordination, Tensor Cores, Intel Xeon 6 as host for NVIDIA DGX Rubin, all CS topics; from Intel blog, a credible CS source; fits Other CS Sources category perfectly.\"}]";
        String result = invokeExtract(response);
        assertEquals("[{\"id\":1775061199095495,\"score\":10,\"reason\":\"Article discusses CPU+GPU coordination, Tensor Cores, Intel Xeon 6 as host for NVIDIA DGX Rubin, all CS topics; from Intel blog, a credible CS source; fits Other CS Sources category perfectly.\"}]", result);
    }

    @Test
    void testExtractMultipleArticles() throws Exception {
        String response = "<think_more>reasoning</think_more>\n[\n  {\"id\":1,\"score\":5,\"reason\":\"a\"},\n  {\"id\":2,\"score\":3,\"reason\":\"b\"}\n]";
        String result = invokeExtract(response);
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("\"id\":1"));
        assertTrue(result.contains("\"id\":2"));
    }

    @Test
    void testTruncatedJson() throws Exception {
        String response = "<think_more>reasoning</think_more>\n[{\"id\":123,\"score\":7,\"reason\":\"test";
        String result = invokeExtract(response);
        assertEquals("[{\"id\":123,\"score\":7,\"reason\":\"test\"}]", result);
    }

    @Test
    void testTruncatedSummaryJson() throws Exception {
        String truncatedJson = "{\n  \"categoryId\": 4,\n  \"articleTopSummaryEn\": \"Summary\",\n  \"articles\": [\n    {\"title\": \"Test\"";
        String result = invokeExtract(truncatedJson);
        assertTrue(result.startsWith("{"));
        assertTrue(result.contains("\"categoryId\": 4"));
        assertTrue(result.contains("\"articleTopSummaryEn\": \"Summary\""));
        assertTrue(result.contains("\"articles\":"));
        assertTrue(result.contains("\"title\": \"Test\""));
        assertTrue(result.endsWith("}"));
        
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> mapper.readTree(result));
    }

    @Test
    void testTruncatedSummaryWithOpenArticle() throws Exception {
        String truncatedJson = "{\n  \"categoryId\": 4,\n  \"articles\": [\n    {\"title\": \"Test\",\n     \"abstract\": \"Some text\"";
        String result = invokeExtract(truncatedJson);
        assertTrue(result.contains("\"title\": \"Test\""));
        assertTrue(result.contains("\"abstract\": \"Some text\""));
        
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> mapper.readTree(result));
    }

    @Test
    void testTruncatedAtStringEnd() throws Exception {
        String truncatedJson = "[{\"id\":1,\"reason\":\"incomplete text";
        String result = invokeExtract(truncatedJson);
        assertEquals("[{\"id\":1,\"reason\":\"incomplete text\"}]", result);
    }

    @Test
    void testDoubleEscapedQuotes() throws Exception {
        String json = "{\"title\": \"Test\", \"abstract\": \"He said \\\\\"hello\\\\\" world\"}";
        String result = invokeExtract(json);
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> mapper.readTree(result));
    }

    @Test
    void testJsonWithRealEscapeIssue() throws Exception {
        String json = "{\"abstract\": \"Meng Wanzhou emphasized the need to \\\\\"restrain development boundaries,\\\\\" suggesting a focus on growth\"}";
        String result = invokeExtract(json);
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> mapper.readTree(result));
    }

    @Test
    void testComplexJsonWithAllFields() throws Exception {
        String json = "{\n  \"categoryId\": 3,\n  \"articleTopSummaryEn\": \"Summary\",\n  \"articleTopSummaryZh\": \"摘要\",\n  \"articles\": [\n    {\"title\": \"Test\", \"abstract\": \"Abstract\", \"articleLink\": \"http://test.com\", \"authors\": [], \"titleCn\": \"测试\", \"abstractCn\": \"摘要\", \"background\": \"bg\", \"effects\": \"ef\", \"eventSummary\": \"es\", \"technologyAndInnovation\": \"ti\", \"valueAndImpact\": \"vi\", \"backgroundCn\": \"背景\", \"effectsCn\": \"效果\", \"eventSummaryCn\": \"事件\", \"technologyAndInnovationCn\": \"技术\", \"valueAndImpactCn\": \"价值\"}\n  ]\n}";
        String result = invokeExtract(json);
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> mapper.readTree(result));
    }

    @Test
    void testArrayWrappedInObject() throws Exception {
        String json = "{\"rankings\": [{\"id\": 123, \"score\": 7, \"reason\": \"test\"}]}";
        String extracted = invokeExtract(json);
        String result = invokeExtractArray(extracted);
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> mapper.readTree(result));
    }

    @Test
    void testSingleObjectWrappedToArray() throws Exception {
        String json = "{\"id\": 1775217723062369, \"score\": 9, \"reason\": \"Article directly discusses voltage-controlled magnetic anisotropy\"}";
        String extracted = invokeExtract(json);
        String result = invokeExtractArray(extracted);
        System.out.println("Result: [" + result + "]");
        assertTrue(result.startsWith("["), "Should start with [");
        assertTrue(result.endsWith("]"), "Should end with ]");
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> mapper.readTree(result));
    }

    @Test
    void testPromptFormatNotCorrupted() throws Exception {
        String rankingFormat = loadResource("prompt/ranking-format.txt");
        int categoryId = 10;
        String categoryName = "Test Category";
        
        String prompt = loadResource("prompt/ranking-prompt.txt")
            .replace("{{categoryName}}", categoryName)
            .replace("{{batchNum}}", "1")
            .replace("{{totalBatches}}", "1")
            .replace("{{jsonFormat}}", rankingFormat);
        
        assertTrue(prompt.contains("\"id\": 12345"), "Format should have example id");
        assertTrue(prompt.contains("\"score\": 8"), "Format should have example score");
        assertFalse(prompt.contains("\"type\":"), "Format should NOT have schema type field");
        assertFalse(prompt.contains("$schema"), "Format should NOT have $schema field");
    }

    @Test
    void testSummaryFormatNotCorrupted() throws Exception {
        String summaryFormat = loadResource("prompt/summary-format.txt")
            .replace("{{categoryId}}", "10");
        String categoryName = "Test Category";
        
        String prompt = loadResource("prompt/summary-prompt.txt")
            .replace("{{categoryName}}", categoryName)
            .replace("{{jsonFormat}}", summaryFormat);
        
        assertTrue(prompt.contains("\"categoryId\": 10"), "Format should have categoryId replaced");
        assertTrue(prompt.contains("\"articleTopSummaryEn\":"), "Format should have articleTopSummaryEn");
        assertFalse(prompt.contains("\"type\": \"integer\""), "Format should NOT have schema type field");
        assertFalse(prompt.contains("$schema"), "Format should NOT have $schema field");
    }

    @Test
    void testExtractJsonWithSchemaExample() throws Exception {
        String response = "{\n  \"categoryId\": 10,\n  \"articleTopSummaryEn\": \"Summary text\",\n  \"articleTopSummaryZh\": \"中文摘要\",\n  \"articles\": []\n}";
        String result = invokeExtract(response);
        assertTrue(result.contains("\"categoryId\": 10"));
        assertTrue(result.contains("\"articleTopSummaryEn\": \"Summary text\""));
    }

    @Test
    void testExtractJsonFromLlmOutputWithSchema() throws Exception {
        String response = "<think_more>The LLM analyzed the articles...</think_more>\n\n{\n  \"categoryId\": 10,\n  \"articleTopSummaryEn\": \"This is a summary\",\n  \"articleTopSummaryZh\": \"这是摘要\",\n  \"articles\": [\n    {\"title\": \"Test\", \"abstract\": \"Abstract\", \"articleLink\": \"http://example.com\", \"authors\": [\"Author\"], \"titleCn\": \"测试\", \"abstractCn\": \"摘要\", \"background\": \"bg\", \"effects\": \"ef\", \"eventSummary\": \"es\", \"technologyAndInnovation\": \"ti\", \"valueAndImpact\": \"vi\", \"backgroundCn\": \"背景\", \"effectsCn\": \"效果\", \"eventSummaryCn\": \"事件\", \"technologyAndInnovationCn\": \"技术\", \"valueAndImpactCn\": \"价值\"}\n  ]\n}";
        String result = invokeExtract(response);
        assertTrue(result.startsWith("{"));
        assertTrue(result.contains("\"categoryId\": 10"));
        assertTrue(result.contains("\"articles\":"));
    }

    private String invokeExtract(String response) throws Exception {
        Method method = AiTopArticlesOrchestrator.class.getDeclaredMethod("extractJsonFromResponse", String.class);
        method.setAccessible(true);
        AiTopArticlesOrchestrator orchestrator = new AiTopArticlesOrchestrator(
            null, null, null, null, null, 100, 5, 600000, "./logs/llm",
            "deepseek/deepseek-v3.2", 0.1, "deepseek/deepseek-v3.2", 0.4
        );
        return (String) method.invoke(orchestrator, response);
    }

    private String invokeExtractArray(String json) throws Exception {
        Method method = AiTopArticlesOrchestrator.class.getDeclaredMethod("extractArrayFromObject", String.class);
        method.setAccessible(true);
        AiTopArticlesOrchestrator orchestrator = new AiTopArticlesOrchestrator(
            null, null, null, null, null, 100, 5, 600000, "./logs/llm",
            "deepseek/deepseek-v3.2", 0.1, "deepseek/deepseek-v3.2", 0.4
        );
        return (String) method.invoke(orchestrator, json);
    }

    private String loadResource(String location) throws Exception {
        Method method = AiTopArticlesOrchestrator.class.getDeclaredMethod("loadResource", String.class);
        method.setAccessible(true);
        AiTopArticlesOrchestrator orchestrator = new AiTopArticlesOrchestrator(
            null, null, null, null, null, 100, 5, 600000, "./logs/llm",
            "deepseek/deepseek-v3.2", 0.1, "deepseek/deepseek-v3.2", 0.4
        );
        return (String) method.invoke(orchestrator, location);
    }
}
