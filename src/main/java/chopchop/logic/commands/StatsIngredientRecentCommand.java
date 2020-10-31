package chopchop.logic.commands;

import java.util.stream.Collectors;

import chopchop.commons.util.Pair;
import chopchop.logic.commands.exceptions.CommandException;
import chopchop.logic.history.HistoryManager;
import chopchop.model.Model;

public class StatsIngredientRecentCommand extends Command {
    private static final int N_MOST_RECENT = 10;

    /**
     * Executes the command and returns the result message.
     *
     * @param model          {@code Model} which the command should operate on.
     * @param historyManager {@code History} which the command should record to.
     * @return feedback message of the operation result for display
     * @throws CommandException If an error occurs during command execution.
     */
    @Override
    public CommandResult execute(Model model, HistoryManager historyManager) throws CommandException {
        try {
            var output = model.getRecentlyUsedIngredients(N_MOST_RECENT);
            var msgOutput = output.stream()
                .map(x -> new Pair<>(x.getName(), x.getPrintableDate()))
                .collect(Collectors.toList());
            return CommandResult.statsMessage(msgOutput, "Here is the list of ingredients recently used");
        } catch (Exception e) {
            return CommandResult.message("Unable to generate ingredient recently used");
        }
    }

    @Override
    public String toString() {
        return "StatsIngredientRecentCommand";
    }

    public static String getCommandString() {
        return "stats ingredient recent";
    }

    public static String getCommandHelp() {
        return "Shows the ingredients that were recently used by recipes that were made";
    }
}