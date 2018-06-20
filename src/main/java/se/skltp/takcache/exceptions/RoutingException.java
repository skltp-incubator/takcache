package se.skltp.takcache.exceptions;

public class RoutingException extends Exception{

    private RoutingFailReason failReason;

    public RoutingException(RoutingFailReason failReason, String message) {
        super(message);
        this.failReason = failReason;
    }

    public RoutingFailReason getFailReason() {
        return failReason;
    }

}
