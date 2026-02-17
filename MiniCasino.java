import java.util.Locale;  // Controls locale-specific formatting behavior.
import java.util.Random;  // Generates random numbers for payout sampling.
import java.util.Scanner; // Reads user input from the keyboard.

public class MiniCasino {
    // Possible payout values the slot machine can return per play.
    private static final int[] PAYOUTS = {0, 1, 5, 100};

    // Cost to play one round.
    private static final int COST_PER_PLAY = 1;

    // One play always consumes 10 seconds in this world.
    private static final int SECONDS_PER_PLAY = 10;

    // Requested fixed total play time.
    private static final int TOTAL_PLAY_TIME = 90;

    // Fixed payout probabilities for both machines.
    // Index mapping must match PAYOUTS array: 0, 1, 5, 100.
    // 50%, 30%, 15%, 5%
    private static final double[] FIXED_CHANCES = {0.50, 0.30, 0.15, 0.05};

    /**
     * Program entry point.
     * Sets up the simulation, runs each round, and prints the narrative output.
     */
    public static void main(String[] args) {
        // Force US locale so decimals stay consistent (e.g., dot instead of comma).
        Locale.setDefault(Locale.US);

        // Scanner for user input.
        Scanner scanner = new Scanner(System.in);

        // Random generator used by samplePayout.
        Random random = new Random();

        // Header output.
        System.out.println("=== MINI-CASINO WORLD ===");

        // Ask only for starting money (time and probabilities are fixed per your request).
        int startingMoney = readNonNegativeInt(scanner, "Enter amount of money to start with (Php): ");

        // Use fixed total play time.
        int totalPlayTime = TOTAL_PLAY_TIME;

        // Both machines use the same fixed probability distribution.
        // clone() avoids accidental shared-array mutation.
        double[] machine1Chances = FIXED_CHANCES.clone();
        double[] machine2Chances = FIXED_CHANCES.clone();

        // Maximum plays allowed by time (90/10 = 9 rounds).
        int maxTimeBasedPlays = totalPlayTime / SECONDS_PER_PLAY;

        // Current money during simulation.
        int money = startingMoney;

        // Count how many times each machine has been played.
        // Index 0 = machine 1, index 1 = machine 2.
        int[] timesPlayed = new int[2];

        // Store last payout observed for each machine.
        // -1 means "not played yet".
        int[] lastPayout = {-1, -1};

        // Tracks the machine used in the previous round.
        // -1 means "no machine chosen yet".
        int currentMachine = -1;

        // Start-of-simulation narrative.
        System.out.println("\n=== Simulation Start ===");
        System.out.println("Starting money: Php " + money);
        System.out.println("Total play time: " + totalPlayTime + " seconds (fixed)");
        System.out.println("Fixed chances per machine: [0->50%, 1->30%, 5->15%, 100->5%]");
        System.out.println("Maximum possible plays by time: " + maxTimeBasedPlays);

        // Round counter.
        int round = 0;

        // Continue while there is still time for another play and enough money to pay the cost.
        while (round < maxTimeBasedPlays && money >= COST_PER_PLAY) {
            // Explain the reflex decision from the agent's point of view before choosing.
            String decisionReason = explainReflexDecision(timesPlayed, lastPayout, currentMachine);

            // Reflex agent chooses machine based on immediate, local state.
            int chosenMachine = chooseActionReflex(timesPlayed, lastPayout, currentMachine);

            // Use chances for selected machine.
            double[] chosenChances = (chosenMachine == 0) ? machine1Chances : machine2Chances;

            // Move to next round.
            round++;

            // Report elapsed time so far.
            int timeUsed = round * SECONDS_PER_PLAY;

            // Narrative output for this round.
            System.out.println("\nRound " + round + " (Time used: " + timeUsed + "s)");
            System.out.println("Agent POV: " + decisionReason);
            System.out.println("Agent POV: I bet Php " + COST_PER_PLAY + " on Machine " + machineLabel(chosenMachine) + ".");

            // Pay the cost to play.
            money -= COST_PER_PLAY;
            System.out.println("Agent POV: After paying the bet, my money is now Php " + money + ".");

            // Sample payout according to selected machine's probability distribution.
            int payout = samplePayout(chosenChances, random);

            // Add payout to money.
            money += payout;

            // Narrative output for result.
            System.out.println("Agent POV: Machine " + machineLabel(chosenMachine) + " gave payout Php " + payout + ".");
            System.out.println("Agent POV: " + describeOutcome(payout));
            System.out.println("Agent POV: I now have Php " + money + ".");

            // Update memory used by reflex policy.
            timesPlayed[chosenMachine]++;     // Increase play count for chosen machine.
            lastPayout[chosenMachine] = payout; // Save its latest payout.
            currentMachine = chosenMachine;   // Mark this machine as current for next decision.
        }

        // End-of-simulation summary.
        System.out.println("\n=== Simulation End ===");
        System.out.println("Plays completed: " + round);
        System.out.println("Money at end: Php " + money);
        System.out.println("Net change: Php " + (money - startingMoney));
        System.out.println("Machine 1 plays: " + timesPlayed[0]);
        System.out.println("Machine 2 plays: " + timesPlayed[1]);

        // Print why simulation stopped.
        if (money < COST_PER_PLAY && round < maxTimeBasedPlays) {
            System.out.println("Reason for stop: Not enough money to continue.");
        } else if (round >= maxTimeBasedPlays) {
            System.out.println("Reason for stop: Time limit reached.");
        }

        // Release scanner resource.
        scanner.close();
    }

    /**
     * Reflex-agent decision rule.
     * Behavior:
     * 1) Play machine 1 once if never played.
     * 2) Play machine 2 once if never played.
     * 3) If last payout from current machine is >= 5, stay.
     * 4) Otherwise, switch machines.
     */
    private static int chooseActionReflex(int[] timesPlayed, int[] lastPayout, int currentMachine) {
        // Ensure first machine is explored at least once.
        if (timesPlayed[0] == 0) {
            return 0;
        }

        // Ensure second machine is explored at least once.
        if (timesPlayed[1] == 0) {
            return 1;
        }

        // Fallback when no machine has been marked as current.
        if (currentMachine == -1) {
            return 0;
        }

        // Main reflex condition: if recent outcome is good enough, repeat same machine.
        if (lastPayout[currentMachine] >= 5) {
            return currentMachine;
        }

        // Otherwise switch to the other machine.
        return 1 - currentMachine;
    }

    /**
     * Returns the machine label used in user-facing narrative.
     * index 0 -> A, index 1 -> B
     */
    private static String machineLabel(int machineIndex) {
        return machineIndex == 0 ? "A" : "B";
    }

    /**
     * Explains the reflex condition that caused the next action.
     */
    private static String explainReflexDecision(int[] timesPlayed, int[] lastPayout, int currentMachine) {
        if (timesPlayed[0] == 0) {
            return "I will try Machine A first because I have not tested it yet.";
        }
        if (timesPlayed[1] == 0) {
            return "I will try Machine B now because I have not tested it yet.";
        }
        if (currentMachine == -1) {
            return "I do not have a current machine yet, so I will start with Machine A.";
        }
        if (lastPayout[currentMachine] >= 5) {
            return "My last payout on Machine " + machineLabel(currentMachine) + " was good (>= 5), so I will stay.";
        }
        return "My last payout on Machine " + machineLabel(currentMachine) + " was low (< 5), so I will switch.";
    }

    /**
     * Describes the result of this round from the agent's perspective.
     */
    private static String describeOutcome(int payout) {
        if (payout == 0) {
            return "I lost this round.";
        }
        if (payout == 1) {
            return "I broke even on this round.";
        }
        return "I won Php " + payout + " this round!";
    }

    /**
     * Samples one payout based on the provided probability distribution.
     * Uses cumulative probability and one random value in [0,1).
     */
    private static int samplePayout(double[] chances, Random random) {
        // Random roll between 0.0 (inclusive) and 1.0 (exclusive).
        double roll = random.nextDouble();

        // Running total of probabilities.
        double cumulative = 0.0;

        // Walk through each payout bucket until roll falls inside one bucket.
        for (int i = 0; i < chances.length; i++) {
            cumulative += chances[i];
            if (roll <= cumulative) {
                return PAYOUTS[i];
            }
        }

        // Fallback due to floating-point edge cases; returns largest payout bucket.
        return PAYOUTS[PAYOUTS.length - 1];
    }

    /**
     * Reads a non-negative integer from user input with validation.
     */
    private static int readNonNegativeInt(Scanner scanner, String prompt) {
        // Keep asking until valid input is entered.
        while (true) {
            // Show prompt.
            System.out.print(prompt);

            // Read and trim input line.
            String line = scanner.nextLine().trim();

            try {
                // Parse to integer.
                int value = Integer.parseInt(line);

                // Reject negatives.
                if (value < 0) {
                    System.out.println("Please enter a non-negative integer.");
                    continue;
                }

                // Return valid value.
                return value;
            } catch (NumberFormatException e) {
                // Handle invalid number format.
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }
}
