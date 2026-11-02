import java.util.Random;

public class Deck {
    private int[] cards;
    private int count;
    private int jailCardId;
    private boolean[] inDeck;

    public Deck(Random random, int jailCardId) {
        cards = new int[16];
        inDeck = new boolean[16];
        this.jailCardId = jailCardId;
        count = 16;
        for (int index = 0; index < cards.length; index++) {
            cards[index] = index;
            inDeck[index] = true;
        }
        for (int index = cards.length - 1; index > 0; index--) {
            int other = random.nextInt(index + 1);
            int saved = cards[index];
            cards[index] = cards[other];
            cards[other] = saved;
        }
    }

    public int draw() {
        if (count == 0) {
            return -1;
        }
        int card = cards[0];
        for (int index = 1; index < count; index++) {
            cards[index - 1] = cards[index];
        }
        count--;
        inDeck[card] = false;
        if (card == jailCardId) {
            return jailCardId;
        }
        return card;
    }

    public void returnCard(int card) {
        if (card >= 0 && card < cards.length && !inDeck[card]) {
            cards[count] = card;
            count++;
            inDeck[card] = true;
        }
    }

    public int size() { return count; }
}
