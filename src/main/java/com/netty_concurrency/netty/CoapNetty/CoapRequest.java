package com.netty_concurrency.netty.CoapNetty;

import com.netty_concurrency.netty.CoapNetty.Options.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class CoapRequest extends Coap{
    private static final String NO_REQUEST_TYPE = "msg type %d is not a suitable type for request(only CON and NON)";
    private static final String NO_REQUEST_CODE = "msg code is not a request code";
    private static final String URI_SCHEME = "URI scheme must be set to \"coap\"(but given URI is: %s)";
    private static final String URI_FRAGMENT = "URI must not have a fragment(but given URI is %s)";

    public CoapRequest(int msgType, int msgCode, URI targetUri) throws IllegalArgumentException{
        this(msgType, msgCode, targetUri, false);
    }

    public CoapRequest(int msgType, int msgCode, URI targetUri, boolean useProxy) throws IllegalArgumentException{
        this(msgType, msgCode);
        if (useProxy){

        } else {

        }
    }

    public CoapRequest(int msgType, int msgCode) throws IllegalArgumentException{
        super(msgType, msgCode);

        if (msgType < MsgType.CON || msgType > MsgType.NON){
            throw new IllegalArgumentException(String.format(NO_REQUEST_TYPE, msgType));
        }

        if (!MsgCode.isRequest(msgCode)){
            throw new IllegalArgumentException(String.format(NO_REQUEST_CODE, msgCode));
        }
    }

    private void setProxyURIOption(URI targetUri) throws IllegalArgumentException{
        this.addStringOption(Option.PROXY_URI, targetUri.toString());
    }

    private void setTargetUriOptions(URI targetUri) throws IllegalArgumentException{
        targetUri = targetUri.normalize();

        String scheme = targetUri.getScheme();
        if (scheme == null){
            throw new IllegalArgumentException(String.format(URI_SCHEME, targetUri.toString()));
        }

        scheme = scheme.toLowerCase(Locale.CHINA);
        if (!(scheme.equals("coap"))){
            throw new IllegalArgumentException(String.format(URI_SCHEME, targetUri.toString()));
        }

        if (targetUri.getFragment() != null){
            throw new IllegalArgumentException(String.format(URI_FRAGMENT, targetUri.toString()));
        }

        if (!(OptionValue.isDefaultValue(Option.URI_HOST, targetUri.getHost().getBytes(Coap.CHARSET)))){
            addUriHostOption(targetUri.getHost());
        }

        if (targetUri.getPort() != -1 && targetUri.getPort() != OptionValue.URI_PORT_DEFAULT){
            addUriPortOption(targetUri.getPort());
        }

        addUriPathOptions(targetUri.getPath());
        addUriQueryOptions(targetUri.getQuery());
    }

    private void addUriQueryOptions(String uriQuery) throws IllegalArgumentException{
        if (uriQuery != null){
            for (String queryComponent : uriQuery.split("&")){
                this.addStringOption(Option.URI_QUERY, queryComponent);
            }
        }
    }

    private void addUriPathOptions(String uriPath) throws IllegalArgumentException{
        if (uriPath != null){
            if (uriPath.startsWith("/")){
                uriPath = uriPath.substring(1);
            }

            if ("".equals(uriPath)){
                return;
            }

            for (String pathComponent : uriPath.split("/")){
                this.addStringOption(Option.URI_PATH, pathComponent);
            }
        }
    }

    private void addUriPortOption(int uriPort) throws IllegalArgumentException{
        if (uriPort > 0 && uriPort != OptionValue.URI_PORT_DEFAULT){
            this.addUintOption(Option.URI_PORT, uriPort);
        }
    }

    private void addUriHostOption(String uriHost) throws IllegalArgumentException{
        addStringOption(Option.URI_HOST, uriHost);
    }

    //这里与参考相比函数有更改
    private void setOpaqueOptions(int optionNum, byte[]... etags) throws IllegalArgumentException{
        this.removeOptions(optionNum);
        try{
            for (byte[] etag : etags){
                this.addOpaqueOption(optionNum, etag);
            }
        } catch (IllegalArgumentException e){
            this.removeOptions(optionNum);
            throw e;
        }
    }

    public void setIfMatch(byte[]... etags) throws IllegalArgumentException{
        setOpaqueOptions(Option.IF_MATCH, etags);
    }

    public Set<byte[]> getIfMatch(){
        Set<OptionValue> ifMatchOptionValues = options.get(Option.IF_MATCH);
        Set<byte[]> result = new HashSet<>(ifMatchOptionValues.size());

        for (OptionValue ifMatchOptionValue : ifMatchOptionValues)
            result.add(((OpaqueOptionValue) ifMatchOptionValue).getDecodedValue());

        return result;
    }

    public String getUriHost(){
        if (options.containsKey(Option.URI_HOST))
            return ((StringOptionValue) options.get(Option.URI_HOST).iterator().next()).getDecodedValue();

        return null;
    }

    public void setEtags(byte[]... etags) throws IllegalArgumentException{
        setOpaqueOptions(Option.ETAG, etags);
    }

    public Set<byte[]> getEtags(){
        Set<byte[]> result = new HashSet<>();

        for (OptionValue optionValue : options.get(Option.ETAG))
            result.add(((OpaqueOptionValue) optionValue).getDecodedValue());

        return result;
    }

    public boolean setIfNonMatch(){
        if (options.containsKey(Option.IF_NONE_MATCH))
            return true;

        try{
            this.addEmptyOption(Option.IF_NONE_MATCH);
            return true;
        } catch (IllegalArgumentException e){
            return false;
        }
    }

    public boolean isIfNonMatchSet(){
        return options.containsKey(Option.IF_NONE_MATCH);
    }

    public long getUriPort(){
        if (options.containsKey(Option.URI_PORT))
            return ((UnitOptionValue) options.get(Option.URI_PORT).iterator().next()).getDecodedValue();

        return OptionValue.URI_PORT_DEFAULT;
    }

    public String getUriPath(){
        String result = "/";

        Iterator<OptionValue> iterator = options.get(Option.URI_PATH).iterator();
        if (iterator.hasNext())
            result += ((StringOptionValue) iterator.next()).getDecodedValue();

        while (iterator.hasNext())
            result += ("/" + ((StringOptionValue) iterator.next()).getDecodedValue());

        return result;
    }

    public String getUriQuery(){
        String result = "";

        if (options.containsKey(Option.URI_QUERY)){
            Iterator<OptionValue> iterator = options.get(Option.URI_QUERY).iterator();
            result += (((StringOptionValue) iterator.next()).getDecodedValue());

            while (iterator.hasNext())
                result += ("&" + ((StringOptionValue) iterator.next()).getDecodedValue());
        }

        return result;
    }

    public String getUriQueryParameterValue(String parameter) {
        if (!parameter.endsWith("="))
            parameter += "=";

        for(OptionValue optionValue : options.get(Option.URI_QUERY)) {
            String value = ((StringOptionValue) optionValue).getDecodedValue();
            if (value.startsWith(parameter))
                return value.substring(parameter.length());
        }

        return null;
    }

    public void setAccept(long... contentFormatNumbers) throws IllegalArgumentException {
        options.removeAll(Option.ACCEPT);
        try{
            for(long contentFormatNumber : contentFormatNumbers)
                this.addUintOption(Option.ACCEPT, contentFormatNumber);
        }
        catch (IllegalArgumentException e) {
            options.removeAll(Option.ACCEPT);
            throw e;
        }
    }

    public Set<Long> getAcceptedContentFormats() {

        Set<Long> result = new HashSet<>();
        for(OptionValue optionValue : options.get(Option.ACCEPT))
            result.add(((UnitOptionValue) optionValue).getDecodedValue());

        return result;
    }

    public URI getProxyURI() throws URISyntaxException {
        if (options.containsKey(Option.PROXY_URI)) {
            OptionValue proxyUriOptionValue = options.get(Option.PROXY_URI).iterator().next();
            return new URI(((StringOptionValue) proxyUriOptionValue).getDecodedValue());
        }

        if (options.get(Option.PROXY_SCHEME).size() == 1) {
            OptionValue proxySchemeOptionValue = options.get(Option.PROXY_SCHEME).iterator().next();
            String scheme = ((StringOptionValue) proxySchemeOptionValue).getDecodedValue();
            String uriHost = getUriHost();
            OptionValue uriPortOptionValue = options.get(Option.URI_PORT).iterator().next();
            int uriPort = ((UnitOptionValue) uriPortOptionValue).getDecodedValue().intValue();
            String uriPath = getUriPath();
            String uriQuery = getUriQuery();

            return new URI(scheme, null, uriHost, uriPort == OptionValue.URI_PORT_DEFAULT ? -1 : uriPort, uriPath,
                    uriQuery, null);
        }

        return null;
    }

    public boolean isObservationRequest() {
        return(!options.get(Option.OBSERVE).isEmpty());
    }
}
