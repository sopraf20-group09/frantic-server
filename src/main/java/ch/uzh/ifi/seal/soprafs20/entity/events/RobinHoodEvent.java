package ch.uzh.ifi.seal.soprafs20.entity.events;

import ch.uzh.ifi.seal.soprafs20.entity.*;
import ch.uzh.ifi.seal.soprafs20.service.GameService;

import java.util.ArrayList;
import java.util.List;

public class RobinHoodEvent implements Event {

    private final GameRound gameRound;
    private final GameService gameService;
    private final List<Player> listOfPlayers;

    public RobinHoodEvent(GameRound gameRound) {
        this.gameRound = gameRound;
        this.gameService = gameRound.getGameService();
        this.listOfPlayers = gameRound.getListOfPlayers();
    }

    public String getName() {
        return "robin-hood";
    }

    public void performEvent() {
        Player currentPlayer = this.gameRound.getCurrentPlayer();
        int numOfPlayers = this.listOfPlayers.size();
        int currentPlayerIndex = this.listOfPlayers.indexOf(currentPlayer);
        int nextPlayerIndex = (currentPlayerIndex + 1) % numOfPlayers;

        //if two player has equal number of cards, the one closer to the current player is affected
        Player minCardsPlayer = this.listOfPlayers.get(nextPlayerIndex);
        Player maxCardsPlayer = this.listOfPlayers.get(nextPlayerIndex);

        for (int i = 1; i < numOfPlayers; i++) {
            Player playerOfInterest = this.listOfPlayers.get((nextPlayerIndex + i) % numOfPlayers);
            if (playerOfInterest.getHandSize() > maxCardsPlayer.getHandSize()) {
                maxCardsPlayer = playerOfInterest;
            }
            if (playerOfInterest.getHandSize() < minCardsPlayer.getHandSize()) {
                minCardsPlayer = playerOfInterest;
            }
        }

        if (!minCardsPlayer.equals(maxCardsPlayer)) {
            List<Card> maxTemp = new ArrayList<>();
            List<Card> minTemp = new ArrayList<>();
            int maxCards = maxCardsPlayer.getHandSize();
            int minCards = minCardsPlayer.getHandSize();

            for (int i = 0; i < maxCards; i++) {
                maxTemp.add(maxCardsPlayer.popCard());
            }
            for (int i = 0; i < minCards; i++) {
                minTemp.add(minCardsPlayer.popCard());
            }

            this.gameService.sendAnimationSpeed(this.gameRound.getLobbyId(), 0);
            this.gameRound.sendCompleteGameState();

            for (int i = 0; i < minCards; i++) {
                maxCardsPlayer.pushCardToHand(minTemp.get(i));
            }
            for (int i = 0; i < maxCards; i++) {
                minCardsPlayer.pushCardToHand(maxTemp.get(i));
            }
        }
        Chat chat = new EventChat("event:robin-hood", maxCardsPlayer.getUsername() + " and " + minCardsPlayer.getUsername() + " swapped all cards.");
        this.gameService.sendChatMessage(this.gameRound.getLobbyId(), chat);

        this.gameService.sendAnimationSpeed(this.gameRound.getLobbyId(), 500);
        this.gameRound.sendCompleteGameState();
        this.gameRound.finishTurn();
    }

    public String getMessage() {
        return "Some call him a hero, some call him a thief! The player with the least cards has to swap cards with the player holding the most!";
    }
}
