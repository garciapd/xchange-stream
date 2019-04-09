package info.bitrich.xchangestream.bitfinex;

import org.knowm.xchange.dto.Order;

/**
 * Created by Daniel Garcia on 09.04.19.
 */
public interface TradeServiceListener {
    void trackOrder(String orderId, Order.OrderStatus orderStatus);
}
