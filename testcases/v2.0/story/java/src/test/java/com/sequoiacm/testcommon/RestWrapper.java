package com.sequoiacm.testcommon;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.sequoiacm.client.element.ScmServiceInstance;

public class RestWrapper {
    private static RestTemplate rest;
    private static Random random = new Random();

    static {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout( 10000 );
        factory.setConnectTimeout( 10000 );
        factory.setBufferRequestBody( false );
        factory.setReadTimeout( 30000 );
        rest = new RestTemplate( factory );
    }

    private HttpHeaders requestHeaders;
    private String url;
    private HttpMethod requestMethod;
    private HttpEntity< ? > requestEntity;
    private Class< ? > responseType;
    private Object uriVariables[];
    private MultiValueMap< Object, Object > param;
    private String version = "v1";
    private String addr;
    private String api;
    private String sessionId;
    private String serverType;
    private InputStreamResource resource = null;

    public RestWrapper() {
        super();
        this.requestHeaders = new HttpHeaders();
        this.param = new LinkedMultiValueMap<>();
        this.serverType = "content-server";
    }

    public RestWrapper reset() {
        this.url = null;
        this.requestMethod = null;
        this.requestEntity = null;
        this.uriVariables = null;
        this.resource = null;
        this.requestHeaders.clear();
        this.requestHeaders.set( "x-auth-token", this.sessionId );
        this.param = new LinkedMultiValueMap<>();
        this.version = "v1";
        return this;
    }

    public RestWrapper setRequestMethod( HttpMethod method ) {
        this.requestMethod = method;
        return this;
    }

    public RestWrapper setInputStream( InputStream is ) {
        this.resource = new InputStreamResource( is );
        return this;
    }

    public void connect( String addr, String user, String passwd ) {
        this.addr = addr;
        Map< String, String > response = this.setApi( "login" )
                .setRequestMethod( HttpMethod.POST )
                .setParameter( "username", user )
                .setParameter( "password", passwd )
                .setResponseType( String.class ).exec().getHeaders()
                .toSingleValueMap();
        this.sessionId = response.get( "x-auth-token" );
        requestHeaders.add( "x-auth-token", this.sessionId );
        this.reset();
    }

    public void disconnect() throws Exception {
        if ( this.sessionId != null ) {
            int statuscode = this.setApi( "logout" )
                    .setRequestMethod( HttpMethod.POST )
                    .setResponseType( String.class ).exec()
                    .getStatusCodeValue();
            if ( statuscode != 200 ) {
                throw new Exception(
                        "logout is fail,sessionId = " + sessionId );
            }
        }
    }

    public RestWrapper setApi( String api ) {
        this.api = api;
        return this;
    }

    private RestWrapper setUrl() {
        if ( this.serverType.equals( "content-server" ) ) {
            List< String > gateWayList = TestScmBase.gateWayList;
            String gateWay = gateWayList
                    .get( random.nextInt( gateWayList.size() ) );
            if ( this.api.equals( "login" ) ) {
                this.url = "http://" + gateWay + "/" + this.api;
            } else if ( this.api.equals( "logout" ) ) {
                this.url = "http://" + gateWay + "/auth/" + this.api;
            } else {
                this.url = "http://" + gateWay + "/" + this.addr + "/api/"
                        + this.getVersion() + "/" + this.api;
            }
        } else if ( this.serverType.equals( "schedule-server" ) ) {
            List< ScmServiceInstance > schedules = ScmInfo.getScheServerList();
            if ( !schedules.isEmpty() ) {
                ScmServiceInstance schedule = schedules
                        .get( random.nextInt( schedules.size() ) );
                this.url = "http://" + schedule.getIp() + ":"
                        + schedule.getPort() + "/api/" + this.getVersion() + "/"
                        + this.api;
            }
        } else if ( this.serverType.equals( "auth-server" ) ) {
            List< ScmServiceInstance > authServers = ScmInfo
                    .getAuthServerList();
            if ( !authServers.isEmpty() ) {
                ScmServiceInstance authserver = authServers
                        .get( random.nextInt( authServers.size() ) );
                this.url = "http://" + authserver.getIp() + ":"
                        + authserver.getPort() + "/api/" + this.getVersion()
                        + "/" + this.api;
            }
        }
        return this;
    }

    public RestWrapper setRequestHeaders( String headerName,
            String headerValue ) {
        requestHeaders.add( headerName, headerValue );
        return this;
    }

    public RestWrapper setParameter( Object key, Object value ) {
        param.set( key, value );
        return this;
    }

    public RestWrapper setUriVariables( Object[] uriVariables ) {
        this.uriVariables = uriVariables;
        return this;
    }

    public RestWrapper setResponseType( Class< ? > responseType ) {
        this.responseType = responseType;
        return this;
    }

    public ResponseEntity< ? > exec() {
        this.setUrl();
        if ( null != resource ) {
            requestEntity = new HttpEntity<>( resource, this.requestHeaders );
        } else {
            requestEntity = new HttpEntity<>( this.param, this.requestHeaders );
        }

        ResponseEntity< ? > response = null;
        try {
            if ( this.uriVariables != null ) {
                response = rest.exchange( this.url, this.requestMethod,
                        this.requestEntity, this.responseType,
                        this.uriVariables );
            } else {
                response = rest.exchange( this.url, this.requestMethod,
                        this.requestEntity, this.responseType );
            }
        } catch ( HttpClientErrorException e ) {
            System.out.println(
                    e.getResponseHeaders().getFirst( "X-SCM-ERROR" ) );
            throw e;
        } finally {
            this.reset();
        }
        return response;
    }

    private String getVersion() {
        return this.version;
    }

    public RestWrapper setVersion( String version ) {
        this.version = version;
        return this;
    }

    /**
     * @return the serverType
     */
    public String getServerType() {
        return serverType;
    }

    /**
     * @param serverType
     *            the serverType to set
     */
    public RestWrapper setServerType( String serverType ) {
        this.serverType = serverType;
        return this;
    }
}
