package ch.uzh.ifi.seal.soprafs20.entity.actions;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.*;
import ch.uzh.ifi.seal.soprafs20.utils.FranticUtils;

import java.util.ArrayList;
import java.util.List;

public class EqualityAction implements Action {
    private final Player initiator;
    private final Player target;
    private final Color color;
    private final DiscardPile discardPile;
    private final DrawStack drawStack;

    public EqualityAction(Player initiator, Player target, Color color, DiscardPile discardPile, DrawStack drawStack) {
        this.initiator = initiator;
        this.target = target;
        this.color = color;
        this.discardPile = discardPile;
        this.drawStack = drawStack;
    }

    @Override
    public List<Chat> perform() {
        List<Chat> chat = new ArrayList<>();
        if (this.target != null) {
            int cardsDrawn = 0;
            while (this.initiator.getHandSize() > this.target.getHandSize() && this.drawStack.size() > 0) {
                this.target.pushCardToHand(drawStack.pop());
                cardsDrawn++;
            }

            chat.add(new EventChat("special:equality", this.target.getUsername()
                    + " drew " + cardsDrawn + (cardsDrawn == 1 ? " card." : " cards.")));
        }
        discardPile.push(new Card(this.color, Type.WISH, Value.COLORWISH, false, 0));
        chat.add(new EventChat("special:equality", this.initiator.getUsername()
                + " wished " + FranticUtils.getStringRepresentation(this.color) + "."));

        return chat;
    }

    @Override
    public Player[] getTargets() {
        if (this.target == null) {
            return new Player[0];
        }
        return new Player[]{this.target};
    }

    @Override
    public Player getInitiator() {
        return this.initiator;
    }

    @Override
    public boolean isCounterable() {
        return true;
    }
}
