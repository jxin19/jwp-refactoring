package kitchenpos.table.dto;

public class OrderTableRequest {
    private Long id;
    private int numberOfGuests;
    private Boolean empty;

    protected OrderTableRequest() {}

    public OrderTableRequest(Long id) {
        this.id = id;
    }

    public OrderTableRequest(Long id, int numberOfGuests, Boolean empty) {
        this.id = id;
        this.numberOfGuests = numberOfGuests;
        this.empty = empty;
    }

    public OrderTableRequest(int numberOfGuests, Boolean empty) {
        this(null, numberOfGuests, empty);
    }

    public Long getId() {
        return id;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public Boolean isEmpty() {
        return empty;
    }

}