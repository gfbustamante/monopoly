package example;

import org.junit.jupiter.api.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MainTest {

  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void test_getGreeting_correctString() {
    Assertions.assertEquals("Hello World", Main.getGreeting());
  }
}