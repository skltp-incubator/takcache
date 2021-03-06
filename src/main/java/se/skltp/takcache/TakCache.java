package se.skltp.takcache;

import se.skltp.takcache.exceptions.RoutingException;

import java.util.List;

public interface TakCache {
    public TakCacheLog refresh();

    public TakCacheLog refresh(String tjanstegranssnitt);

    public boolean isAuthorized(String senderId, String tjanstegranssnitt, String receiverAddress);

    public List<RoutingInfo> getRoutingInfo(String tjanstegranssnitt, String receiverAddress);

    public String getRoutingAddress(String tjanstegranssnitt, String receiverAddress, String rivProfile) throws RoutingException;
}
