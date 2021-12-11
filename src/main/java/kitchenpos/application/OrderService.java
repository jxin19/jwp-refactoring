package kitchenpos.application;

import kitchenpos.domain.menu.Menu;
import kitchenpos.domain.menu.MenuRepository;
import kitchenpos.domain.order.Orders;
import kitchenpos.domain.order.OrderLineItem;
import kitchenpos.domain.order.OrderLineItemRepository;
import kitchenpos.domain.order.OrdersRepository;
import kitchenpos.domain.order.OrderStatus;
import kitchenpos.domain.order.OrderTable;
import kitchenpos.domain.order.OrderTableRepository;
import kitchenpos.dto.OrderDto;
import kitchenpos.dto.OrderLineItemDto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final MenuRepository menuRepository;
    private final OrdersRepository orderRepository;
    private final OrderLineItemRepository orderLineItemRepository;
    private final OrderTableRepository orderTableRepository;

    public OrderService(
            final MenuRepository menuRepository,
            final OrdersRepository orderRepository,
            final OrderLineItemRepository orderLineItemRepository,
            final OrderTableRepository orderTableRepository
    ) {
        this.menuRepository = menuRepository;
        this.orderRepository = orderRepository;
        this.orderLineItemRepository = orderLineItemRepository;
        this.orderTableRepository = orderTableRepository;
    }

    @Transactional
    public Orders create(final OrderDto order) {
        final OrderTable orderTable = orderTableRepository.findById(order.getOrderTableId()).orElseThrow(IllegalArgumentException::new);
        final List<OrderLineItemDto> orderLineItems = order.getOrderLineItems();

        validationOfCreate(orderTable, orderLineItems);

        final Orders savedOrder = orderRepository.save(Orders.of(orderTable, OrderStatus.COOKING, LocalDateTime.now(), null));
        final List<OrderLineItem> savedOrderLineItems = saveOrderLineItem(savedOrder, orderLineItems);

        savedOrder.changeOrderLineItems(savedOrderLineItems);

        return savedOrder;
    }

    private List<OrderLineItem> saveOrderLineItem(final Orders order, final List<OrderLineItemDto> orderLineItems) {
        final List<OrderLineItem> savedOrderLineItems = new ArrayList<>();

        for (final OrderLineItemDto orderLineItem : orderLineItems) {
            Menu menu = menuRepository.findById(orderLineItem.getMenuId()).orElseThrow(IllegalArgumentException::new);
            savedOrderLineItems.add(orderLineItemRepository.save(OrderLineItem.of(order, menu, orderLineItem.getQuantity())));
        }

        return savedOrderLineItems;
    }

    private void validationOfCreate(final OrderTable orderTable, final List<OrderLineItemDto> orderLineItems) {
        checkEmptyOfOrderLineItems(orderLineItems);
        checkExistOfMenu(orderLineItems);
        checkEmptyTable(orderTable);
    }

    private void checkEmptyTable(final OrderTable orderTable) {
        if (orderTable.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    private void checkExistOfMenu(final List<OrderLineItemDto> orderLineItems) {
        final List<Long> menuIds = orderLineItems.stream()
                                                    .map(OrderLineItemDto::getMenuId)
                                                    .collect(Collectors.toList());

        if (orderLineItems.size() != menuRepository.countByIdIn(menuIds)) {
            throw new IllegalArgumentException();
        }
    }

    private void checkEmptyOfOrderLineItems(final List<OrderLineItemDto> orderLineItems) {
        if (CollectionUtils.isEmpty(orderLineItems)) {
            throw new IllegalArgumentException();
        }
    }

    @Transactional(readOnly = true)
    public List<Orders> list() {
        final List<Orders> orders = orderRepository.findAll();

        for (final Orders order : orders) {
            order.changeOrderLineItems(orderLineItemRepository.findAllByOrderId(order.getId()));
        }

        return orders;
    }

    @Transactional
    public Orders changeOrderStatus(final Long orderId, final OrderDto order) {
        final Orders savedOrder = orderRepository.findById(orderId)
                                                .orElseThrow(IllegalArgumentException::new);

        validateionOfChageOrderStatus(savedOrder);

        savedOrder.changeOrderStatus(OrderStatus.valueOf(order.getOrderStatus()));

        orderRepository.save(savedOrder);

        savedOrder.changeOrderLineItems(orderLineItemRepository.findAllByOrderId(orderId));

        return savedOrder;
    }

    private void validateionOfChageOrderStatus(final Orders savedOrder) {
        if (OrderStatus.COMPLETION.equals(savedOrder.getOrderStatus())) {
            throw new IllegalArgumentException();
        }
    }
}
