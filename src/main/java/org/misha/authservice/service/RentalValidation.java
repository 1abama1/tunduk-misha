package org.misha.authservice.service;
import org.misha.authservice.entity.*;
import org.misha.authservice.exception.BadRequestException;
import org.misha.authservice.dto.CloseContractRequest;
import java.time.LocalDateTime;
public final class RentalValidation {
    private RentalValidation() {}
    public static void client(Client client) {
        if (client.getTag() == Tag.Должник || (client.getTags() != null && client.getTags().contains(ClientTag.DEBTOR)))
            throw new BadRequestException("Выдача должнику запрещена");
    }
    public static void close(RentalDocument doc, CloseContractRequest req) {
        if (req == null) return;
        if (req.paidAmount() != null && (!Double.isFinite(req.paidAmount()) || req.paidAmount() < 0))
            throw new BadRequestException("Оплата должна быть неотрицательным конечным числом");
        if (req.comment() != null && req.comment().length() > 255)
            throw new BadRequestException("Комментарий длиннее 255 символов");
        if (req.actualReturnDate() != null && doc.getStartDateTime() != null && req.actualReturnDate().isBefore(doc.getStartDateTime()))
            throw new BadRequestException("Возврат раньше начала аренды");
    }
}
