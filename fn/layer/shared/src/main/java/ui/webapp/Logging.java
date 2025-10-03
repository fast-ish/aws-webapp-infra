package ui.webapp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Logging {
  private static final Logger log = LogManager.getLogger(Logging.class);

  public Logging() {}

  public static Logger handler() {
    return log;
  }
}
