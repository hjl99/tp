package chopchop.logic.commands;

import static java.util.Objects.requireNonNull;

import chopchop.logic.commands.exceptions.CommandException;
import chopchop.logic.history.HistoryManager;
import chopchop.logic.parser.ItemReference;
import chopchop.model.Model;
import chopchop.ui.DisplayNavigator;

/**
 * Displays a recipe identified by the index number or its name from the recipe book.
 */
public class ViewRecipeCommand extends Command {

    private final ItemReference item;

    /**
     * Constructs a {@code ViewRecipeCommand} from the given recipe item.
     * @param item
     */
    public ViewRecipeCommand(ItemReference item) {
        requireNonNull(item);
        this.item = item;
    }

    @Override
    public CommandResult execute(Model model, HistoryManager historyManager) throws CommandException {

        var recipe = resolveRecipeReference(this.item, model);
        if (recipe.isError()) {
            return CommandResult.error(recipe.getError());
        }


        if (DisplayNavigator.hasDisplayController()) {
            DisplayNavigator.loadRecipeDisplay(recipe.getValue());
        }

        return CommandResult.message("Displaying recipe '%s'", recipe.getValue().getName());
    }

    @Override
    public String toString() {
        return String.format("ViewRecipeCommand(%s)", this.item);
    }

    @Override
    public boolean equals(Object other) {
        return other == this
                || (other instanceof ViewRecipeCommand
                && this.item.equals(((ViewRecipeCommand) other).item));
    }

    public static String getCommandString() {
        return "view";
    }

    public static String getCommandHelp() {
        return "Views the given recipe by opening its detailed view";
    }
}