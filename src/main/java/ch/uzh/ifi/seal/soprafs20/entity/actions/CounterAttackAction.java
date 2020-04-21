package ch.uzh.ifi.seal.soprafs20.entity.actions;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.*;
import ch.uzh.ifi.seal.soprafs20.utils.FranticUtils;

public class CounterAttackAction implements Action {
    private Player initiator;
    private Player[] targets;
    private Color color;
    private DiscardPile discardPile;
    private DrawStack drawStack;

    public CounterAttackAction(Player initiator, Color color, DiscardPile discardPile) {
        this.initiator = initiator;
        this.color = color;
        this.discardPile = discardPile;
    }

    @Override
    public Chat perform() {
        this.discardPile.push(new Card(this.color, Type.WISH, Value.COLORWISH, false, 0));
        return new Chat("event", "counter-attack", this.initiator.getUsername()
                + " wished " + FranticUtils.getStringRepresentation(this.color));
    }

    @Override
    public Player[] getTargets() {
        return this.targets;
    }

    @Override
    public Player getInitiator() {
        return this.initiator;
    }

    @Override
    public boolean isCounterable() {
        return false;
    }
}
