package ch.uzh.ifi.seal.soprafs20.entity.events;

import ch.uzh.ifi.seal.soprafs20.constant.Color;
import ch.uzh.ifi.seal.soprafs20.constant.Type;
import ch.uzh.ifi.seal.soprafs20.constant.Value;
import ch.uzh.ifi.seal.soprafs20.entity.*;
import ch.uzh.ifi.seal.soprafs20.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GamblingManEventTest {

    @Mock
    private GameService gameService;
    @Mock
    private GameRound gameRound;

    private List<Player> listOfPlayers = new ArrayList<>();

    @BeforeEach
    void setup() {
        this.listOfPlayers = new ArrayList<>();

        MockitoAnnotations.initMocks(this);
        Mockito.when(this.gameRound.getGameService()).thenReturn(this.gameService);
        Mockito.when(this.gameRound.getListOfPlayers()).thenReturn(this.listOfPlayers);
    }

    @Test
    void getNameTest() {
        GamblingManEvent gamblingMan = new GamblingManEvent(this.gameRound);
        assertEquals("gambling-man", gamblingMan.getName());
    }

    @Test
    void performEventTest() {
        Mockito.doNothing().when(gameService).sendGamblingMan(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(gameService).sendTimer(Mockito.any(), Mockito.anyInt());

        Player player1 = new Player();
        player1.pushCardToHand(new Card(Color.RED, 7, 11));
        player1.pushCardToHand(new Card(Color.MULTICOLOR, Type.SPECIAL, Value.FANTASTIC, false, 12));
        player1.pushCardToHand(new Card(Color.RED, Type.SPECIAL, Value.SKIP, true, 13));
        this.listOfPlayers.add(player1);

        DiscardPile pile = new DiscardPile();
        pile.push(new Card(Color.RED, 3, 1));
        pile.push(new Card(Color.BLACK, 3, 2));
        Mockito.when(this.gameRound.getDiscardPile()).thenReturn(pile);
        Mockito.when(this.gameRound.getCurrentPlayer()).thenReturn(player1);

        Event gamblingMan = new GamblingManEvent(this.gameRound);
        gamblingMan.performEvent();

        Mockito.verify(gameService).sendGamblingMan(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void getMessageTest() {
        GamblingManEvent gamblingMan = new GamblingManEvent(this.gameRound);
        assertEquals("It's time to gamble! Choose a number card of the last played color. The player with the highest digit has to take all of them. So choose wisely!", gamblingMan.getMessage());
    }
}