package com.netty_concurrency.netty.Application;

import com.netty_concurrency.netty.CoapNetty.Coap;

import java.net.InetSocketAddress;

public abstract class ClientCallback {
    public abstract void processCoapResponse(Coap coapResponse);

    public boolean continueObservation(){
        return false;
    }

    public void processRemoteSocketChanged(InetSocketAddress remoteSocket, InetSocketAddress previous){

    }

    public void processTransmissionTimeout(){

    }

    public void processReset(){

    }

    public void processRetransmission(){

    }

    public void processResponseBlockReceived(long receivedLength, long expectedLength){

    }

    public void processBlockwiseResponseTransferFailed(){

    }

    public void processEmptyAcknowledgement(){

    }

    public void processMiscellaneousError(String description){

    }

    public void processMsgIDAssignment(int msgID){

    }

    public void processNoMsgIDAvailable(){

    }
}
