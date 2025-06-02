package com.Alimhan.JavaFlutterminiGame.Controllers;
import com.Alimhan.JavaFlutterminiGame.models.*;
import com.Alimhan.JavaFlutterminiGame.service.GameService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/game")
@CrossOrigin(origins = "*")  // Enable CORS for all origins
public class GameController {
    private final GameService gameService;
    private static final Logger logger = Logger.getLogger(GameController.class.getName());

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Creates a new game room
     * @param room Room object containing room details
     * @return Response with room ID or error message
     */
    @PostMapping("/createRoom")
    public ResponseEntity<Map<String, String>> createRoom(@RequestBody Room room) {
        logger.info("Received request to create room: " + room);

        try {
            String roomId = gameService.createRoom(room);
            if (roomId != null) {
                logger.info("Room created successfully with ID: " + roomId);
                return ResponseEntity.ok(Map.of("roomId", roomId, "status", "success"));
            } else {
                logger.warning("Failed to create room, room might already exist");
                return ResponseEntity.badRequest().body(Map.of("error", "Room already exists or invalid room data"));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating room", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    /**
     * Joins an existing game room
     * @param joinRequest Object containing room and player information
     * @return Response with room and player IDs or error message
     */
    @PostMapping("/joinRoom")
    public ResponseEntity<Map<String, String>> joinRoom(@RequestBody JoinRoomRequest joinRequest) {
        logger.info("Received request to join room: " + joinRequest);

        try {
            if (joinRequest == null || joinRequest.getRoomID() == null || joinRequest.getPlayer() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid request data"));
            }

            Map<String, String> result = gameService.joinRoom(joinRequest.getRoomID(), joinRequest.getRoomPassword(), joinRequest.getPlayer());
            if (result != null) {
                logger.info("Player " + joinRequest.getPlayer().getId() + " joined room " + joinRequest.getRoomID());
                return ResponseEntity.ok(result);
            } else {
                logger.warning("Failed to join room: Room not found or player already exists");
                return ResponseEntity.badRequest().body(Map.of("error", "Room not found or player already exists"));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error joining room", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    /**
     * Player exits a room
     * @param exitRequest Object containing room ID and player information
     * @return Response indicating success or error
     */
    @PostMapping("/rooms/exit")
    public ResponseEntity<Map<String, String>> exitRoom(@RequestBody ExitRoomRequest exitRequest) {
        logger.info("Received request to exit room: " + exitRequest);

        try {
            if (exitRequest == null || exitRequest.getRoomId() == null || exitRequest.getPlayer() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid request data"));
            }

            gameService.exitRoom(exitRequest.getRoomId(), exitRequest.getPlayer());
            logger.info("Player " + exitRequest.getPlayer().getId() + " exited room " + exitRequest.getRoomId());
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error exiting room", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    /**
     * Deletes a room
     * @param roomId The ID of the room to delete
     * @return Response indicating success or error
     */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, String>> deleteRoom(@PathVariable String roomId) {
        logger.info("Received request to delete room: " + roomId);

        try {
            if (roomId == null || roomId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid room ID"));
            }

            gameService.deleteRoom(roomId);
            logger.info("Room " + roomId + " deleted successfully");
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error deleting room", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    /**
     * Gets a room by ID
     * @param roomId The ID of the room to get
     * @return Response with room details or error message
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {
        logger.info("Received request to get room: " + roomId);

        try {
            Room room = gameService.getRoomById(roomId);
            if (room != null) {
                logger.info("Room " + roomId + " found");
                return ResponseEntity.ok(room);
            } else {
                logger.warning("Room " + roomId + " not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting room", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    @PostMapping("/rooms/{roomId}/nextQuestion")
    public ResponseEntity<Map<String, String>> nextQuestion(
            @PathVariable String roomId,
            @RequestParam String currentQuestionId) {
        String nextQuestionId = gameService.nextQuestion(roomId, currentQuestionId);
        if (nextQuestionId != null) {
            logger.info("Next question ID: " + nextQuestionId);
            return ResponseEntity.ok(Map.of("questionId", nextQuestionId));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Could not get next question"));
    }

    @PostMapping("/FirstBuzzer")
    public ResponseEntity<Map<String, String>> firstBuzzer(@RequestBody BuzzerRequest request) {
        logger.info("Received request to set first buzzer: " + request);
        try {
            if (request.getRoomId() == null      || request.getPlayerId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid roomId or playerId"));
            }
            gameService.setFirstBuzzer(request.getRoomId(), request.getPlayerId());
            logger.info("Player " + request.getPlayerId() + " is the first buzzer in room " + request.getRoomId());
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting first buzzer", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    /**
     * Resets buzzing and updates player score
     */
    @PostMapping("/resetBuzzing")
    public ResponseEntity<Map<String, String>> resetBuzzing(@RequestBody BuzzerRequest request) {
        logger.info("Received request to reset buzzing: " + request);
        try {
            if (request.getRoomId() == null  || request.getQuestionId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid roomId, playerId, or questionId"));
            }
            gameService.resetBuzzing(request.getRoomId(), request.getQuestionId());
            logger.info("Buzzing reset for player " + request.getPlayerId() + " in room " + request.getRoomId());
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error resetting buzzing", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    /**
     * Cancels buzzing and deducts player score
     */
    @PostMapping("/cancelBuzzing")
    public ResponseEntity<Map<String, String>> cancelBuzzing(@RequestBody BuzzerRequest request) {
        logger.info("Received request to cancel buzzing: " + request);
        try {
            if (request.getRoomId() == null || request.getQuestionId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid roomId, playerId, or questionId"));
            }
            gameService.cancelBuzzing(request.getRoomId(), request.getQuestionId());
            logger.info("Buzzing cancelled for room " + request.getRoomId());
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error cancelling buzzing", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    @PostMapping("/final/{roomId}")
    public ResponseEntity<Map<String, String>> createFinalScoreBoard(@PathVariable String roomId){
        try {
            gameService.createFinalScoreBoard(roomId);
            return ResponseEntity.ok(Map.of("status", "success"));
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating final scoreboard", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }
}


/**
 * Request object for buzzer-related actions
 */
@Setter
@Getter
class BuzzerRequest {
    private String roomId;
    private String playerId;
    private String questionId;

    public BuzzerRequest() {}

    @Override
    public String toString() {
        return "BuzzerRequest{roomId=" + roomId + ", playerId=" + playerId + ", questionId=" + questionId + "}";
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }
}

/**
 * Request object for joining a room
 */
@Setter
@Getter
class JoinRoomRequest {
    private String roomID;
    private String roomPassword;
    private Player player;

    public JoinRoomRequest() {}

    @Override
    public String toString() {
        return "JoinRoomRequest{room=" + roomID + ", player=" + player + "}";
    }

    public String getRoomID() {
        return roomID;
    }

    public void setRoomID(String roomID) {
        this.roomID = roomID;
    }

    public String getRoomPassword() {
        return roomPassword;
    }

    public void setRoomPassword(String roomPassword) {
        this.roomPassword = roomPassword;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}

/**
 * Request object for exiting a room
 */
@Setter
@Getter
class ExitRoomRequest {
    private String roomId;
    private Player player;

    public ExitRoomRequest() {}

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public String toString() {
        return "ExitRoomRequest{roomId=" + roomId + ", player=" + player + "}";
    }
}
