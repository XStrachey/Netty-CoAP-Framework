package com.netty_concurrency.netty.Application;

import com.netty_concurrency.netty.CoapNetty.Coap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleClientCallback extends ClientCallback {
    private AtomicBoolean responseReceived;
    private AtomicInteger transmissionCounter;
    private AtomicBoolean timeOut;

    public SimpleClientCallback(){
        this.responseReceived = new AtomicBoolean(false);
        this.transmissionCounter = new AtomicInteger(0);
        this.timeOut = new AtomicBoolean(false);
    }

    @Override
    public void processCoapResponse(Coap coapResponse){
        responseReceived.set(true);
    }

    public int getResponseCount(){
        return this.responseReceived.get() ? 1 : 0;
    }

    @Override
    public void processRetransmission(){
        int value = transmissionCounter.incrementAndGet();
    }

    @Override
    public void processTransmissionTimeout(){
        timeOut.set(true);
    }

    @Override
    public void processResponseBlockReceived(long receivedLength, long expectedLength){

    }

    public boolean isTimeOut(){
        return timeOut.get();
    }
}
