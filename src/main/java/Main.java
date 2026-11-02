import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] arguments) {
        boolean automated = arguments.length > 0 && arguments[0].equals("--auto");
        long seed = 9;
        int limit = 10000;
        if (automated && arguments.length > 1) { seed = Long.parseLong(arguments[1]); }
        if (automated && arguments.length > 2) { limit = Integer.parseInt(arguments[2]); }
        if (limit < 1) {
            System.out.println("Turn limit must be positive.");
            return;
        }
        Scanner scanner = new Scanner(System.in);
        ConsoleInput input = new ConsoleInput(scanner, automated, automated);
        input.say("MONOPOLY: Java teacher reference. All money is whole dollars.");
        int count = automated ? 4 : input.choose("Number of players", 2, 8, 2);
        String[] names = new String[count];
        for (int index = 0; index < count; index++) {
            names[index] = automated ? "Bot " + (index + 1) : input.text("Player " + (index + 1) + " name:");
            if (names[index].length() == 0) { names[index] = "Player " + (index + 1); }
        }
        Random random = automated ? new Random(seed) : new Random();
        MonopolyGame game = new MonopolyGame(names, input, new Dice(random), random);
        game.play(automated ? limit : 0);
        System.out.println(game.result());
    }
}
