package de.unistuttgart.t2.ui.domain;

/**
 * TODO
 *
 * @author maumau
 */
public class ItemToAdd {

    private String productId;
    private int units;

    public ItemToAdd() {
        // TODO Auto-generated constructor stub
    }

    public ItemToAdd(String productId, int units) {
        this.productId = productId;
        this.units = units;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getUnits() {
        return units;
    }

    public void setUnits(int units) {
        this.units = units;
    }

    @Override
    public String toString() {
        return "ItemToAdd [productId=" + productId + ", units=" + units + "]";
    }
}
