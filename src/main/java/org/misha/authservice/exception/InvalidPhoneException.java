package org.misha.authservice.exception;

public class InvalidPhoneException extends BadRequestException {

    public InvalidPhoneException(String phone) {
        super("Некорректный формат номера телефона: \"" + phone + "\". "
                + "Укажите корректный номер (например: 0700123456, 996700123456 или +79001234567)");
    }
}
