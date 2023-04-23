package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    private Thread[] playersThreads;

    private List<Integer> slotsToRemove;

    public static int SECOND = 1000;

    public static int MILISECOND = 1;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.playersThreads = new Thread[players.length];
        this.slotsToRemove = new LinkedList<Integer>();
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        createAndRunPlayersThreads();
        while (!shouldFinish()) {
            placeCardsOnTable();
            updateTimerDisplay(true);
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        if (env.util.findSets(deck, 1).size() == 0)
            announceWinners();
        closeAllPlayersThreads();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did
     * not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }


    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        this.terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        synchronized (table) {
            if (!slotsToRemove.isEmpty()) {
                for (Integer slot : slotsToRemove)
                    removeSingleCard(slot);
                this.slotsToRemove.clear();
                if (deck.isEmpty()) {
                    List<Integer> cardsOnTable = table.getCardsOnTable();
                    if (env.util.findSets(cardsOnTable, 1).size() == 0)
                        this.terminate = true;
                }
                updateTimerDisplay(true);
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        synchronized (table) {
            boolean placeCard = false;
            while (table.countCards() < env.config.tableSize && deck.size() != 0) {
                placeCard = true;
                Integer nextEmptySlot = table.getNextEmptySlot();
                if (nextEmptySlot == null)
                    throw new IllegalArgumentException("No empty Slot Available");
                Random x = new Random();
                int randomIndexCard = x.nextInt(deck.size());
                int cardToPut = deck.get(randomIndexCard);
                table.placeCard(cardToPut, nextEmptySlot);
                deck.remove(randomIndexCard);
            }
            if (placeCard && env.config.hints) {
                table.hints();
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some
     * purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        try {
            Integer claimedPlayer = null;
            synchronized (table.waitingPlayers) {
                long timeCountDown = (reshuffleTime - System.currentTimeMillis()) / SECOND;
                timeCountDown = timeCountDown * SECOND;
                if (timeCountDown > env.config.turnTimeoutWarningMillis)
                    claimedPlayer = table.waitingPlayers.poll(SECOND, TimeUnit.MILLISECONDS);
                else
                    claimedPlayer = table.waitingPlayers.poll(MILISECOND, TimeUnit.MILLISECONDS);
            }

            if (claimedPlayer != null) {
                List<Integer> claimedSetList = table.getCardsWithTokensOfPlayer(claimedPlayer);
                int[] claimedSet = convertListToArray(claimedSetList);
                if (claimedSetList.size() == env.config.featureSize) // All tokens are still on table.
                {
                    players[claimedPlayer].setResultOfChecking(1);
                    if (env.util.testSet(claimedSet)) {

                        for (int i = 0; i < claimedSet.length; i++)
                            slotsToRemove.add(table.cardToSlot[claimedSet[i]]);
                    } else {
                        players[claimedPlayer].setResultOfChecking(-1);
                    }
                }
                synchronized (players[claimedPlayer]) {
                    players[claimedPlayer].notifyAll();
                }
            }
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        if (reset)
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        long timeCountDown = (reshuffleTime - System.currentTimeMillis());
        if (timeCountDown > env.config.turnTimeoutWarningMillis)
            env.ui.setCountdown(timeCountDown, false);
        else {
            if (timeCountDown < 0)
                timeCountDown = 0;
            env.ui.setCountdown(timeCountDown, true);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        synchronized (table) {
            for (int i = 0; i < env.config.tableSize; i++) {
                if (table.slotToCard[i] != null) {
                    deck.add(table.slotToCard[i]);
                    removeSingleCard(i);
                }
            }
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int maxScore = 0;
        for (Player player : players) {
            if (maxScore < player.score())
                maxScore = player.score();
        }
        List<Integer> winnersList = new LinkedList<Integer>();
        for (Player player : players) {
            if (player.score() == maxScore)
                winnersList.add(player.id);
        }
        int[] winners = convertListToArray(winnersList);
        env.ui.announceWinner(winners);
    }

    private void createAndRunPlayersThreads() {
        for (int i = 0; i < playersThreads.length; i++) {
            playersThreads[i] = new Thread(this.players[i], "Player number " + i);
            synchronized (players[i]) {
                playersThreads[i].start();
                try{players[i].wait();} catch (InterruptedException ignored) {}

            }
        }
    }

    public void closeAllPlayersThreads(){
        for (int i = players.length - 1; i >= 0; i--) {
            players[i].terminate();
            playersThreads[i].interrupt();
            try{
                playersThreads[i].join();
            } catch(InterruptedException e) {}

        }
    }

    private void removeSingleCard(Integer slotToRemove) {
        List<Integer> playersWithToken = new LinkedList<Integer>(table.tokens.get(slotToRemove));
        for (int playerId : playersWithToken)
            players[playerId].removeToken(slotToRemove);
        table.removeCard(slotToRemove);
    }

    public static int[] convertListToArray(List<Integer> ls) {
        int[] output = new int[ls.size()];
        int i = 0;
        for (Integer num : ls) {
            output[i] = num;
            i++;
        }
        return output;
    }

    public void removeAllCardsFromTableForTesting() {
        removeAllCardsFromTable();
    }

    public void placeCardsOnTableForTesting() {
        placeCardsOnTable();
    }

}