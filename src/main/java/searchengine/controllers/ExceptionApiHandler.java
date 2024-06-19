package searchengine.controllers;

import org.apache.tomcat.websocket.AuthenticationException;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.resource.beans.container.internal.NoSuchBeanException;
import org.jsoup.HttpStatusException;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jndi.TypeMismatchNamingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.MethodNotAllowedException;
import searchengine.dto.Message;

import java.nio.file.NoSuchFileException;
import java.rmi.NoSuchObjectException;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ExceptionApiHandler {
    @ExceptionHandler(ArrayIndexOutOfBoundsException.class)
    public ResponseEntity<Message> ArrayIndexOutOfBoundsException(ArrayIndexOutOfBoundsException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Message(false, "Страница не найдена"));
    }

//    @ExceptionHandler(NullPointerException.class)
//    public ResponseEntity<Message> NullPointerException(NullPointerException exception) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Message(false, "Нет проиндексированных страниц"));
//    }

    @ExceptionHandler({BindException.class, HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class, TypeMismatchNamingException.class})
    public ResponseEntity<Message> BadRequestException(BindException bindException, HttpMessageNotReadableException httpMessageNotReadableException,
                                                       MethodArgumentNotValidException methodArgumentNotValidException,
                                                       MissingServletRequestParameterException missingServletRequestParameterException,
                                                       MissingServletRequestPartException missingServletRequestPartException,
                                                       TypeMismatchNamingException typeMismatchNamingException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message(false, "Некорректный запрос"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Message> AuthenticationException(AuthenticationException authenticationException) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Message(false, "Ошибка авторизации"));
    }

    @ExceptionHandler({NoSuchElementException.class, NoSuchParameterException.class, NoSuchObjectException.class,
            NoSuchMethodException.class, NoSuchFileException.class})
    public ResponseEntity<Message> NoSuchElementException(NoSuchElementException noSuchElementException,
                                                          NoSuchParameterException noSuchParameterException,
                                                          NoSuchObjectException noSuchObjectException,
                                                          NoSuchMethodException noSuchMethodException,
                                                          NoSuchFileException noSuchFileException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Message(false, "Источник не найден"));
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    public ResponseEntity<Message> MethodNotAllowed(MethodNotAllowedException methodNotAllowedException) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new Message(false, "Метод не поддерживается"));
    }

    @ExceptionHandler(HttpStatusException.class)
    public ResponseEntity<Message> HttpStatusException(HttpStatusException httpStatusException) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Message(false, "Доступ к сайту запрещен"));
    }
}
