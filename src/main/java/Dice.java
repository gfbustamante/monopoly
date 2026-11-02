import java.util.Random;

public class Dice {
    private Random random;
    private int[] script;
    private int next;

    public Dice(Random random) {
        this.random = random;
    }

    public Dice(int[] script) {
        this.script = script;
        random = new Random(1);
    }

    public int roll() {
        if (script != null && next < script.length) {
            int value = script[next++];
            if (value >= 1 && value <= 6) { return value; }
        }
        return random.nextInt(6) + 1;
    }
}
