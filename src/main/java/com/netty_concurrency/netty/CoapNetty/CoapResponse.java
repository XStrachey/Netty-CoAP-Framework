package com.netty_concurrency.netty.CoapNetty;

import com.netty_concurrency.netty.CoapNetty.Options.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

public class CoapResponse extends Coap {

    private static final String NO_ERROR_CODE = "Code %s is no error code";

    public CoapResponse(int msgType, int msgCode) throws IllegalArgumentException{
        super(msgType, msgCode);
        if (!MsgCode.isResponse(msgCode))
            throw new IllegalArgumentException("Msg code" + msgCode + "is no response code");
    }

    public static CoapResponse createErrorResponse(int msgType, int msgCode, String content) throws IllegalArgumentException{
        if (!MsgCode.isErrorMsg(msgCode)){
            throw new IllegalArgumentException(String.format(NO_ERROR_CODE, MsgCode.asString(msgCode)));
        }

        CoapResponse errorResponse = new CoapResponse(msgType, msgCode);
        errorResponse.setContent(content.getBytes(Coap.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

        return errorResponse;
    }

    public static CoapResponse createErrorResponse(int messageType, int messageCode, Throwable throwable)
            throws IllegalArgumentException{

        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return createErrorResponse(messageType, messageCode, stringWriter.toString());
    }

    public boolean isErrorResponse() {
        return MsgCode.isErrorMsg(this.getMsgCode());
    }

    public void setEtag(byte[] etag) throws IllegalArgumentException {
        this.addOpaqueOption(Option.ETAG, etag);
    }

    public byte[] getEtag() {
        if (options.containsKey(Option.ETAG)) {
            return ((OpaqueOptionValue) options.get(Option.ETAG).iterator().next()).getDecodedValue();
        } else {
            return null;
        }
    }

    public void setLocationURI(URI locationURI) throws IllegalArgumentException {

        options.removeAll(Option.LOCATION_PATH);
        options.removeAll(Option.LOCATION_QUERY);

        String locationPath = locationURI.getRawPath();
        String locationQuery = locationURI.getRawQuery();

        try{
            if (locationPath != null) {
                //Path must not start with "/" to be further processed
                if (locationPath.startsWith("/"))
                    locationPath = locationPath.substring(1);

                for(String pathComponent : locationPath.split("/"))
                    this.addStringOption(Option.LOCATION_PATH, pathComponent);
            }

            if (locationQuery != null) {
                for(String queryComponent : locationQuery.split("&"))
                    this.addStringOption(Option.LOCATION_QUERY, queryComponent);
            }
        } catch(IllegalArgumentException ex) {
            options.removeAll(Option.LOCATION_PATH);
            options.removeAll(Option.LOCATION_QUERY);
            throw ex;
        }
    }

    public URI getLocationURI() throws URISyntaxException {

        //Reconstruct path
        StringBuilder locationPath = new StringBuilder();

        if (options.containsKey(Option.LOCATION_PATH)) {
            for (OptionValue optionValue : options.get(Option.LOCATION_PATH))
                locationPath.append("/").append(((StringOptionValue) optionValue).getDecodedValue());
        }

        //Reconstruct query
        StringBuilder locationQuery = new StringBuilder();

        if (options.containsKey(Option.LOCATION_QUERY)) {
            Iterator<OptionValue> queryComponentIterator = options.get(Option.LOCATION_QUERY).iterator();
            locationQuery.append(((StringOptionValue) ((Iterator) queryComponentIterator).next()).getDecodedValue());
            while(queryComponentIterator.hasNext())
                locationQuery.append("&")
                        .append(((StringOptionValue) queryComponentIterator.next()).getDecodedValue());
        }

        if (locationPath.length() == 0 && locationQuery.length() == 0)
            return null;

        return new URI(null, null, null, (int) UnitOptionValue.UNDEFINED, locationPath.toString(),
                locationQuery.toString(), null);
    }

    public void setMaxAge(long maxAge) {
        try {
            this.options.removeAll(Option.MAX_AGE);
            this.addUintOption(Option.MAX_AGE, maxAge);
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    public long getMaxAge() {
        if (options.containsKey(Option.MAX_AGE)) {
            return ((UnitOptionValue) options.get(Option.MAX_AGE).iterator().next()).getDecodedValue();
        } else {
            return OptionValue.MAX_AGE_DEFAULT;
        }
    }

    public void setObserve() {
    }

    public void isUpdateNotification(){
    }

}
