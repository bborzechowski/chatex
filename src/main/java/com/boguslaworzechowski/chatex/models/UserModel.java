package com.boguslaworzechowski.chatex.models;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class UserModel {

    private WebSocketSession session;
    private String nick;


    public UserModel(WebSocketSession session){
        this.session = session;
    }
}