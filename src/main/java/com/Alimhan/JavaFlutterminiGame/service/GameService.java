package com.Alimhan.JavaFlutterminiGame.service;
import com.Alimhan.JavaFlutterminiGame.models.*;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class GameService {
    private final Firestore db;
    private static final Logger logger = Logger.getLogger(GameService.class.getName());

    public GameService() {
        db = FirestoreClient.getFirestore();
    }

    private DocumentReference getRoomRef(String roomId) {
        return db.collection("Rooms").document(roomId);
    }

    private CollectionReference getPlayersRef(String roomId) {
        return getRoomRef(roomId).collection("Players");
    }

    private CollectionReference getQuestionsRef() {
        return db.collection("Questions");
    }

    public String createRoom(Room newRoom) {
        if (newRoom == null || newRoom.getId() == null) return null;

        DocumentReference roomRef = getRoomRef(newRoom.getId());
        try {
            roomRef.create(newRoom).get();
            logger.info("Room created: " + newRoom.getId());
            return newRoom.getId();
        } catch (Exception e) {
            logger.warning("Failed to create room: " + e.getMessage());
            return null;
        }
    }

    public Map<String, String> joinRoom(String roomNumber, String roomPassword, Player player) {
        if (roomNumber == null || roomPassword == null || player == null || player.getId() == null)
            return null;

        try {
            QuerySnapshot snapshot = db.collection("Rooms")
                    .whereEqualTo("roomNumber", roomNumber)
                    .whereEqualTo("roomPassword", roomPassword)
                    .limit(1)
                    .get().get();

            if (snapshot.isEmpty()) return null;

            DocumentReference roomRef = snapshot.getDocuments().getFirst().getReference();
            DocumentReference playerRef = roomRef.collection("Players").document(player.getId());
            playerRef.set(player, SetOptions.merge()).get();

            return Map.of("room_id", roomRef.getId(), "player_id", player.getId());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error joining room: " + e.getMessage(), e);
            return null;
        }
    }

    public void exitRoom(String roomId, Player player) {
        if (roomId == null || player == null || player.getId() == null) return;

        DocumentReference roomRef = getRoomRef(roomId);
        try {
            DocumentSnapshot roomDoc = roomRef.get().get();
            if (!roomDoc.exists()) return;

            Room room = roomDoc.toObject(Room.class);
            if (room == null) return;

            if (room.getHostId().equals(player.getId())) {
                deleteRoom(roomId);
            } else {
                getPlayersRef(roomId).document(player.getId()).delete().get();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error exiting room: " + e.getMessage(), e);
        }
    }

    public void deleteRoom(String roomId) {
        if (roomId == null) return;

        DocumentReference roomRef = getRoomRef(roomId);
        try {
            Iterable<DocumentReference> players = getPlayersRef(roomId).listDocuments();
            WriteBatch batch = db.batch();
            players.forEach(batch::delete);
            batch.delete(roomRef);
            batch.commit().get();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error deleting room: " + e.getMessage(), e);
        }
    }

    public Room getRoomById(String roomId) {
        if (roomId == null) return null;

        try {
            DocumentSnapshot doc = getRoomRef(roomId).get().get();
            return doc.exists() ? doc.toObject(Room.class) : null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting room: " + e.getMessage(), e);
            return null;
        }
    }

    public String nextQuestion(String roomId, String currentQuestionId) {
        try {
            DocumentReference currentRef = db.collection("Questions").document(currentQuestionId);
            DocumentSnapshot currentSnapshot = currentRef.get().get();

            Query query = db.collection("Questions")
                    .orderBy(FieldPath.documentId())
                    .startAfter(currentSnapshot)
                    .limit(1);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            String nextId = docs.isEmpty() ?
                    db.collection("Questions").orderBy(FieldPath.documentId()).limit(1).get().get().getDocuments().get(0).getId()
                    : docs.getFirst().getId();

            getRoomRef(roomId).update("currentQuestion", nextId).get();
            return nextId;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching next question", e);
            return null;
        }
    }

    public void setFirstBuzzer(String roomId, String playerId) {
        if (roomId == null || playerId == null) return;

        try {
            DocumentReference roomRef = getRoomRef(roomId);
            DocumentSnapshot roomSnapshot = roomRef.get().get();
            if (!roomSnapshot.exists()) {
                logger.warning("Room not found: " + roomId);
                return;
            }

            String currentFirstBuzzer = roomSnapshot.getString("firstBuzzer");
            if (currentFirstBuzzer != null) {
                logger.warning("First buzzer already set for room: " + roomId + " to player: " + currentFirstBuzzer);
                return;
            }

            roomRef.update("firstBuzzer", playerId, "buzzingOpen", true);
            logger.info("First buzzer set to player: " + playerId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting first buzzer: " + e.getMessage(), e);
        }
    }

    public void resetBuzzing(String roomId, String questionId) {
        if (roomId == null || questionId == null) return;

        try {
            // Get room document to retrieve firstBuzzer
            DocumentReference roomRef = getRoomRef(roomId);
            DocumentSnapshot roomSnapshot = roomRef.get().get();
            if (!roomSnapshot.exists()) {
                logger.warning("Room not found: " + roomId);
                return;
            }

            String playerId = roomSnapshot.getString("firstBuzzer");
            if (playerId == null) {
                logger.warning("No first buzzer set for room: " + roomId);
                return;
            }

            // Get question data
            DocumentReference questionRef = db.collection("Questions").document(questionId);
            DocumentSnapshot questionSnapshot = questionRef.get().get();

            if (!questionSnapshot.exists()) {
                logger.warning("Question not found: " + questionId);
                return;
            }

            Long questionScore = questionSnapshot.getLong("score");
            if (questionScore == null) questionScore = 0L;

            // Get player data and update score
            DocumentReference playerRef = getRoomRef(roomId).collection("Players").document(playerId);
            DocumentSnapshot playerSnapshot = playerRef.get().get();

            Long currentScore = playerSnapshot.getLong("score");
            if (currentScore == null) currentScore = 0L;

            long newScore = currentScore + questionScore;
            playerRef.update("score", newScore).get();

            // Reset buzzing flags
            roomRef.update("buzzingOpen", false, "firstBuzzer", null).get();
            logger.info("Buzzing reset and score updated for player: " + playerId + " with +" + questionScore);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error resetting buzzing: " + e.getMessage(), e);
        }
    }

    public void cancelBuzzing(String roomId, String questionId) {
        if (roomId == null || questionId == null) return;

        try {
            // Get room document to retrieve firstBuzzer
            DocumentReference roomRef = getRoomRef(roomId);
            DocumentSnapshot roomSnapshot = roomRef.get().get();
            if (!roomSnapshot.exists()) {
                logger.warning("Room not found: " + roomId);
                return;
            }

            String playerId = roomSnapshot.getString("firstBuzzer");
            if (playerId == null) {
                logger.warning("No first buzzer set for room: " + roomId);
                return;
            }

            // Get question data
            DocumentReference questionRef = db.collection("Questions").document(questionId);
            DocumentSnapshot questionSnapshot = questionRef.get().get();

            if (!questionSnapshot.exists()) {
                logger.warning("Question not found: " + questionId);
                return;
            }

            Long questionScore = questionSnapshot.getLong("score");
            if (questionScore == null) questionScore = 0L;

            // Get player data and update score
            DocumentReference playerRef = getRoomRef(roomId).collection("Players").document(playerId);
            DocumentSnapshot playerSnapshot = playerRef.get().get();

            Long currentScore = playerSnapshot.getLong("score");
            if (currentScore == null) currentScore = 0L;

            long newScore = currentScore - questionScore;
            playerRef.update("score", newScore).get();

            // Reset buzzing flags
            roomRef.update("buzzingOpen", false, "firstBuzzer", null).get();
            logger.info("Buzzing cancelled and score updated for player: " + playerId + " with -" + questionScore);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error cancelling buzzing: " + e.getMessage(), e);
        }
    }

    public void createFinalScoreBoard(String roomId) {
        try {
            QuerySnapshot playersSnapshot = getPlayersRef(roomId)
                    .orderBy("score", Query.Direction.DESCENDING)
                    .get()
                    .get();

            if (playersSnapshot.isEmpty()) {
                logger.warning("No players found in room: " + roomId);
                return;
            }

            String text = "";
            int rank = 1;
            for (QueryDocumentSnapshot playerDoc : playersSnapshot.getDocuments()) {
                Player player = playerDoc.toObject(Player.class);
                text += rank + " " + player.getName() + " - " + player.getScore() + "\n";
                rank++;
            }
            Map<String, Object> finalScoreboard = new HashMap<>();
            finalScoreboard.put("text", text);
            finalScoreboard.put("score", 0);
            finalScoreboard.put("answer", "Final Scoreboard");
            DocumentReference scoreboard = getQuestionsRef().document();
            scoreboard.set(finalScoreboard).get();
            getRoomRef(roomId).update("currentQuestion", scoreboard.getId()).get();
            logger.info("Final scoreboard created for room: " + roomId);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating final scoreboard: " + e.getMessage(), e);
        }

    }





}
