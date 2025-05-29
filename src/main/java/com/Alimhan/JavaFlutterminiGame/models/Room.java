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





}
