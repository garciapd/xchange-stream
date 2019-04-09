package info.bitrich.xchangestream.bitfinex;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthOrder;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthPreTrade;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthTrade;
import info.bitrich.xchangestream.core.StreamingTradeService;
import io.reactivex.Observable;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.exceptions.ExchangeSecurityException;

/**
 * Created by Lukas Zaoralek on 7.11.17.
 */
public class BitfinexStreamingTradeService implements StreamingTradeService, TradeServiceListener {

    private final BitfinexStreamingService service;
    private Cache<String, Order.OrderStatus> orderTrack = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .removalListener(notification -> {

                if (!notification.wasEvicted() && isRefreshableStatus((Order.OrderStatus) notification.getValue())) {
                    //TODO schedule a fetch order
                }
            })
            .build();

    public BitfinexStreamingTradeService(BitfinexStreamingService service) {
        this.service = service;
    }

    public Observable<Order> getOrderChanges() {
        return getRawAuthenticatedOrders()
                .filter(o -> o.getId() != 0)
                .map(BitfinexStreamingAdapters::adaptOrder)
                .doOnNext(o -> {

                    //TODO check all valid status that make the order to be finished
                    if (orderTrack.asMap().containsKey(o.getId()) && !isRefreshableStatus(o.getStatus())) {
                        orderTrack.invalidate(o.getId());
                    }

                    service.scheduleCalculatedBalanceFetch(o.getCurrencyPair().base.getCurrencyCode());
                    service.scheduleCalculatedBalanceFetch(o.getCurrencyPair().counter.getCurrencyCode());
                });
    }

    @Override
    public Observable<Order> getOrderChanges(CurrencyPair currencyPair, Object... args) {
        return getOrderChanges()
                .filter(o -> currencyPair.equals(o.getCurrencyPair()));
    }

    /**
     * Gets a stream of all user trades to which we are subscribed.
     *
     * @return The stream of user trades.
     */
    public Observable<UserTrade> getUserTrades() {
        return getRawAuthenticatedTrades()
                .filter(o -> o.getId() != 0)
                .map(BitfinexStreamingAdapters::adaptUserTrade)
                .doOnNext(t -> {
                    service.scheduleCalculatedBalanceFetch(t.getCurrencyPair().base.getCurrencyCode());
                    service.scheduleCalculatedBalanceFetch(t.getCurrencyPair().counter.getCurrencyCode());
                });
    }

    @Override
    public Observable<UserTrade> getUserTrades(CurrencyPair currencyPair, Object... args) {
        return getUserTrades()
                .filter(t -> currencyPair.equals(t.getCurrencyPair()));
    }

    public Observable<BitfinexWebSocketAuthOrder> getRawAuthenticatedOrders() {
        return withAuthenticatedService(BitfinexStreamingService::getAuthenticatedOrders);
    }

    public Observable<BitfinexWebSocketAuthPreTrade> getRawAuthenticatedPreTrades() {
        return withAuthenticatedService(BitfinexStreamingService::getAuthenticatedPreTrades);
    }

    public Observable<BitfinexWebSocketAuthTrade> getRawAuthenticatedTrades() {
        return withAuthenticatedService(BitfinexStreamingService::getAuthenticatedTrades);
    }

    private <T> Observable<T> withAuthenticatedService(Function<BitfinexStreamingService, Observable<T>> serviceConsumer) {
        if (!service.isAuthenticated()) {
            throw new ExchangeSecurityException("Not authenticated");
        }
        return serviceConsumer.apply(service);
    }

    @Override
    public void trackOrder(String orderId, Order.OrderStatus orderStatus) {
        orderTrack.put(orderId, orderStatus);
    }

    private boolean isRefreshableStatus(Order.OrderStatus orderStatus) {
        //TODO improve and set all the status
        return !orderStatus.equals(Order.OrderStatus.FILLED);
    }
}
