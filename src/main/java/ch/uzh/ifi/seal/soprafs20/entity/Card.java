package ch.uzh.ifi.seal.soprafs20.entity;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.utils.FranticUtils;

public class Card {

    private final int orderKey;
    private final String key;
    private final boolean counterable;
    private final Color color;
    private final Type type;
    private final Value value;

    public Card(Color c, int value, int orderKey) {
        this(c, Type.NUMBER, Value.values()[value - 1], false, orderKey);
        if (value > 10) {
            throw new RuntimeException("Invalid number");
        }
    }

    public Card(Color c, Type t, Value v, boolean counterable, int orderKey) {
        this.type = t;
        this.color = c;
        this.value = v;
        this.counterable = counterable;
        this.key = FranticUtils.generateId(8);
        this.orderKey = orderKey;
    }

    // constructor for wish card.
    public Card(Color c, Type t, Value v) {
        this(c, t, v, false, -1);
    }

    public Color getColor() {
        return color;
    }

    public Value getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public int getOrderKey() {
        return orderKey;
    }

    public boolean isCounterable() {
        return counterable;
    }

    public boolean isPlayableOn(Card other) {
        // if a multicolor card is on top at the beginning
        if (other.getColor().equals(Color.MULTICOLOR) && other.getType().equals(Type.SPECIAL)) {
            if (other.getValue().equals(Value.FANTASTIC) || other.getValue().equals(Value.FANTASTICFOUR)) {
                return true;
            }
            else if (this.getValue() == Value.FUCKYOU) {
                return true;
            }
            else {
                return !this.getColor().equals(Color.BLACK);
            }
        }
        // if the fuckyou card is on top at the beginning
        if (other.getValue().equals(Value.FUCKYOU)) {
            return true;
        }

        switch (this.getColor()) {
            case BLACK:
                return this.getValue() == Value.FUCKYOU || this.getValue() == other.getValue();

            case MULTICOLOR:
                return true;

            default:
                return (this.getColor() == other.getColor() || this.getValue() == other.getValue());
        }
    }

    public String toString() {
        return "Card: " + this.color.toString() + ", " + this.value.toString() + ", " + this.type.toString();
    }

    public String keysToString() {
        return "Card key of " + this.color + this.value + ": " + this.key + ", " + this.orderKey;
    }

}
