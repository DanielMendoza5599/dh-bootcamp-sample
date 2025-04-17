package com.dharbor.sales.services;

import com.dharbor.sales.clients.*;
import com.dharbor.sales.exceptions.SaleNotCompletedException;
import com.dharbor.sales.model.User;
import com.dharbor.sales.model.dto.NewSaleDto;
import com.dharbor.sales.model.rest.Character;
import com.dharbor.sales.model.rest.ProductReservationRequest;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewSalesService {

    private final UsersFeignClient usersFeignClient;

    private final StockFeignClient stockFeignClient;

    private final NotificationsFeignClient notificationsFeignClient;

    private final RickMortyApiFeignClient rickMortyApiFeignClient;

    @CircuitBreaker(name = "newSaleCB", fallbackMethod = "fallbackNewSale")
    public String newSale(NewSaleDto newSaleDto) throws SaleNotCompletedException {

        User user;

        try {
            user = this.usersFeignClient.findById(newSaleDto.getUserId());
        } catch (FeignException.NotFound notFoundException) {
            throw new SaleNotCompletedException("Sale cant be completed", notFoundException);
        }

        ProductReservationRequest reservationRequest = new ProductReservationRequest();
        reservationRequest.setQuantity(newSaleDto.getQuantity());
        reservationRequest.setProductId(newSaleDto.getProductId());
        String reservationId = this.stockFeignClient.reserve(reservationRequest);

        String notification = this.notificationsFeignClient.sendNotification(newSaleDto.getUserId().toString());

        Character character = this.rickMortyApiFeignClient.findById(2);

        System.out.println(character.getName()+" "+character.getSpecies()+" "+character.getStatus());

        return "New Sale for " + user.getName() + " with reservation id " + reservationId + " and user has been notified " + notification;
    }

    public String fallbackNewSale(NewSaleDto newSaleDto, Throwable t) {
        System.out.println(">>> Fallback ACTIVATED: " + t.getMessage());
        return "Fallback: " + t.getMessage();
    }
}
