package com.Alimhan.JavaFlutterminiGame.models;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Room {
    private String id;
    private String roomNumber;
    private String roomPassword;
    private boolean buzzingOpen = false;
    private String firstBuzzer;      // playerId
    private String currentQuestion = "1";
    private Player host;

    public Room() {}

    public Room(String id, String roomNumber, String roomPassword, Player host) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomPassword = roomPassword;
        this.host = host;
    }



    public String getHostId(){
        return host.getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomPassword() {
        return roomPassword;
    }

    public void setRoomPassword(String roomPassword) {
        this.roomPassword = roomPassword;
    }

    public boolean isBuzzingOpen() {
        return buzzingOpen;
    }

    public void setBuzzingOpen(boolean buzzingOpen) {
        this.buzzingOpen = buzzingOpen;
    }

    public String getFirstBuzzer() {
        return firstBuzzer;
    }

    public void setFirstBuzzer(String firstBuzzer) {
        this.firstBuzzer = firstBuzzer;
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(String currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public Player getHost() {
        return host;
    }

    public void setHost(Player host) {
        this.host = host;
    }
}
