package com.github.kongchen.swagger.docgen.reader;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import org.apache.maven.plugin.logging.Log;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class JaxrsReaderTest {
  @Mock
  private Log log;

  private JaxrsReader reader;

  @BeforeMethod
  public void setup() {
    Swagger swagger = new Swagger();
    reader = new JaxrsReader(swagger, log);
  }

  @Test
  public void ignoreClassIfNoApiAnnotation() {
    Swagger result = reader.read(NotAnnotatedApi.class);

    assertEmptySwaggerResponse(result);
  }

  @Test
  public void ignoreApiIfHiddenAttributeIsTrue() {
    Swagger result = reader.read(HiddenApi.class);

    assertEmptySwaggerResponse(result);
  }

  @Test
  public void includeApiIfHiddenParameterIsTrueAndApiHiddenAttributeIsTrue() {
    Swagger result = reader.read(HiddenApi.class, "", null, true, new String[0], new String[0], new HashMap<String, Tag>(), new ArrayList<Parameter>());

    assertNotNull(result, "No Swagger object created");
    assertFalse(result.getTags().isEmpty(), "Should contain api tags");
    assertFalse(result.getPaths().isEmpty(), "Should contain operation paths");
  }

  @Test
  public void discoverApiOperation() {
    Tag expectedTag = new Tag();
    expectedTag.name("atag");
    Swagger result = reader.read(AnApi.class);

    assertSwaggerResponseContents(expectedTag, result);
  }

  @Test
  public void createNewSwaggerInstanceIfNoneProvided() {
    JaxrsReader nullReader = new JaxrsReader(null, log);
    Tag expectedTag = new Tag();
    expectedTag.name("atag");
    Swagger result = nullReader.read(AnApi.class);

    assertSwaggerResponseContents(expectedTag, result);
  }

  private void assertEmptySwaggerResponse(Swagger result) {
    assertNotNull(result, "No Swagger object created");
    assertNull(result.getTags(), "Should not have any tags");
    assertNull(result.getPaths(), "Should not have any paths");
  }

  private void assertSwaggerResponseContents(Tag expectedTag, Swagger result) {
    assertNotNull(result, "No Swagger object created");
    assertFalse(result.getTags().isEmpty(), "Should contain api tags");
    assertTrue(result.getTags().contains(expectedTag), "Expected tag missing");
    assertFalse(result.getPaths().isEmpty(), "Should contain operation paths");
    assertTrue(result.getPaths().containsKey("/apath"), "Path missing from paths map");
    io.swagger.models.Path path = result.getPaths().get("/apath");
    assertFalse(path.getOperations().isEmpty(), "Should be a get operation");
  }

  @Test
  public void testDiscoverResponseDtoBySingleReturnValue() throws Exception {
    Swagger result = reader.read(ApiWithSingleResponse.class);
    assertContainsModel("ResponseDto", result);
  }

  @Test
  public void testDiscoverResponseDtoAsElementOfList() throws Exception {
    Swagger result = reader.read(ApiWithResponseList.class);
    assertContainsModel("ResponseDto", result);
  }

  private void assertContainsModel(String modelName, Swagger result) {
    Model responseDto = result.getDefinitions().get(modelName);
    Assert.assertNotNull(responseDto);
  }

  @Api(tags = "atag")
  @Path("/apath")
  static class AnApi {
    @ApiOperation(value = "Get a model.")
    @GET
    public Response getOperation() {
      return Response.ok().build();
    }
  }

  @Api(hidden = true, tags = "atag")
  @Path("/hidden/path")
  static class HiddenApi {
    @ApiOperation(value = "Get a model.")
    @GET
    public Response getOperation() {
      return Response.ok().build();
    }
  }

  @Path("/apath")
  static class NotAnnotatedApi {
  }

  @ApiModel
  static class ResponseDto {

  }

  static class ResponseDtoList extends ArrayList<ResponseDto> {

  }

  @Api
  @Path("/response")
  static class ApiWithSingleResponse {
    @ApiOperation("Get a response")
    public ResponseDto getResponse() {
      return new ResponseDto();
    }
  }

  @Api
  @Path("/responselist")
  static class ApiWithResponseList { // fails when using response=ResponseDto[].class too
    @ApiOperation(value = "Get a list of responses", response = ResponseDtoList.class)
    public List<ResponseDto> getResponses() {
      return new ArrayList<ResponseDto>();
    }
  }
}
