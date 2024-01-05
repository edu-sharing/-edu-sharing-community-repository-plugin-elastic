package org.edu_sharing.elasticsearch.elasticsearch.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
// TODO

//    @Mock
//    private  co.elastic.clients.elasticsearch.ElasticsearchClient elasticsearchClient;
//    private IndexService underTest;
//
//    private static String indentJson(String json) {
//        JsonParser parser = new JsonParser();
//        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
//        JsonElement el = parser.parse(json);
//        return gson.toJson(el);
//    }
//
//    @BeforeEach
//    void setUp() {
//        underTest = Mockito.spy(new IndexService(new IndexNameProvider(), elasticsearchClient));
//    }
//
//    @Test
//    void getWorkspaceMappingsTest() throws Exception {
//        TypeMapping mappings = underTest.getWorkspaceMappings(new TypeMapping.Builder()).build();
//        String actual = indentJson(JsonpUtils.toJsonString(mappings, new JacksonJsonpMapper()));
//
//        String expected = StreamUtils.copyToString(getClass().getClassLoader().getResource("getWorkspaceMappingsTest.json").openStream(), StandardCharsets.UTF_8);
//        assertEquals(expected, actual);
//    }
//    @Test
//    void getWorkspaceIndexSettingsTest() throws Exception {
//        IndexSettings settings = underTest.getWorkspaceIndexSettings(new IndexSettings.Builder()).build();
//        String actual = indentJson(JsonpUtils.toJsonString(settings, new JacksonJsonpMapper()));
//        String expected = StreamUtils.copyToString(getClass().getClassLoader().getResource("getWorkspaceIndexSettingsTest.json").openStream(), StandardCharsets.UTF_8);
//        assertEquals(expected, actual);
//    }
}