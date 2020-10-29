// AutoCompleter.java

package chopchop.logic.autocomplete;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import chopchop.commons.core.LogsCenter;
import chopchop.commons.util.StringView;
import chopchop.commons.util.Strings;
import chopchop.logic.parser.ArgName;
import chopchop.logic.parser.CommandArguments;
import chopchop.logic.parser.CommandParser;
import chopchop.logic.parser.commands.CommandTarget;
import chopchop.model.Entry;
import chopchop.model.Model;

public class AutoCompleter {

    private static final Logger logger = LogsCenter.getLogger(AutoCompleter.class);

    private int lastCompletionIndex = 0;
    private List<String> lastViableCompletions = null;

    /**
     * Resets the internal state of the completer, namely the last-provided completion. When
     * the user types into the text field, we should restart the completion cycling.
     */
    public void resetCompletionState() {
        this.lastCompletionIndex = 0;
        this.lastViableCompletions = null;
    }

    /**
     * Computes the completion for the given user input. If there is no completion
     * available, the string is returned as-is.
     *
     * @param parser the command parser
     * @param model  the model
     * @param orig   the input
     * @return       the auto-completed input
     */
    public String getCompletionForInput(CommandParser parser, Model model, String orig) {

        // can't read your mind.
        if (orig.isEmpty()) {
            return orig;
        }

        // first, try to parse the arguments. this doesn't run the command-specific parser,
        // just the one that splits arguments.
        var res = parser.parseArgs(orig);

        if (res.isError()) {
            return orig;
        }

        var statsRecipeKinds = List.of(Strings.STATS_KIND_TOP, Strings.STATS_KIND_MADE,
            Strings.STATS_KIND_CLEAR, Strings.STATS_KIND_RECENT);

        var statsIngredientKinds = List.of(Strings.STATS_KIND_USED, Strings.STATS_KIND_CLEAR,
            Strings.STATS_KIND_RECENT);


        var args = res.getValue();
        var req = getRequiredCompletion(args, orig);
        logger.info(String.format("completing: %s", req));


        switch (req) {

        case COMMAND_NAME:
            return completeCommand(args, orig, /* nested: */ false);

        case TARGET_NAME:
            return completeTarget(args, orig, /* nested: */ false);

        case NESTED_COMMAND_NAME:
            return completeCommand(args, orig, /* nested: */ true);

        case NESTED_TARGET_NAME:
            return completeTarget(args, orig, /* nested: */ true);

        case ARGUMENT_NAME:
            return completeArgument(args, orig);

        case RECIPE_NAME: // fallthrough
        case RECIPE_NAME_IN_ARG:
            return completeRecipe(req, model, args, orig);

        case INGREDIENT_NAME: // fallthrough
        case INGREDIENT_NAME_IN_ARG:
            return completeIngredient(req, model, args, orig);

        case COMPONENT_NAME:
            return completeArgComponent(args, orig);

        case TAG_NAME:
            return completeTag(model, args, orig);

        case STATS_RECIPE_KIND_NAME:
            return completeStatsKind(args, orig, statsRecipeKinds, /* nested: */ false);

        case STATS_INGREDIENT_KIND_NAME:
            return completeStatsKind(args, orig, statsIngredientKinds, /* nested: */ false);

        case NESTED_STATS_RECIPE_KIND_NAME:
            return completeStatsKind(args, orig, statsRecipeKinds, /* nested: */ true);

        case NESTED_STATS_INGREDIENT_KIND_NAME:
            return completeStatsKind(args, orig, statsIngredientKinds, /* nested: */ true);

        case NONE: // fallthrough
        default:
            return orig;
        }
    }





    /**
     * Returns a completion for the command only.
     */
    private String completeCommand(CommandArguments args, String orig, boolean nested) {

        var partial = nested
            ? args.getFirstWordFromRemaining()
            : args.getCommand();

        var valids = new ArrayList<String>();
        for (var cmd : Strings.COMMAND_NAMES) {
            if (cmd.startsWith(partial)) {
                valids.add(cmd);
            }
        }

        valids.sort((a, b) -> a.length() - b.length());

        if (this.lastViableCompletions == null) {
            this.lastViableCompletions = new ArrayList<String>(valids);
        }

        if (this.lastViableCompletions.isEmpty()) {
            return orig;
        } else {

            // this should always hold, because:
            // (a) we always perform the modulo at the end
            // (b) the list of viableCompletions should not change as long as the
            //     internal state is not reset
            // (c) if 'partial' changed due to user input, then we are supposed to be reset.

            assert this.lastCompletionIndex < this.lastViableCompletions.size();

            var completion = this.lastViableCompletions.get(this.lastCompletionIndex);
            this.lastCompletionIndex = (this.lastCompletionIndex + 1) % this.lastViableCompletions.size();

            if (nested) {

                // now this is a little complicated; split the input into two pieces; the first being
                // everything occurring before "partial" (this is the only part we want)
                var idx = orig.lastIndexOf(partial);
                var prefix = orig.substring(0, idx);

                return prefix + completion + " ";
            } else {
                // if we're not nested, then we're guaranteed that the command must happen at the beginning of
                // the input, so we can just return the completion as-is.
                return completion + " ";
            }
        }
    }

    /**
     * Returns a completion for the target only.
     */
    private String completeTarget(CommandArguments args, String orig, boolean nested) {

        String partial = "";
        if (nested) {
            var words = new StringView(args.getRemaining()).words();
            if (words.size() < 2) {
                return orig;
            }

            partial = words.get(1);

        } else {
            partial = args.getFirstWordFromRemaining();
        }

        var valids = new ArrayList<String>();
        for (var tgt : CommandTarget.values()) {
            if (tgt.toString().startsWith(partial)) {

                // only suggest 'recommendation' for the 'list' command.
                if (!tgt.equals(CommandTarget.RECOMMENDATION)
                    || args.getCommand().equals(Strings.COMMAND_LIST)) {

                    valids.add(tgt.toString());
                }
            }
        }

        valids.sort((a, b) -> a.length() - b.length());

        if (this.lastViableCompletions == null) {
            this.lastViableCompletions = new ArrayList<String>(valids);
        }


        var allExceptLast = orig.stripTrailing().substring(0,
            orig.stripTrailing().length() - partial.length());

        if (this.lastViableCompletions.isEmpty()) {
            return orig;
        } else {

            // see the note in completeCommand ('this should always hold, because...')
            assert this.lastCompletionIndex < this.lastViableCompletions.size();

            var completion = this.lastViableCompletions.get(this.lastCompletionIndex);
            this.lastCompletionIndex = (this.lastCompletionIndex + 1) % this.lastViableCompletions.size();

            return allExceptLast + completion + " ";
        }
    }

    private String completeTag(Model model, CommandArguments args, String orig) {
        assert args.getAllArguments().size() > 0;

        var lastArg = args.getAllArguments().get(args.getAllArguments().size() - 1);
        var partial = lastArg.snd();

        return CommandTarget.of(args.getFirstWordFromRemaining())
            .flatMap(target -> {
                List<String> tags = List.of();

                if (target == CommandTarget.RECIPE) {
                    tags = getAllRecipeTags(model);
                } else if (target == CommandTarget.INGREDIENT) {
                    tags = getAllIngredientTags(model);
                } else {
                    return Optional.empty();
                }

                return tryCompletionUsing(tags, orig, partial);
            })
            .orElse(orig);
    }


    /**
     * Returns a completion for {@code <x>} in {@code stats (ingredient|recipe) <x>}.
     * Since each "kind" is uniquely prefixed (T, U, M, C, R), we don't have to care about cycling.
     */
    private String completeStatsKind(CommandArguments args, String orig, List<String> valids, boolean nested) {

        var xs = new StringView(args.getRemaining()).words();
        if ((nested && xs.size() < 3) || (!nested && xs.size() < 2)) {
            return orig;
        }

        var partial = nested
            ? xs.get(2)
            : xs.get(1);

        return tryCompletionUsing(valids, orig, partial)
            .orElse(orig);

        // assert this.lastCompletionIndex < this.lastViableCompletions.size();

        //     // var completion = this.lastViableCompletions.get(this.lastCompletionIndex);
        //     // this.lastCompletionIndex = (this.lastCompletionIndex + 1) % this.lastViableCompletions.size();

        // if (nested) {

        //     // now this is a little complicated; split the input into two pieces; the first being
        //     // everything occurring before "partial" (this is the only part we want)
        //     var idx = orig.lastIndexOf(partial);
        //     var prefix = orig.substring(0, idx);

        //     return prefix + completion + " ";
        // } else {
        //     // if we're not nested, then we're guaranteed that the command must happen at the beginning of
        //     // the input, so we can just return the completion as-is.
        //     return completion + " ";
        // }
    }





    /**
     * Returns a completion for the argument name only.
     */
    private String completeArgument(CommandArguments args, String orig) {
        assert args.getAllArguments().size() > 0;

        var lastArg = args.getAllArguments().get(args.getAllArguments().size() - 1);
        var partial = lastArg.fst().name();

        // we could complete all arguments for you, but we can do something a little smarter
        // by looking at the current command+target combo to get the valid arguments.

        var cmd = args.getCommand();
        var tgt = args.getFirstWordFromRemaining();

        var validArguments = new ArrayList<ArgName>();

        if (cmd.equals(Strings.COMMAND_ADD)) {
            if (tgt.equals(CommandTarget.RECIPE.toString())) {
                validArguments.add(Strings.ARG_INGREDIENT);
                validArguments.add(Strings.ARG_QUANTITY);
                validArguments.add(Strings.ARG_STEP);
                validArguments.add(Strings.ARG_TAG);

            } else if (tgt.equals(CommandTarget.INGREDIENT.toString())) {

                validArguments.add(Strings.ARG_QUANTITY);
                validArguments.add(Strings.ARG_EXPIRY);
                validArguments.add(Strings.ARG_TAG);
            }
        } else if (cmd.equals(Strings.COMMAND_EDIT)) {
            if (tgt.equals(CommandTarget.RECIPE.toString())) {
                return completeEditRecipeArguments(args, partial, orig);
            }
        } else if (cmd.equals(Strings.COMMAND_DELETE)) {
            if (tgt.equals(CommandTarget.INGREDIENT.toString())) {
                validArguments.add(Strings.ARG_QUANTITY);
            }
        } else if (cmd.equals(Strings.COMMAND_FILTER)) {
            if (tgt.equals(CommandTarget.RECIPE.toString())) {

                validArguments.add(Strings.ARG_TAG);
                validArguments.add(Strings.ARG_INGREDIENT);

            } else if (tgt.equals(CommandTarget.INGREDIENT.toString())) {

                validArguments.add(Strings.ARG_TAG);
                validArguments.add(Strings.ARG_EXPIRY);
            }
        }

        return tryCompletionUsing(getArgNames(validArguments), orig, partial)
            .orElse(orig);
    }


    private List<String> getValidEditOps(String arg) {
        if (Strings.ARG_INGREDIENT.nameEquals(arg) || Strings.ARG_STEP.nameEquals(arg)) {
            return List.of("add", "edit", "delete");
        } else if (Strings.ARG_TAG.nameEquals(arg)) {
            return List.of("add", "delete");
        } else {
            return List.of();
        }
    }

    private String completeArgComponent(CommandArguments args, String orig) {
        assert args.getAllArguments().size() > 0;

        var last = args.getAllArguments().get(args.getAllArguments().size() - 1);
        var comps = last.fst().getComponents();
        var argname = last.fst();

        // for now, there's only 2 components max, and the last component is always the index.
        if (comps.isEmpty() || comps.size() > 1) {
            return orig;
        }

        var partial = comps.get(comps.size() - 1);

        var valids = getValidEditOps(argname.name());
        return tryCompletionUsing(valids, orig, partial, /* appending: */ "")
            .map(comp -> {
                if (comp.endsWith(":")) {
                    return comp;
                }

                if (argname.nameEquals(Strings.ARG_STEP)) {
                    if (comp.endsWith("edit") || comp.endsWith("delete")) {
                        return comp + ":";
                    }
                }

                return comp + " ";

            })
            .orElse(orig);
    }

    private String completeEditRecipeArguments(CommandArguments args, String partial, String orig) {

        // oof.
        var opt = tryCompletionUsing(getArgNames(Strings.ARG_NAME, Strings.ARG_QUANTITY), orig, partial);
        if (opt.isPresent()) {
            return opt.get();
        }

        return tryCompletionUsing(getArgNames(Strings.ARG_TAG, Strings.ARG_INGREDIENT, Strings.ARG_STEP),
            orig, partial, /* appending: */ ":")
        .orElse(orig);
    }





    private <T extends Entry> Optional<String> completeNamedItem(RequiredCompletion req, CommandArguments args,
        String orig, List<T> entries) {

        var words = new StringView(orig).words();
        assert !words.isEmpty();

        String partial = "";
        if (req == RequiredCompletion.RECIPE_NAME_IN_ARG || req == RequiredCompletion.INGREDIENT_NAME_IN_ARG) {

            // get the last argument.
            var arglist = args.getAllArguments();
            if (arglist.isEmpty()) {
                return Optional.empty();
            }

            var last = arglist.get(arglist.size() - 1);
            partial = last.snd();

        } else {

            if (commandRequiresTarget(args.getCommand())) {
                var split = new StringView(args.getRemaining()).bisect(' ');
                if (split.snd().isEmpty()) {
                    return Optional.empty();
                }

                partial = split.snd().toString();
            } else {
                partial = args.getRemaining();
            }
        }

        // the entire command string *except* the partial item name.
        var allExceptLast = orig.stripTrailing().substring(0,
            orig.stripTrailing().length() - partial.length());

        // make a copy of the list, then sort by name length.
        var sortedList = new ArrayList<>(entries);
        sortedList.sort((a, b) -> {
            return a.getName().length() - b.getName().length();
        });

        if (this.lastViableCompletions == null) {
            this.lastViableCompletions = new ArrayList<String>();

            for (var entry : sortedList) {
                var name = entry.getName();

                if (name.toLowerCase().startsWith(partial)) {
                    this.lastViableCompletions.add(name);
                }
            }
        }

        if (this.lastViableCompletions.isEmpty()) {
            return Optional.empty();
        } else {

            // see the note in completeCommand ('this should always hold, because...')

            assert this.lastCompletionIndex < this.lastViableCompletions.size();

            var completion = this.lastViableCompletions.get(this.lastCompletionIndex);
            this.lastCompletionIndex = (this.lastCompletionIndex + 1) % this.lastViableCompletions.size();

            return Optional.of(allExceptLast + completion + " ");
        }
    }



    /**
     * Returns a completion for the recipe name only.
     */
    private String completeRecipe(RequiredCompletion req, Model model, CommandArguments args, String orig) {

        return completeNamedItem(req, args, orig, model.getRecipeBook().getEntryList())
            .orElse(orig);
    }

    /**
     * Returns a completion for the ingredient name only.
     */
    private String completeIngredient(RequiredCompletion req, Model model, CommandArguments args, String orig) {

        return completeNamedItem(req, args, orig, model.getIngredientBook().getEntryList())
            .orElse(orig);
    }

    /**
     * Returns the required kind of completion for the current state of the user input.
     */
    private RequiredCompletion getRequiredCompletion(CommandArguments args, String orig) {

        if (args.getAllArguments().isEmpty()) {

            // if there's no remaining bits, then you clearly want the command name.
            if (args.getRemaining().isEmpty()) {
                return RequiredCompletion.COMMAND_NAME;
            }

            var cmd = args.getCommand();
            var sv = new StringView(args.getRemaining());
            var words = sv.words();

            if (commandRequiresTarget(cmd) && words.size() == 1) {
                return RequiredCompletion.TARGET_NAME;
            }

            // the help command needs another command.
            if (cmd.equals(Strings.COMMAND_HELP)) {

                // this is tricky. first, get the command you want help for:
                var helpedCmd = args.getFirstWordFromRemaining();
                if (commandRequiresTarget(helpedCmd) && words.size() == 2) {

                    return RequiredCompletion.NESTED_TARGET_NAME;

                } else if (helpedCmd.equals(Strings.COMMAND_STATS) && words.size() > 2) {

                    var target = words.get(1);
                    return CommandTarget.of(target)
                        .map(tgt -> {
                            switch (tgt) {
                            case RECIPE:
                                return RequiredCompletion.NESTED_STATS_RECIPE_KIND_NAME;

                            case INGREDIENT:
                                return RequiredCompletion.NESTED_STATS_INGREDIENT_KIND_NAME;

                            default:
                                return RequiredCompletion.NONE;
                            }
                        }).orElse(RequiredCompletion.NONE);
                }

                return RequiredCompletion.NESTED_COMMAND_NAME;
            }

            if (commandRequiresTarget(cmd)) {
                var target = args.getFirstWordFromRemaining();
                return CommandTarget.of(target)
                    .map(tgt -> {

                        switch (tgt) {
                        case RECIPE:
                            // we don't let you autocomplete recipe names when adding them
                            // -- only for ingredients.
                            if (cmd.equals(Strings.COMMAND_ADD)) {
                                return RequiredCompletion.NONE;
                            } else if (cmd.equals(Strings.COMMAND_STATS)) {
                                return RequiredCompletion.STATS_RECIPE_KIND_NAME;
                            }

                            return RequiredCompletion.RECIPE_NAME;

                        case INGREDIENT:
                            if (cmd.equals(Strings.COMMAND_STATS)) {
                                return RequiredCompletion.STATS_INGREDIENT_KIND_NAME;
                            }

                            return RequiredCompletion.INGREDIENT_NAME;

                        default:
                            return RequiredCompletion.NONE;
                        }

                    }).orElse(RequiredCompletion.NONE);

            } else if (commandRequiresItemReference(cmd)) {

                if (cmd.equals(Strings.COMMAND_MAKE) || cmd.equals(Strings.COMMAND_VIEW)) {
                    return RequiredCompletion.RECIPE_NAME;
                } else {
                    return RequiredCompletion.NONE;
                }

            } else {
                return RequiredCompletion.NONE;
            }

        } else {

            // get the last argument.
            // congratulations, it's 2020 and your dumb langauge collection library
            // has neither a front() nor back() method.
            var last = args.getAllArguments().get(args.getAllArguments().size() - 1);
            var lastArg = last.fst();
            var lastVal = last.snd();

            if (!lastVal.isEmpty()) {

                // now we should check what the name is.
                if (lastArg.nameEquals(Strings.ARG_INGREDIENT)) {
                    return RequiredCompletion.INGREDIENT_NAME_IN_ARG;
                } else if (lastArg.nameEquals(Strings.ARG_TAG)) {
                    return RequiredCompletion.TAG_NAME;
                } else {
                    return RequiredCompletion.NONE;
                }

            } else if (orig.endsWith(" ")) {

                // a bit dirty, but if the last char in the raw input was a space,
                // then we shouldn't complete anything (since the argument name was
                // already finished.
                return RequiredCompletion.NONE;

            } else {

                // check if the argument had components
                if (lastArg.getComponents().size() > 0) {
                    return RequiredCompletion.COMPONENT_NAME;
                } else {
                    // complete the argument name.
                    return RequiredCompletion.ARGUMENT_NAME;
                }
            }
        }
    }

    private Optional<String> tryCompletionUsing(List<String> candidates, String orig, String partial,
        String appending) {

        for (var arg : candidates) {
            if (arg.equals(partial)) {
                return Optional.of(orig);
            } else if (arg.startsWith(partial)) {
                return Optional.of(orig + arg.substring(partial.length()) + appending);
            }
        }

        return Optional.empty();
    }

    private Optional<String> tryCompletionUsing(List<String> candidates, String orig, String partial) {
        return tryCompletionUsing(candidates, orig, partial, " ");
    }

    private <T extends Entry> List<String> getAllTags(List<T> entries) {
        return new ArrayList<>(new HashSet<>(entries.stream()
            .flatMap(e -> e.getTags().stream())
            .map(t -> t.toString())
            .collect(Collectors.toList())));
    }

    private List<String> getAllIngredientTags(Model model) {
        return getAllTags(model.getIngredientBook().getEntryList());
    }

    private List<String> getAllRecipeTags(Model model) {
        return getAllTags(model.getRecipeBook().getEntryList());
    }


    @SafeVarargs
    private List<String> getArgNames(ArgName... args) {
        return Arrays.stream(args).map(ArgName::name).collect(Collectors.toList());
    }

    private List<String> getArgNames(List<ArgName> args) {
        return args.stream().map(ArgName::name).collect(Collectors.toList());
    }

    private boolean commandRequiresTarget(String commandName) {
        return List.of(
            Strings.COMMAND_ADD,
            Strings.COMMAND_LIST,
            Strings.COMMAND_FIND,
            Strings.COMMAND_EDIT,
            Strings.COMMAND_STATS,
            Strings.COMMAND_FILTER,
            Strings.COMMAND_DELETE
        ).indexOf(commandName) >= 0;
    }

    private boolean commandRequiresItemReference(String commandName) {
        return List.of(
            Strings.COMMAND_ADD,
            Strings.COMMAND_MAKE,
            Strings.COMMAND_EDIT,
            Strings.COMMAND_VIEW,
            Strings.COMMAND_DELETE
        ).indexOf(commandName) >= 0;
    }

    enum RequiredCompletion {
        NONE,

        COMMAND_NAME,
        TARGET_NAME,
        ARGUMENT_NAME,
        RECIPE_NAME,
        INGREDIENT_NAME,
        RECIPE_NAME_IN_ARG,
        INGREDIENT_NAME_IN_ARG,
        COMPONENT_NAME,
        TAG_NAME,
        STATS_RECIPE_KIND_NAME,
        STATS_INGREDIENT_KIND_NAME,
        NESTED_STATS_RECIPE_KIND_NAME,
        NESTED_STATS_INGREDIENT_KIND_NAME,
        NESTED_COMMAND_NAME,
        NESTED_TARGET_NAME
    }
}
