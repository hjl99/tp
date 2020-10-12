package chopchop.model.ingredient;

import chopchop.testutil.IngredientBuilder;
import chopchop.model.attributes.units.Count;

import static chopchop.testutil.TypicalIngredients.APRICOT;
import static chopchop.testutil.TypicalIngredients.BANANA;
import static seedu.address.logic.commands.CommandTestUtil.VALID_NAME_BOB;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngredientTest {

    @Test
    public void equals() {
        // same values -> returns true
        Ingredient apricotCopy = new IngredientBuilder(APRICOT).build();
        assertTrue(APRICOT.equals(apricotCopy));

        // same object -> returns true
        assertTrue(APRICOT.equals(APRICOT));

        // null -> returns false
        assertFalse(APRICOT.equals(null));

        // different type -> returns false
        assertFalse(APRICOT.equals(5));

        // different ingredient -> returns false
        assertFalse(APRICOT.equals(BANANA));

        // different name -> returns false
        Ingredient editedApricot = new IngredientBuilder(APRICOT).withName(VALID_NAME_BOB).build();
        assertFalse(APRICOT.equals(editedApricot));

        // different date -> returns false. Different ingredients can be of the same name but different expiry
        editedApricot = new IngredientBuilder(APRICOT).withDate("2020-12-02").build();
        assertFalse(APRICOT.equals(editedApricot));

        // different qty -> returns true
        editedApricot = new IngredientBuilder(APRICOT).withQuantity(Count.of(5)).build();
        assertTrue(APRICOT.equals(editedApricot));
    }
}
