package com.skywilling.cn.connection.service;

import com.alibaba.fastjson.JSONObject;
import com.skywilling.cn.connection.model.ACK;
import com.skywilling.cn.common.enums.TypeField;
import com.skywilling.cn.connection.infrastructure.client.ClientService;
import com.skywilling.cn.connection.model.Packet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Service
public class RequestSender {
    @Autowired
    ClientService clientService;

    static class PacketConsumer implements BiConsumer<Packet, Throwable> {

        CompletableFuture<Boolean> thenFuture;

        PacketConsumer(CompletableFuture<Boolean> thenFuture) {
            this.thenFuture = thenFuture;
        }

        @Override
        public void accept(Packet packet, Throwable throwable) {
            if (throwable != null) {
                thenFuture.completeExceptionally(throwable);
            } else {
                if (packet.getAck() == ACK.SUCCESS.getCode()) {
                    thenFuture.complete(true);
                } else {
                    thenFuture.complete(false);
                }
            }
        }
    }

    public CompletableFuture<Boolean> sendRequest(String vin, TypeField typeField, JSONObject body) {

        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        Packet.Builder builder = new Packet.Builder();
        Packet packet = builder.buildRequest(vin, typeField).buildBody(body)
                .build();
        CompletableFuture<Packet> respFuture = clientService.sendRequest(packet);
        if (respFuture == null) {
            throw new NullPointerException();
        }
        respFuture.whenComplete(new PacketConsumer(resultFuture));
        return resultFuture;
    }


}
