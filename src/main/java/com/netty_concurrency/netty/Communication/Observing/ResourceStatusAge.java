package com.netty_concurrency.netty.Communication.Observing;

public class ResourceStatusAge {
    public static final long MODULUS = (long)Math.pow(2,24);
    private static final long THRESHOLD = (long)Math.pow(2,23);

    private long sequenceNum;
    private long timestamp;

    public ResourceStatusAge(long sequenceNum, long timestamp){
        this.sequenceNum = sequenceNum;
        this.timestamp = timestamp;
    }

    public static boolean isReceivedStatusNewer(ResourceStatusAge latest, ResourceStatusAge received){
        if (latest.sequenceNum < received.sequenceNum && received.sequenceNum - latest.sequenceNum < THRESHOLD) {
            return true;
        }

        if (latest.sequenceNum > received.sequenceNum && latest.sequenceNum - received.sequenceNum > THRESHOLD) {
            return true;
        }

        if (received.timestamp > latest.timestamp + 128000L) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "STATUS AGE (Sequence No: " + this.sequenceNum + ", Reception Timestamp: " + this.timestamp + ")";
    }
}
