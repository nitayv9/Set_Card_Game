package bguspl.set.ex;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    private BlockingQueue<Integer> actions;

    private int resultOfChecking;

    public static int SECOND = 1000;


    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.actions = new LinkedBlockingQueue<Integer>(env.config.featureSize);
        this.resultOfChecking = 0;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();
        synchronized (this) {this.notifyAll();} // For starting all the threads gracefully.
        while (!terminate) {
            try {
                Integer nextAction = actions.take();
                if (table.getSlotsOfTokensOfPlayer(id).contains(nextAction)) {
                    synchronized (table) {
                        this.removeToken(nextAction);
                    }
                }
                else {
                    boolean wasAdded = false;
                    synchronized (table) {
                        if (table.getNumbersOfTokensOfPlayer(id) < env.config.featureSize && table.slotToCard[nextAction] != null) {
                            this.addToken(nextAction);
                            wasAdded = true;
                        }
                    }
                        if (table.getNumbersOfTokensOfPlayer(id) == env.config.featureSize && wasAdded) {
                            synchronized (this) {
                                table.waitingPlayers.add(this.id);
                                try {
                                    wait();
                                } catch (InterruptedException ignored) {
                                }
                                if (resultOfChecking== -1)
                                    penalty();
                                if (resultOfChecking == 1)
                                    point();
                            }
                        }
                    }
            }
            catch (InterruptedException ignored) {}
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                Random x = new Random();
                int randomSlot = x.nextInt(env.config.tableSize);
                try {
                    actions.put(randomSlot);
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate = true;
        actions.clear(); // Wake up the AI Thread
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     * @post - the slot should be added to the player's actions queue, if there is a space in the queue.
     */
    public void keyPressed(int slot) {
        if(actions.size() < env.config.featureSize)
        {
            try{
                actions.put(slot);}
            catch (InterruptedException ignored) {}
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        score++;
        env.ui.setScore(id, score);
        freezePlayer (env.config.pointFreezeMillis);
        actions.clear();
        resultOfChecking = 0;
    }

    /**
     * Penalize a player and perform other related actions.
     * @post - the player's score should not be changed.
     * @post - the player's action queue should be empty.
     * @post - the player's result of checking should be restarted to 0.
     */
    public void penalty() {
        freezePlayer (env.config.penaltyFreezeMillis);
        actions.clear();
        resultOfChecking = 0;
    }

    public int score() {
        return score;
    }

    public void addToken(Integer slot){
        if(!(env.config.tableSize + 1 > slot && slot > -1))
            throw new IllegalArgumentException("Slot is out of table size");
        table.placeToken(this.id,slot);
    }

    public void removeToken(Integer slot)
    {
        if(!(env.config.tableSize + 1 > slot && slot > -1))
            throw new IllegalArgumentException("Slot is out of table size");
        table.removeToken(this.id,slot);
    }

    public void setResultOfChecking(int setTo){this.resultOfChecking = setTo;}

    public void freezePlayer (long timeToFreeze)
    {
        long remainTimePenalty = timeToFreeze%SECOND;
        env.ui.setFreeze(this.id,timeToFreeze);
        try { Thread.sleep(remainTimePenalty); } catch (InterruptedException ignoredException) {}

        for(int i=0; i < timeToFreeze/SECOND && !terminate;i++)
        {
            env.ui.setFreeze(this.id,timeToFreeze - (i * SECOND));
            try { Thread.sleep(SECOND); } catch (InterruptedException ignoredException) {}
        }

        env.ui.setFreeze(this.id,0);
    }

    public int getResultOfChecking()
    {
        return this.resultOfChecking;
    }

    public BlockingQueue<Integer> getActions ()
    {
        return this.actions;
    }
}
