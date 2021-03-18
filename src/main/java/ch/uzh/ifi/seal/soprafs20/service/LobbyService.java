package ch.uzh.ifi.seal.soprafs20.service;

import ch.uzh.ifi.seal.soprafs20.entity.Chat;
import ch.uzh.ifi.seal.soprafs20.entity.EventChat;
import ch.uzh.ifi.seal.soprafs20.entity.Lobby;
import ch.uzh.ifi.seal.soprafs20.entity.Player;
import ch.uzh.ifi.seal.soprafs20.exceptions.PlayerServiceException;
import ch.uzh.ifi.seal.soprafs20.repository.GameRepository;
import ch.uzh.ifi.seal.soprafs20.repository.LobbyRepository;
import ch.uzh.ifi.seal.soprafs20.repository.PlayerRepository;
import ch.uzh.ifi.seal.soprafs20.websocket.dto.incoming.KickDTO;
import ch.uzh.ifi.seal.soprafs20.websocket.dto.incoming.LobbySettingsDTO;
import ch.uzh.ifi.seal.soprafs20.websocket.dto.outgoing.DisconnectDTO;
import ch.uzh.ifi.seal.soprafs20.websocket.dto.outgoing.LobbyPlayerDTO;
import ch.uzh.ifi.seal.soprafs20.websocket.dto.outgoing.LobbyStateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Handles Lobby functionalities (create, join, leave, update, etc)
 */
@Service
@Transactional
public class LobbyService {

    Logger log = LoggerFactory.getLogger(LobbyService.class);

    private final WebSocketService webSocketService;
    private final PlayerService playerService;

    private final PlayerRepository playerRepository;
    private final LobbyRepository lobbyRepository;

    @Autowired
    public LobbyService(WebSocketService webSocketService, PlayerService playerService,
                        @Qualifier("playerRepository") PlayerRepository playerRepository,
                        @Qualifier("lobbyRepository") LobbyRepository lobbyRepository) {
        this.webSocketService = webSocketService;
        this.playerService = playerService;
        this.playerRepository = playerRepository;
        this.lobbyRepository = lobbyRepository;
    }

    public List<Lobby> getLobbies(String q) {
        List<Lobby> allLobbies;
        if (q != null) {
            allLobbies = this.lobbyRepository.findByNameContainsOrCreatorContains(q, q);
        }
        else {
            allLobbies = this.lobbyRepository.findAll();
        }
        allLobbies.removeIf(l -> !l.isPublic() || l.isPlaying() || l.getPlayers() == 0);
        allLobbies.removeIf(l -> !l.getListOfPlayers().contains(l.getCreator()));
        return allLobbies;
    }

    public synchronized String createLobby(Player creator) {
        Lobby newLobby = new Lobby();
        newLobby.setCreator(creator.getUsername());
        newLobby.addPlayer(creator);
        newLobby.setName(creator.getUsername() + "'s lobby");
        newLobby = this.lobbyRepository.save(newLobby);
        this.lobbyRepository.flush();

        String lobbyId = newLobby.getLobbyId();
        creator.setLobbyId(lobbyId);
        creator.setAdmin(true);
        this.playerRepository.save(creator);
        this.playerRepository.flush();
        return lobbyId;
    }

    public synchronized void joinLobby(String lobbyId, Player player) {
        Lobby lobby = this.lobbyRepository.findByLobbyId(lobbyId);
        lobby.addPlayer(player);
        this.lobbyRepository.flush();
        player = this.playerRepository.findByIdentity(player.getIdentity());
        player.setLobbyId(lobbyId);
        this.playerRepository.flush();
    }

    public synchronized void kickPlayer(String lobbyId, String identity, KickDTO dto) {
        if (this.webSocketService.checkSender(lobbyId, identity)) {
            Player admin = this.playerRepository.findByIdentity(identity);
            if (!admin.isAdmin()) {
                throw new PlayerServiceException("Invalid action. Not admin.");
            }

            Player toKick = this.playerRepository.findByUsernameAndLobbyId(dto.getUsername(), lobbyId);
            if (!admin.equals(toKick)) {
                DisconnectDTO disconnectDTO = new DisconnectDTO("You were kicked out of the Lobby.");
                this.webSocketService.sendToPlayer(toKick.getIdentity(), "/queue/disconnect", disconnectDTO);
                this.playerService.removePlayer(toKick);

                Chat chat = new EventChat("avatar:" + toKick.getUsername(), toKick.getUsername() + " was kicked!");
                this.webSocketService.sendChatMessage(lobbyId, chat);
                this.webSocketService.sendToLobby(lobbyId, "/lobby-state", getLobbyState(lobbyId));
            }
        }
    }

    public void handleDisconnect(String identity) {

        Player player = this.playerRepository.findByIdentity(identity);

        if (player != null) {
            String lobbyId = this.playerService.removePlayer(player);
            Lobby lobby = this.lobbyRepository.findByLobbyId(lobbyId);
            if (lobby != null) {
                log.info("Lobby " + lobbyId + ": Player " + identity + " left");

                Chat chat = new EventChat("avatar:" + player.getUsername(), player.getUsername() + " left the lobby.");
                this.webSocketService.sendChatMessage(lobbyId, chat);
                List<Player> playerList = this.playerRepository.findByLobbyId(lobbyId);
                if (playerList.size() > 1) {
                    if (player.isAdmin()) {
                        setNewHost(lobbyId, playerList.get(0).getUsername());
                    }
                    this.webSocketService.sendToLobby(lobbyId, "/lobby-state", getLobbyState(lobbyId));
                }
                else if (playerList.size() == 1) {
                    if (lobby.isPlaying()) {
                        Player last = this.playerRepository.findByUsernameAndLobbyId(playerList.get(0).getUsername(), lobbyId);
                        DisconnectDTO disconnectDTO = new DisconnectDTO("Not enough players to play.");
                        this.webSocketService.sendToPlayer(last.getIdentity(), "/queue/disconnect", disconnectDTO);
                        this.playerService.removePlayer(last);
                        this.lobbyRepository.delete(lobby);
                        GameRepository.removeGame(lobbyId);
                    }
                    else {
                        if (player.isAdmin()) {
                            setNewHost(lobbyId, playerList.get(0).getUsername());
                        }
                        this.webSocketService.sendToLobby(lobbyId, "/lobby-state", getLobbyState(lobbyId));
                    }
                }
                else {
                    this.lobbyRepository.delete(lobby);
                }
            }
        }
    }

    private synchronized void setNewHost(String lobbyId, String newHostUsername) {
        Lobby lobby = this.lobbyRepository.findByLobbyId(lobbyId);
        Player newHost = this.playerRepository.findByUsernameAndLobbyId(newHostUsername, lobbyId);
        newHost.setAdmin(true);
        lobby.setCreator(newHost.getUsername());
        this.playerRepository.flush();
        this.lobbyRepository.flush();
        Chat chat = new EventChat("avatar:" + newHost.getUsername(), newHost.getUsername() + " is now host.");
        this.webSocketService.sendChatMessage(lobbyId, chat);
    }

    public synchronized void updateLobbySettings(String lobbyId, String identity, LobbySettingsDTO dto) {
        if (this.webSocketService.checkSender(lobbyId, identity)) {
            Lobby lobbyToUpdate = this.lobbyRepository.findByLobbyId(lobbyId);
            if (dto.getLobbyName() != null && !dto.getLobbyName().matches("^\\s*$")) {
                lobbyToUpdate.setName(dto.getLobbyName());
            }
            if (dto.getGameDuration() != null) {
                lobbyToUpdate.setGameDuration(dto.getGameDuration());
            }
            if (dto.getTurnDuration() != null) {
                lobbyToUpdate.setTurnDuration(dto.getTurnDuration());
            }
            if (dto.getPublicLobby() != null) {
                lobbyToUpdate.setIsPublic(dto.getPublicLobby());
            }
            this.lobbyRepository.flush();
            this.webSocketService.sendToLobby(lobbyId, "/lobby-state", getLobbyState(lobbyId));
        }
    }

    public void rematch(String lobbyId, String identity) {
        if (this.webSocketService.checkSender(lobbyId, identity)) {
            Player player = this.playerRepository.findByIdentity(identity);
            Lobby lobby = this.lobbyRepository.findByLobbyId(lobbyId);
            if (!lobby.isPlaying()) {
                lobby.addPlayer(player);
                this.lobbyRepository.flush();
                for (String username : lobby.getListOfPlayers()) {
                    Player p = this.playerRepository.findByUsernameAndLobbyId(username, lobbyId);
                    this.webSocketService.sendToPlayerInLobby(lobbyId, p.getIdentity(), "/lobby-state", getLobbyState(lobbyId));
                }
                Chat chat = new EventChat("avatar:" + player.getUsername(), player.getUsername() + " accepted a rematch!");
                this.webSocketService.sendChatMessage(lobbyId, chat);
            }
        }
    }

    public LobbyStateDTO getLobbyState(String lobbyId) {
        Lobby lobby = this.lobbyRepository.findByLobbyId(lobbyId);
        List<String> listOfPlayers = lobby.getListOfPlayers();

        LobbyPlayerDTO[] players = new LobbyPlayerDTO[listOfPlayers.size()];
        int c = 0;
        for (String p : listOfPlayers) {
            Player currentPlayer = this.playerRepository.findByUsernameAndLobbyId(p, lobbyId);
            LobbyPlayerDTO player = new LobbyPlayerDTO();
            player.setUsername(currentPlayer.getUsername());
            player.setAdmin(currentPlayer.isAdmin());
            players[c] = player;
            c++;
        }

        LobbySettingsDTO settings = new LobbySettingsDTO();
        settings.setLobbyName(lobby.getName());
        settings.setGameDuration(lobby.getGameDuration());
        settings.setTurnDuration(lobby.getTurnDuration());
        settings.setPublicLobby(lobby.isPublic());

        return new LobbyStateDTO(players, settings);
    }

    public boolean isUsernameAlreadyInLobby(String lobbyId, String username) {
        return this.playerRepository.findByUsernameAndLobbyId(username, lobbyId) != null;
    }

    public void checkLobbyJoin(String lobbyId, String username) {
        checkLobbyCreate(username);
        if (this.lobbyRepository.findByLobbyId(lobbyId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found.");
        }
        if (isUsernameAlreadyInLobby(lobbyId, username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists.");
        }
        if (this.lobbyRepository.findByLobbyId(lobbyId).getPlayers() >= 8) {
            throw new ResponseStatusException(HttpStatus.GONE, "Lobby is full.");
        }
        if (this.lobbyRepository.findByLobbyId(lobbyId).isPlaying()) {
            throw new ResponseStatusException(HttpStatus.GONE, "The game has already started");
        }
    }

    public void checkLobbyCreate(String username) {
        if (username == null || !username.matches("^\\S{2,20}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username missing or invalid.");
        }
    }
}
