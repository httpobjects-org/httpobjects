package org.httpobjects.header.request;

import org.httpobjects.header.request.credentials.BasicCredentials;
import org.httpobjects.header.response.WWWAuthenticateField;
import org.httpobjects.impl.Base64;

import java.util.StringTokenizer;

public class AuthorizationValue {

    public static AuthorizationValue parse(String s) throws ParsingException {
        String fValue = s.trim();
        StringTokenizer tokens = new StringTokenizer(fValue);

        if (tokens.countTokens() < 2)
            throw new ParsingException(ParsingException.Failure.MISSING_SCHEME);

        WWWAuthenticateField.Method method;
        try {
            method = WWWAuthenticateField.Method.valueOf(tokens.nextToken());
        } catch (IllegalArgumentException ex) {
            throw new ParsingException(ParsingException.Failure.UNSUPPORTED_SCHEME);
        }

        return new AuthorizationValue(method, tokens.nextToken());
    }

    private final WWWAuthenticateField.Method method;
    private final String rawCredentials;
    public AuthorizationValue(WWWAuthenticateField.Method method, String rawCredentials) {
        super();
        this.method = method;
        this.rawCredentials = rawCredentials;
    }
    public WWWAuthenticateField.Method method() {
        return method;
    }
    public String rawCredentials() {
        return rawCredentials;
    }

    public String credentialsString(){
        return new String(Base64.decode(rawCredentials));
    }

    public BasicCredentials basicCredentials(){
        if(this.method == WWWAuthenticateField.Method.Basic){
            return BasicCredentials.parse(credentialsString());
        }else{
            return null;
        }
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "(\"" + method + " " + rawCredentials + "\")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthorizationValue that = (AuthorizationValue) o;

        return this.toString().equals(that.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public static class ParsingException extends RuntimeException {
        public enum Failure {
            MISSING_SCHEME("missing authorization scheme"),
            UNSUPPORTED_SCHEME("unsupported authorization scheme");

            private final String message;

            Failure(String message) {
                this.message = message;
            }

            public String getMessage() {
                return message;
            }
        }

        private final Failure failure;

        public ParsingException(Failure failure) {
            super(failure.getMessage());
            this.failure = failure;
        }

        public Failure getFailure() {
            return failure;
        }
    }
}
