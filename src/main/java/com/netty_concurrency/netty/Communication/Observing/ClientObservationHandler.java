package com.netty_concurrency.netty.Communication.Observing;

import com.google.common.collect.Table;
import com.netty_concurrency.netty.CoapNetty.Token;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientObservationHandler {
    private Table<InetSocketAddress, Token, ResourceStatusAge> observations;
    private ReentrantReadWriteLock lock;


}
