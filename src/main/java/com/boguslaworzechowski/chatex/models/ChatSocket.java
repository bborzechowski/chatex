package com.boguslaworzechowski.chatex.models;

import org.hibernate.validator.constraints.EAN;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@EnableWebSocket
public class ChatSocket extends TextWebSocketHandler implements WebSocketConfigurer {

    private List<UserModel> users = new ArrayList<>();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(this, "/chat").setAllowedOrigins("*");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        users.remove(findUserModelBySession(session));
        UserModel userModel = findUserModelBySession(session);
        sendMessageToAllWithoutMe(userModel, userModel.getNick() + ", opuścił chat");

    }

    private UserModel findUserModelBySession(WebSocketSession session) {
        return users
                .stream()
                .filter(s -> s.getSession().getId().equals(session.getId()))
                .findAny()
                .get();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        users.add(new UserModel(session));
        session.sendMessage(new TextMessage("<server>Twoja pierwsza wiadomość, będzie Twoim nickiem"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UserModel sender = findUserModelBySession(session);
        if (sender.getNick() == null) {
            if (!isNickFree(message.getPayload())) {
                sender.getSession().sendMessage(new TextMessage("Twój nick jest zajęty"));
                return;
            }
            sender.setNick(message.getPayload());
            sender.getSession().sendMessage(new TextMessage("Twój nick został ustawiony"));
            sendMessageToAllWithoutMe(sender, message.getPayload() + ", dołączył do chatu");
            return;
        }
        for (UserModel user : users) {
            user.getSession().sendMessage(new TextMessage(sender.getNick() + ": " + message.getPayload()));
        }

    }

    private Boolean isNickFree(String nick) {
        for (UserModel user : users) {
            if (user.getNick() != null && user.getNick().equals(nick)) {
                return false;
            }
        }
        return true;
    }

    private void sendMessageToAllWithoutMe(UserModel userModel, String message) {
        for (UserModel user : users) {
            if (!user.equals(userModel)) {
                try {
                    user.getSession().sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
