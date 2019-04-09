package info.bitrich.xchangestream.bitfinex;

import java.io.IOException;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.bitfinex.v1.service.BitfinexTradeService;
import org.knowm.xchange.dto.trade.MarketOrder;

/**
 * Created by Daniel Garcia on 09.04.19.
 */
public class BitfinexTradeServiceOrderTracker extends BitfinexTradeService {

    protected TradeServiceListener listener;

    public BitfinexTradeServiceOrderTracker(Exchange exchange, TradeServiceListener listener) {
        super(exchange);
        this.listener = listener;
    }

    @Override
    public String placeMarketOrder(MarketOrder marketOrder) throws IOException {
        String orderId = super.placeMarketOrder(marketOrder);
        listener.trackOrder(orderId, marketOrder.getStatus());
        return orderId;
    }

    //TODO add other operations
}
