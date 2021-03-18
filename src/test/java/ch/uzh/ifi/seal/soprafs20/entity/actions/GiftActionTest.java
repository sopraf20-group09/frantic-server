package ch.uzh.ifi.seal.soprafs20.entity.actions;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.Card;
import ch.uzh.ifi.seal.soprafs20.entity.Chat;
import ch.uzh.ifi.seal.soprafs20.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiftActionTest {

    private Player initiator;
    private Player target;
    private Action giftAction;
    private final Card blue1 = new Card(Color.BLUE, Type.NUMBER, Value.ONE, false, 0);
    private final Card blue2 = new Card(Color.BLUE, Type.NUMBER, Value.TWO, false, 1);
    private final Card blue3 = new Card(Color.BLUE, Type.NUMBER, Value.THREE, false, 2);
    private final Card fuckYou = new Card(Color.MULTICOLOR, Type.SPECIAL, Value.FUCKYOU, false, 3);

    @BeforeEach
    void setup() {
        this.initiator = new Player();
        this.initiator.setUsername("GiftMaker");
        this.initiator.pushCardToHand(blue1);
        this.initiator.pushCardToHand(blue2);
        this.initiator.pushCardToHand(blue3);

        this.target = new Player();
        this.target.setUsername("GiftTaker");

        int[] gifts = new int[]{1, 2};

        this.giftAction = new GiftAction(initiator, target, gifts);
    }

    @Test
    void performTest() {
        List<Chat> resultChat = giftAction.perform();
        assertEquals(1, this.initiator.getHandSize());
        assertEquals(2, this.target.getHandSize());
        assertEquals(blue1, this.initiator.popCard(0));
        assertEquals(blue2, this.target.popCard(0));
        assertEquals(blue3, this.target.popCard(0));

        assertEquals("event", resultChat.get(0).getType());
        assertEquals("special:gift", resultChat.get(0).getIcon());
        assertEquals("GiftMaker gifted GiftTaker 2 cards.", resultChat.get(0).getMessage());
    }

    @Test
    void getTargetsTest() {
        assertEquals(this.target, giftAction.getTargets()[0]);
    }

    @Test
    void getInitiatorTest() {
        assertEquals(this.initiator, giftAction.getInitiator());
    }

    @Test
    void isCounterableTest() {
        assertTrue(giftAction.isCounterable());
    }

    @Test
    void fuckYouNotGiftableTest() {
        this.initiator.popCard(0);
        this.initiator.pushCardToHand(fuckYou);
        giftAction.perform();
        assertEquals(2, this.initiator.getHandSize());
        assertEquals(1, this.target.getHandSize());
        assertEquals(blue2, this.initiator.popCard(0));
        assertEquals(fuckYou, this.initiator.popCard(0));
        assertEquals(blue3, this.target.popCard(0));
    }
}