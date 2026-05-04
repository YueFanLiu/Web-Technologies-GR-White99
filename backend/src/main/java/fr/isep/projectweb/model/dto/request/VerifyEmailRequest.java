package fr.isep.projectweb.model.dto.request;

public class VerifyEmailRequest {

    private String tokenHash;
    private String type;

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
