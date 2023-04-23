package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DealerTest {

    Dealer dealer;

    Table table;

    Player [] players;

    private Integer[] slotToCard;
    private Integer[] cardToSlot;

    @BeforeEach
    void setUp() {

        Properties properties = new Properties();
        properties.put("Rows", "3");
        properties.put("Columns", "4");
        properties.put("FeatureSize", "3");
        properties.put("FeatureCount", "4");
        properties.put("TableDelaySeconds", "0");
        properties.put("PlayerKeys1", "81,87,69,82");
        properties.put("PlayerKeys2", "85,73,79,80");
        properties.put("HumanPlayers", "2");
        properties.put("ComputerPlayers", "2");
        MockLogger logger = new MockLogger();
        Config config = new Config(logger, properties);
        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];
        players = new Player[config.players];

        Env env = new Env(logger, config, new MockUserInterface(), new MockUtil());
        table = new Table(env, slotToCard, cardToSlot);
        dealer = new Dealer(env,table,players);
    }

    @Test
    void removeAllCardsFromTable() {

        // call the method we are testing
        dealer.removeAllCardsFromTableForTesting();

        // check that there are no cards on the table
        assertEquals(0, table.countCards());
    }

    @Test
    void placeCardsOnTable()
    {

        // call the method we are testing
        dealer.placeCardsOnTableForTesting();

        // check that there are no cards on the table
        assertEquals(12, table.countCards());
    }

    static class MockUserInterface implements UserInterface {
        @Override
        public void dispose() {}
        @Override
        public void placeCard(int card, int slot) {}
        @Override
        public void removeCard(int slot) {}
        @Override
        public void setCountdown(long millies, boolean warn) {}
        @Override
        public void setElapsed(long millies) {}
        @Override
        public void setScore(int player, int score) {}
        @Override
        public void setFreeze(int player, long millies) {}
        @Override
        public void placeToken(int player, int slot) {}
        @Override
        public void removeTokens() {}
        @Override
        public void removeTokens(int slot) {}
        @Override
        public void removeToken(int player, int slot) {}
        @Override
        public void announceWinner(int[] players) {}
    };

    static class MockUtil implements Util {
        @Override
        public int[] cardToFeatures(int card) {
            return new int[0];
        }

        @Override
        public int[][] cardsToFeatures(int[] cards) {
            return new int[0][];
        }

        @Override
        public boolean testSet(int[] cards) {
            return false;
        }

        @Override
        public List<int[]> findSets(List<Integer> deck, int count) {
            return null;
        }

        @Override
        public void spin() {}
    }

    static class MockLogger extends Logger {
        protected MockLogger() {
            super("", null);
        }
    }
}