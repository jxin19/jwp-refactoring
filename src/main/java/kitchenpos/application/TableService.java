package kitchenpos.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kitchenpos.domain.order.OrdersRepository;
import kitchenpos.domain.order.OrderStatus;
import kitchenpos.domain.order.OrderTable;
import kitchenpos.domain.order.OrderTableRepository;
import kitchenpos.dto.OrderTableDto;

import java.util.Arrays;
import java.util.List;

@Service
public class TableService {
    private final OrdersRepository orderRepository;
    private final OrderTableRepository orderTableRepository;

    public TableService(final OrdersRepository orderRepository, final OrderTableRepository orderTableRepository) {
        this.orderRepository = orderRepository;
        this.orderTableRepository = orderTableRepository;
    }

    @Transactional
    public OrderTableDto create(final OrderTableDto orderTable) {
        return OrderTableDto.of(orderTableRepository.save(OrderTable.of(orderTable.getNumberOfGuests(), orderTable.getEmpty())));
    }

    @Transactional(readOnly = true)
    public List<OrderTable> list() {
        return orderTableRepository.findAll();
    }

    @Transactional
    public OrderTable changeEmpty(final Long orderTableId, final OrderTableDto orderTable) {
        final OrderTable savedOrderTable = orderTableRepository.findById(orderTableId)
                                                                .orElseThrow(IllegalArgumentException::new);

        validationOfChangeEmpty(orderTableId, savedOrderTable);

        savedOrderTable.changeEmpty(orderTable.isEmpty());

        return orderTableRepository.save(savedOrderTable);
    }

    private void validationOfChangeEmpty(final Long orderTableId, final OrderTable savedOrderTable) {
        checkHasTableGroup(savedOrderTable);
        checkOrderStatusOfOrderTable(orderTableId);
    }

    private void checkOrderStatusOfOrderTable(final Long orderTableId) {
        if (orderRepository.existsByOrderTableIdAndOrderStatusIn(orderTableId, Arrays.asList(OrderStatus.COOKING, OrderStatus.MEAL))) {
            throw new IllegalArgumentException();
        }
    }

    private void checkHasTableGroup(final OrderTable savedOrderTable) {
        if (savedOrderTable.hasTableGroup()) {
            throw new IllegalArgumentException();
        }
    }

    @Transactional
    public OrderTable changeNumberOfGuests(final Long orderTableId, final OrderTableDto orderTable) {
        final int numberOfGuests = orderTable.getNumberOfGuests();

        final OrderTable savedOrderTable = orderTableRepository.findById(orderTableId)
                                                                .orElseThrow(IllegalArgumentException::new);

        validationOfChangeNumberOfGuests(numberOfGuests, savedOrderTable);

        savedOrderTable.changeNumberOfGuests(numberOfGuests);

        return orderTableRepository.save(savedOrderTable);
    }

    private void validationOfChangeNumberOfGuests(final int numberOfGuests, final OrderTable savedOrderTable) {
        checkPotiveOfNumberOfGuests(numberOfGuests);
        checkEmptyTable(savedOrderTable);
    }

    private void checkEmptyTable(final OrderTable savedOrderTable) {
        if (savedOrderTable.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    private void checkPotiveOfNumberOfGuests(final int numberOfGuests) {
        if (numberOfGuests < 0) {
            throw new IllegalArgumentException();
        }
    }
}
