package com.example.webfluxplay.api;

import com.example.webfluxplay.dao.SomeEntityDao;
import com.example.webfluxplay.model.SomeEntity;
import lombok.Data;
import org.reactivestreams.Publisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class SomeEntityHandler {

    private final Validator validator;
    private final SomeEntityDao dao;
    private final Supplier<Publisher<SomeEntity>> findAllPub;
    private final Function<Long, Publisher<Integer>> deleteByIdPub;
    private final Function<Mono<SomeEntity>, Publisher<SomeEntity>> savePub;
    private final Supplier<Publisher<SomeEntityReport>> reportPub;

    public SomeEntityHandler(
            Validator validator,
            SomeEntityDao dao
    ) {
        this.validator = validator;
        this.dao = dao;
        findAllPub = ()-> dao.findAll();
        deleteByIdPub = dao::deleteById;
        savePub = m->m.doOnNext(this::validate).flatMap(dao::save);
        reportPub = ()->dao.findAll().reduce(new SomeEntityReport(), SomeEntityReport::addSomeEntity);
    }

    public Mono<ServerResponse> listSomeEntities(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
//                .body(findAllPub.get(), SomeEntity.class);
                .body(findAllPub.get(), SomeEntity.class);
    }

    @Data
    static class SomeEntityReport {
        private long count;
        private long length;
        public SomeEntityReport addSomeEntity(SomeEntity someEntity) {
            count += 1;
            length += someEntity.getValue().length();
            return this;
        }
    }

    public Mono<ServerResponse> reportSomeEntities(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(reportPub.get(), SomeEntityReport.class);
//                .body(dao.findAll().reduce(new SomeEntityReport(), SomeEntityReport::addSomeEntity), SomeEntityReport.class);
    }
    public Mono<ServerResponse> createSomeEntity(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
//                .body(request.bodyToMono(SomeEntity.class).doOnNext(this::validate).flatMap(dao::save), SomeEntity.class);
                .body(savePub.apply(request.bodyToMono(SomeEntity.class)), SomeEntity.class);
    }

    public Mono<ServerResponse> updateSomeEntity(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(request.bodyToMono(SomeEntity.class)
                        .doOnNext(this::validate)
                        .flatMap(dao::update), SomeEntity.class);
    }

    public Mono<ServerResponse> getSomeEntity(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(dao.findById(Long.valueOf(request.pathVariable("id"))), SomeEntity.class);
    }

    public Mono<ServerResponse> deleteSomeEntity(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
//                .body(dao.deleteById(Long.valueOf(request.pathVariable("id"))), SomeEntity.class);
                .body(deleteByIdPub.apply(Long.valueOf(request.pathVariable("id"))), SomeEntity.class);
    }

    private void validate(SomeEntity someEntity) {
        Errors errors = new BeanPropertyBindingResult(someEntity, "SomeEntity");
        validator.validate(someEntity, errors);
        if (errors.hasErrors()) {
            throw new ServerWebInputException(errors.toString());
        }
    }

}