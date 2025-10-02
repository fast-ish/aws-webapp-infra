package ui.webapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class JsonUtil {
  @SneakyThrows
  public static String toJson(Object object, ObjectMapper mapper) {
    return mapper.writeValueAsString(object);
  }

  @SneakyThrows
  public static <T> T fromJson(ObjectMapper mapper, String json, Class<T> clazz) {
    return mapper.readValue(json, clazz);
  }

  @SneakyThrows
  public static <T> T fromJson(ObjectMapper mapper, String json, TypeReference<T> clazz) {
    return mapper.readValue(json, clazz);
  }
}
