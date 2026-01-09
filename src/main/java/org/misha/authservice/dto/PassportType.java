package org.misha.authservice.dto;

/**
 * Типы паспортов для единообразного отображения в Excel.
 */
public enum PassportType {
    ID("ID"),           // Паспорт гражданина КР
    AN("AN"),           // Паспорт гражданина КР (старый формат)
    MIA("MIA"),         // Паспорт МВД
    FOREIGN("FOREIGN"), // Иностранный паспорт
    OTHER("OTHER");     // Другой тип

    private final String code;

    PassportType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * Определяет тип паспорта на основе серии/номера.
     * Если серия начинается с букв, пытаемся определить тип.
     */
    public static PassportType fromSeries(String series) {
        if (series == null || series.isBlank()) {
            return OTHER;
        }
        String upper = series.toUpperCase().trim();
        if (upper.startsWith("ID")) {
            return ID;
        } else if (upper.startsWith("AN")) {
            return AN;
        } else if (upper.startsWith("MIA")) {
            return MIA;
        }
        return OTHER;
    }
}

