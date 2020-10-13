package chopchop.ui;

import chopchop.model.attributes.Step;
import chopchop.model.ingredient.IngredientReference;
import chopchop.model.recipe.Recipe;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RecipeDisplay extends UiPart<Region> {

    private static final String FXML = "RecipeDisplay.fxml";

    private final Recipe recipe;

    private String name;
    private String ingredients;
    private String steps;


    @FXML
    private TextArea recipeName;

    @FXML
    private TextArea instructionDisplay;

    @FXML
    private TextArea ingredientDisplay;

    /**
     * Creates a {@code RecipeDisplay} with a {@code Recipe}.
     * @param recipe
     */
    public RecipeDisplay(Recipe recipe) {
        super(FXML);
        this.recipe = recipe;
        stringRepresentation();
        display();
    }

    /**
     * Displays the recipe on the recipeDisplay.
     */
    private void display() {

        assert !name.isEmpty();

        recipeName.clear();
        recipeName.setText(name);

        if (!ingredients.isEmpty()) {
            ingredientDisplay.clear();
            ingredientDisplay.setText(ingredients);
        }
        if (!steps.isEmpty()) {
            instructionDisplay.clear();
            instructionDisplay.setText(steps);
        }
    }

    private void stringRepresentation() {
        final StringBuilder builder = new StringBuilder();
        name = recipe.getName().toString();

        List<IngredientReference> ingredientList = recipe.getIngredients();
        if (!ingredientList.isEmpty()) {
            builder.append(" Ingredients:\n");
            ingredientList.forEach(ingredient -> builder.append(ingredient).append("\n"));
            ingredients = builder.toString();
        }

        List<Step> stepsList = recipe.getSteps();
        if (!stepsList.isEmpty()) {
            builder.setLength(0);
            builder.append(" Steps:");
            AtomicInteger counter = new AtomicInteger(1);
            stepsList.forEach(step -> {
                builder.append("\n").append(counter.getAndIncrement()).append(". ");
                builder.append(step);
            });
            steps = builder.toString();
        }
    }
}
