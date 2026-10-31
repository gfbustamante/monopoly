package example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Main class is the entry point of the Java program.
 */
public class Main {
  private static final Logger logger = LogManager.getLogger(Main.class);

  public static void main(String[] args) {
    logger.info("Starting main method");
    System.out.println(getGreeting());
  }

  /**
   * Returns a greeting message.
   *
   * @return the greeting message
   */
  public static String getGreeting() {
    return "Hello World";
  }
}