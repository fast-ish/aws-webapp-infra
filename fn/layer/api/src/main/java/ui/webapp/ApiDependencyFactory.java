package ui.webapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ApiDependencyFactory {

  private ApiDependencyFactory() {}

  public static List<String> profanity() {
    final ArrayList<String> p;

    try (var inputStream = ApiDependencyFactory.class.getClassLoader().getResourceAsStream("profanity/en.txt");
         var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))) {
      p = new ArrayList<>(reader.lines().toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try (var inputStream = ApiDependencyFactory.class.getClassLoader().getResourceAsStream("profanity/es.txt");
         var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))) {
      p.addAll(reader.lines().toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return p;
  }
}
