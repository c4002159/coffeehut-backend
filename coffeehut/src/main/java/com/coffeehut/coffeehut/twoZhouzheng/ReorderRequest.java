package com.coffeehut.coffeehut.two;

import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.model.OrderItem;

import java.util.List;

public class ReorderRequest {

    private Order order;
    private List<OrderItem> items;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}