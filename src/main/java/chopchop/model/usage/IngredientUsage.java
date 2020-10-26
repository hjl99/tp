package chopchop.model.usage;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import chopchop.model.attributes.Quantity;

public class IngredientUsage extends Usage {
    private final Quantity qty;

    /**
     * Construct an {@code IngredientUsage} given the name, date and quantity.
     */
    public IngredientUsage(String name, LocalDateTime date, Quantity qty) {
        super(name, date);
        requireNonNull(qty);
        this.qty = qty;
    }

    public Quantity getQty() {
        return this.qty;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] Qty: %s", this.getName(),
            this.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            this.getQty());
    }
}
