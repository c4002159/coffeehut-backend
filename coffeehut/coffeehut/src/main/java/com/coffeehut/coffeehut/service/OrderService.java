package com.coffeehut.coffeehut.twoZhouzheng;

import com.coffeehut.coffeehut.dto.OrderDetailDTO;
import com.coffeehut.coffeehut.dto.OrderWithItemsDTO;
import com.coffeehut.coffeehut.model.Item;
import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.model.OrderItem;
import com.coffeehut.coffeehut.repository.ItemRepository;
import com.coffeehut.coffeehut.repository.OrderItemRepository;
import com.coffeehut.coffeehut.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    // ==================== 原有方法（保留） ====================
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public List<Order> getOrdersByCustomer(String name) {
        return orderRepository.findByCustomerName(name);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public Order createReorder(ReorderRequest request) {
        Order newOrder = new Order();
        newOrder.setCustomerName(request.getCustomerName());
        newOrder.setCustomerPhone(request.getCustomerPhone());
        newOrder.setTotalPrice(request.getTotalPrice());
        newOrder.setStatus("pending");
        newOrder.setIsArchived(false);
        newOrder.setCreatedAt(LocalDateTime.now());
        if (request.getPickupTime() != null) {
            newOrder.setPickupTime(request.getPickupTime());
        } else {
            newOrder.setPickupTime(LocalDateTime.now().plusMinutes(10));
        }
        Order saved = orderRepository.save(newOrder);
        if (request.getItems() != null) {
            for (OrderItem item : request.getItems()) {
                OrderItem newItem = new OrderItem();
                newItem.setOrderId(saved.getId());
                newItem.setItemId(item.getItemId());
                newItem.setSize(item.getSize());
                newItem.setQuantity(item.getQuantity());
                newItem.setSubtotal(item.getSubtotal());
                orderItemRepository.save(newItem);
            }
        }
        return saved;
    }

    // ==================== 新增员工端方法 ====================
    /**
     * 获取所有活跃订单（未归档），并附带每个订单的商品摘要（名称、数量、规格）
     */
    public List<OrderWithItemsDTO> getActiveOrdersWithItems() {
        List<Order> activeOrders = orderRepository.findByIsArchivedFalse();
        return activeOrders.stream().map(order -> {
            OrderWithItemsDTO dto = new OrderWithItemsDTO();
            dto.setOrder(order);
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            List<OrderWithItemsDTO.OrderItemSummary> summaries = items.stream().map(item -> {
                OrderWithItemsDTO.OrderItemSummary summary = new OrderWithItemsDTO.OrderItemSummary();
                String itemName = itemRepository.findById(item.getItemId())
                        .map(Item::getName)
                        .orElse("Unknown");
                summary.setName(itemName);
                summary.setQuantity(item.getQuantity());
                summary.setSize(item.getSize());
                return summary;
            }).collect(Collectors.toList());
            dto.setItems(summaries);
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 更新订单状态，若状态变为 collected 则自动归档
     */
    public Order updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            order.setStatus(status);
            if ("collected".equals(status)) {
                order.setIsArchived(true);
            }
            return orderRepository.save(order);
        }
        return null;
    }

    /**
     * 获取订单详情（包含完整的商品列表及商品名称）
     */
    public OrderDetailDTO getOrderDetail(Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return null;
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        List<OrderDetailDTO.OrderItemWithName> itemsWithName = items.stream().map(item -> {
            OrderDetailDTO.OrderItemWithName dtoItem = new OrderDetailDTO.OrderItemWithName();
            dtoItem.setId(item.getId());
            dtoItem.setOrderId(item.getOrderId());
            dtoItem.setItemId(item.getItemId());
            dtoItem.setSize(item.getSize());
            dtoItem.setQuantity(item.getQuantity());
            dtoItem.setSubtotal(item.getSubtotal());
            String itemName = itemRepository.findById(item.getItemId())
                    .map(Item::getName)
                    .orElse("Unknown");
            dtoItem.setName(itemName);
            return dtoItem;
        }).collect(Collectors.toList());
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrder(order);
        dto.setItems(itemsWithName);
        return dto;
    }

    /**
     * 获取归档订单，按 TODAY / YESTERDAY / LAST_7_DAYS 分组
     */
    public Map<String, List<Order>> getArchivedOrdersGrouped() {
        List<Order> archived = orderRepository.findByIsArchivedTrueOrderByCreatedAtDesc();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime weekAgo = todayStart.minusDays(7);

        Map<String, List<Order>> result = new HashMap<>();
        result.put("TODAY", archived.stream()
                .filter(o -> o.getCreatedAt().isAfter(todayStart))
                .collect(Collectors.toList()));
        result.put("YESTERDAY", archived.stream()
                .filter(o -> o.getCreatedAt().isBefore(todayStart) && o.getCreatedAt().isAfter(yesterdayStart))
                .collect(Collectors.toList()));
        result.put("LAST_7_DAYS", archived.stream()
                .filter(o -> o.getCreatedAt().isBefore(yesterdayStart) && o.getCreatedAt().isAfter(weekAgo))
                .collect(Collectors.toList()));
        return result;
    }

    /**
     * 搜索归档订单（按订单号或客户名）
     */
    public List<Order> searchArchivedOrders(String keyword) {
        return orderRepository.findByIsArchivedTrue().stream()
                .filter(o -> o.getOrderNumber().toLowerCase().contains(keyword.toLowerCase())
                        || o.getCustomerName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
}