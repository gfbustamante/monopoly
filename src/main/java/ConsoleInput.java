import java.util.Scanner;

public class ConsoleInput {
    private Scanner scanner;
    private boolean automated;
    private boolean closed;
    private boolean quiet;

    public ConsoleInput(Scanner scanner, boolean automated, boolean quiet) {
        this.scanner = scanner;
        this.automated = automated;
        this.quiet = quiet;
    }

    public boolean isAutomated() { return automated; }
    public boolean isClosed() { return closed; }

    public void say(String text) {
        if (!quiet) { System.out.println(text); }
    }

    public String text(String prompt) {
        say(prompt);
        if (!scanner.hasNextLine()) {
            closed = true;
            return "";
        }
        return scanner.nextLine().trim();
    }

    public int choose(String prompt, int minimum, int maximum, int automatic) {
        if (automated || closed) { return automatic; }
        while (!closed) {
            String value = text(prompt + " [" + minimum + ".." + maximum + "]");
            if (closed) { return automatic; }
            if (value.length() > 0 && value.length() < 10) {
                boolean digits = true;
                for (int index = 0; index < value.length(); index++) {
                    if (value.charAt(index) < '0' || value.charAt(index) > '9') {
                        digits = false;
                    }
                }
                if (digits) {
                    int number = Integer.parseInt(value);
                    if (number >= minimum && number <= maximum) { return number; }
                }
            }
            say("Enter a whole number in the displayed range.");
        }
        return automatic;
    }
}
