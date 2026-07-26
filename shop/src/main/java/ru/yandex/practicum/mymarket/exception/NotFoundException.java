package ru.yandex.practicum.mymarket.exception;

public class NotFoundException extends RuntimeException {

    public enum Resource {
        ITEM,
        ORDER
    }

    private final Resource resource;

    public NotFoundException(Resource resource, long id) {
        super(resource + " not found: id=" + id);
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }
}
