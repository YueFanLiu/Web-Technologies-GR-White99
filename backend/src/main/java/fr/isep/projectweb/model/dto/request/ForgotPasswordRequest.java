package fr.isep.projectweb.model.dto.request;

public class ForgotPasswordRequest {

    private String email;
    private String redirectTo;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRedirectTo() {
        return redirectTo;
    }

    public void setRedirectTo(String redirectTo) {
        this.redirectTo = redirectTo;
    }
}
