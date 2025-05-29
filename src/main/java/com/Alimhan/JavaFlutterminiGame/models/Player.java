package com.Alimhan.JavaFlutterminiGame.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player {
    private String id;
    private String name;
    private int score = 0;
    private String avatar;
    public Player() {
    }
    public Player(String id, String name, String avatar) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
    }


}
