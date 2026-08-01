package org.misha.authservice.exception;

public class InvalidPhoneException extends BadRequestException {

    public InvalidPhoneException(String phone) {
        super("Некорректный формат номера телефона: \"" + phone + "\". "
                + "Ожидается формат: 0XXXXXXXXX, XXXXXXXXX или 996XXXXXXXXX");
    }
}
